package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.Ressource;
import com.pfe.pfe_backend.domain.Utilisateur;
import com.pfe.pfe_backend.domain.enums.Role;
import com.pfe.pfe_backend.dto.RessourceDto;
import com.pfe.pfe_backend.dto.RessourceTelechargement;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.RessourceRepository;
import com.pfe.pfe_backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Bibliotheque de ressources partagees (Lot 5, bloc B). Bibliotheque
 * globale, non rattachee a un projet.
 *
 * Consultation ouverte a tout compte authentifie. Creation reservee a
 * l'encadrant ou a l'administrateur (verifie par @PreAuthorize au
 * controleur). Edition et suppression reservees a l'auteur de la ressource
 * ou a un administrateur (verifie ici, meme principe que la suppression de
 * document au lot 4). Chaque ressource doit avoir au moins un lien ou un
 * fichier.
 */
@Service
@RequiredArgsConstructor
public class RessourceService {

    private final RessourceRepository ressourceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DocumentStorageService documentStorageService;

    @Transactional(readOnly = true)
    public List<RessourceDto> lister() {
        return ressourceRepository.findAllByOrderByDateCreationDesc().stream()
                .map(RessourceDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RessourceDto> listerParCategorie(String categorie) {
        return ressourceRepository.findByCategorieIgnoreCaseOrderByDateCreationDesc(categorie).stream()
                .map(RessourceDto::from).toList();
    }

    @Transactional
    public RessourceDto creer(
            String emailAuteur, String titre, String description, String categorie,
            String lien, MultipartFile fichier) {

        if (titre == null || titre.isBlank()) {
            throw new BusinessException("Le titre est obligatoire");
        }
        if (categorie == null || categorie.isBlank()) {
            throw new BusinessException("La categorie est obligatoire");
        }
        boolean lienFourni = lien != null && !lien.isBlank();
        boolean fichierFourni = fichier != null && !fichier.isEmpty();
        if (!lienFourni && !fichierFourni) {
            throw new BusinessException("Il faut fournir un lien ou un fichier");
        }

        Ressource.RessourceBuilder builder = Ressource.builder()
                .titre(titre.trim())
                .description(description != null ? description.trim() : null)
                .categorie(categorie.trim())
                .lien(lienFourni ? lien.trim() : null)
                .auteur(trouverUtilisateur(emailAuteur));

        if (fichierFourni) {
            FichierStocke stocke = stockerFichier(fichier);
            builder.fichierNom(stocke.nom())
                    .fichierType(stocke.type())
                    .fichierTaille(stocke.taille())
                    .fichierCheminMinio(stocke.cheminObjet());
        }

        return RessourceDto.from(ressourceRepository.save(builder.build()));
    }

    /**
     * Modification d'une ressource : titre, description, categorie et lien
     * sont toujours remplaces par les valeurs envoyees. Un nouveau fichier
     * remplace l'ancien (qui est retire de MinIO) ; sans nouveau fichier,
     * le fichier existant (le cas echeant) est conserve tel quel.
     */
    @Transactional
    public RessourceDto modifier(
            String email, Long ressourceId, String titre, String description, String categorie,
            String lien, MultipartFile nouveauFichier) {

        Ressource ressource = trouverRessource(ressourceId);
        verifierAutoriseAGerer(ressource, email);

        if (titre == null || titre.isBlank()) {
            throw new BusinessException("Le titre est obligatoire");
        }
        if (categorie == null || categorie.isBlank()) {
            throw new BusinessException("La categorie est obligatoire");
        }
        boolean lienFourni = lien != null && !lien.isBlank();
        boolean nouveauFichierFourni = nouveauFichier != null && !nouveauFichier.isEmpty();
        boolean conserveFichierExistant = !nouveauFichierFourni && ressource.getFichierCheminMinio() != null;
        if (!lienFourni && !nouveauFichierFourni && !conserveFichierExistant) {
            throw new BusinessException("Il faut fournir un lien ou un fichier");
        }

        ressource.setTitre(titre.trim());
        ressource.setDescription(description != null ? description.trim() : null);
        ressource.setCategorie(categorie.trim());
        ressource.setLien(lienFourni ? lien.trim() : null);

        if (nouveauFichierFourni) {
            String ancienChemin = ressource.getFichierCheminMinio();
            FichierStocke stocke = stockerFichier(nouveauFichier);
            ressource.setFichierNom(stocke.nom());
            ressource.setFichierType(stocke.type());
            ressource.setFichierTaille(stocke.taille());
            ressource.setFichierCheminMinio(stocke.cheminObjet());
            if (ancienChemin != null) {
                documentStorageService.supprimerObjet(ancienChemin);
            }
        }

        return RessourceDto.from(ressource);
    }

    @Transactional
    public void supprimer(String email, Long ressourceId) {
        Ressource ressource = trouverRessource(ressourceId);
        verifierAutoriseAGerer(ressource, email);

        if (ressource.getFichierCheminMinio() != null) {
            documentStorageService.supprimerObjet(ressource.getFichierCheminMinio());
        }
        ressourceRepository.delete(ressource);
    }

    @Transactional(readOnly = true)
    public RessourceTelechargement telecharger(Long ressourceId) {
        Ressource ressource = trouverRessource(ressourceId);
        if (ressource.getFichierCheminMinio() == null) {
            throw BusinessException.introuvable("Cette ressource n'a pas de fichier");
        }
        InputStream contenu = documentStorageService.telechargerObjet(ressource.getFichierCheminMinio());
        return new RessourceTelechargement(ressource.getFichierNom(), ressource.getFichierType(), contenu);
    }

    // ------------------------------------------------------------ prive

    private Ressource trouverRessource(Long ressourceId) {
        return ressourceRepository.findById(ressourceId)
                .orElseThrow(() -> BusinessException.introuvable("Ressource introuvable"));
    }

    private void verifierAutoriseAGerer(Ressource ressource, String email) {
        if (ressource.getAuteur().getEmail().equalsIgnoreCase(email)) {
            return;
        }
        if (utilisateurRepository.findByEmail(email)
                .map(u -> u.getRole() == Role.ADMINISTRATEUR)
                .orElse(false)) {
            return;
        }
        throw BusinessException.interdit("Vous n'etes pas autorise a gerer cette ressource");
    }

    private FichierStocke stockerFichier(MultipartFile fichier) {
        String nom = nettoyerNom(fichier.getOriginalFilename());
        if (nom.isBlank()) {
            throw new BusinessException("Le nom du fichier est invalide");
        }
        String typeContenu = fichier.getContentType() != null
                ? fichier.getContentType() : "application/octet-stream";
        String cheminObjet = genererCheminObjet(nom);

        try (InputStream contenu = fichier.getInputStream()) {
            documentStorageService.uploaderObjet(cheminObjet, contenu, fichier.getSize(), typeContenu);
        } catch (IOException e) {
            throw new BusinessException("Impossible de lire le fichier envoye", HttpStatus.BAD_REQUEST);
        }
        return new FichierStocke(nom, typeContenu, fichier.getSize(), cheminObjet);
    }

    private record FichierStocke(String nom, String type, long taille, String cheminObjet) {}

    private Utilisateur trouverUtilisateur(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.introuvable("Utilisateur introuvable"));
    }

    /** Ne conserve que le nom de fichier (pas de chemin), tronque a la longueur de colonne (255). */
    private String nettoyerNom(String nomOriginal) {
        if (nomOriginal == null) {
            return "";
        }
        String nom = Paths.get(nomOriginal).getFileName().toString().trim();
        return nom.length() > 255 ? nom.substring(0, 255) : nom;
    }

    /** Cle d'objet MinIO dediee a la bibliotheque de ressources (pas d'arborescence projet). */
    private String genererCheminObjet(String nom) {
        String nomSanitise = nom.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "ressources/" + UUID.randomUUID() + "_" + nomSanitise;
    }
}
