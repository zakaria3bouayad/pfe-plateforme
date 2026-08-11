package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.Document;
import com.pfe.pfe_backend.domain.Etape;
import com.pfe.pfe_backend.domain.Etudiant;
import com.pfe.pfe_backend.domain.Projet;
import com.pfe.pfe_backend.dto.DocumentDto;
import com.pfe.pfe_backend.dto.DocumentTelechargement;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.DocumentRepository;
import com.pfe.pfe_backend.repository.EtapeRepository;
import com.pfe.pfe_backend.repository.EtudiantRepository;
import com.pfe.pfe_backend.repository.ProjetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Gestion des documents verses sur un projet (Lot 4, bloc A).
 *
 * Upload reserve au chef d'equipe (meme regle que la soumission d'un jalon
 * au lot 3). Versionnement automatique : deposer un fichier du meme nom sur
 * le meme couple (projet, jalon) cree une nouvelle version sans ecraser
 * l'historique. Suppression toujours logique cote base (EF-03) ; l'objet
 * MinIO correspondant est lui reellement retire, la ligne servant de trace.
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ProjetRepository projetRepository;
    private final EtapeRepository etapeRepository;
    private final EtudiantRepository etudiantRepository;
    private final DocumentStorageService documentStorageService;

    @Transactional(readOnly = true)
    public List<DocumentDto> listerParProjet(Long projetId) {
        return documentRepository.findByProjetIdAndSupprimeFalseOrderByDateUploadDesc(projetId).stream()
                .map(DocumentDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> listerParEtape(Long etapeId) {
        return documentRepository.findByEtapeIdAndSupprimeFalseOrderByDateUploadDesc(etapeId).stream()
                .map(DocumentDto::from).toList();
    }

    /** Historique complet des versions (actives et supprimees) d'un document donne, du plus recent au plus ancien. */
    @Transactional(readOnly = true)
    public List<DocumentDto> historiqueVersions(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> BusinessException.introuvable("Document introuvable"));
        Long etapeId = document.getEtape() != null ? document.getEtape().getId() : null;
        return historiqueVersions(document.getProjet().getId(), etapeId, document.getNom())
                .stream().toList();
    }

    /** Historique complet des versions (actives et supprimees) pour un nom donne sur un couple (projet, jalon). */
    private List<DocumentDto> historiqueVersions(Long projetId, Long etapeId, String nom) {
        List<Document> versions = etapeId != null
                ? documentRepository.findByProjetIdAndNomAndEtapeIdOrderByVersionDesc(projetId, nom, etapeId)
                : documentRepository.findByProjetIdAndNomAndEtapeIsNullOrderByVersionDesc(projetId, nom);
        return versions.stream().map(DocumentDto::from).toList();
    }

    @Transactional
    public DocumentDto uploader(String emailUploadeur, Long projetId, Long etapeId, MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Le fichier envoye est vide");
        }

        String nom = nettoyerNom(fichier.getOriginalFilename());
        if (nom.isBlank()) {
            throw new BusinessException("Le nom du fichier est invalide");
        }

        Projet projet = trouverProjet(projetId);
        Etape etape = etapeId != null ? trouverEtapeDuProjet(etapeId, projet) : null;
        Etudiant uploadeur = verifierChefDeLequipe(projet, emailUploadeur);

        int version = prochaineVersion(projetId, etapeId, nom);
        String cheminObjet = genererCheminObjet(projetId, etapeId, version, nom);
        String typeContenu = fichier.getContentType() != null ? fichier.getContentType() : "application/octet-stream";

        try (InputStream contenu = fichier.getInputStream()) {
            documentStorageService.uploaderObjet(cheminObjet, contenu, fichier.getSize(), typeContenu);
        } catch (IOException e) {
            throw new BusinessException("Impossible de lire le fichier envoye", HttpStatus.BAD_REQUEST);
        }

        Document document = Document.builder()
                .nom(nom)
                .type(typeContenu)
                .taille(fichier.getSize())
                .cheminMinio(cheminObjet)
                .version(version)
                .projet(projet)
                .etape(etape)
                .uploadeur(uploadeur)
                .build();

        return DocumentDto.from(documentRepository.save(document));
    }

    @Transactional(readOnly = true)
    public DocumentTelechargement telecharger(Long documentId) {
        Document document = trouverActif(documentId);
        InputStream contenu = documentStorageService.telechargerObjet(document.getCheminMinio());
        return new DocumentTelechargement(document.getNom(), document.getType(), contenu);
    }

    @Transactional
    public void supprimer(String emailDemandeur, Long documentId) {
        Document document = trouverActif(documentId);
        verifierAutoriseASupprimer(document, emailDemandeur);

        documentStorageService.supprimerObjet(document.getCheminMinio());
        document.setSupprime(true);
        document.setDateSuppression(LocalDateTime.now());
    }

    // ------------------------------------------------------------ prive

    private Projet trouverProjet(Long projetId) {
        return projetRepository.findById(projetId)
                .orElseThrow(() -> BusinessException.introuvable("Projet introuvable"));
    }

    private Etape trouverEtapeDuProjet(Long etapeId, Projet projet) {
        Etape etape = etapeRepository.findById(etapeId)
                .orElseThrow(() -> BusinessException.introuvable("Jalon introuvable"));
        if (!etape.getProjet().getId().equals(projet.getId())) {
            throw BusinessException.conflit("Ce jalon n'appartient pas a ce projet");
        }
        return etape;
    }

    private Document trouverActif(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> BusinessException.introuvable("Document introuvable"));
        if (document.isSupprime()) {
            throw BusinessException.introuvable("Document introuvable");
        }
        return document;
    }

    private Etudiant verifierChefDeLequipe(Projet projet, String emailEtudiant) {
        Etudiant etudiant = etudiantRepository.findByEmail(emailEtudiant)
                .orElseThrow(() -> BusinessException.introuvable("Etudiant introuvable"));

        Long chefId = projet.getEquipe().getChef().getId();
        if (!chefId.equals(etudiant.getId())) {
            throw BusinessException.interdit("Seul le chef d'equipe peut deposer un document");
        }
        return etudiant;
    }

    private void verifierAutoriseASupprimer(Document document, String email) {
        if (document.getUploadeur().getEmail().equalsIgnoreCase(email)) {
            return;
        }
        if (document.getProjet().getEncadrant().getEmail().equalsIgnoreCase(email)) {
            return;
        }
        throw BusinessException.interdit("Vous n'etes pas autorise a supprimer ce document");
    }

    /** Prochain numero de version pour ce nom, en tenant compte de tout l'historique (meme les versions supprimees). */
    private int prochaineVersion(Long projetId, Long etapeId, String nom) {
        List<Document> versions = etapeId != null
                ? documentRepository.findByProjetIdAndNomAndEtapeIdOrderByVersionDesc(projetId, nom, etapeId)
                : documentRepository.findByProjetIdAndNomAndEtapeIsNullOrderByVersionDesc(projetId, nom);
        return versions.stream().findFirst().map(d -> d.getVersion() + 1).orElse(1);
    }

    /** Ne conserve que le nom de fichier (pas de chemin), tronque a la longueur de colonne (255). */
    private String nettoyerNom(String nomOriginal) {
        if (nomOriginal == null) {
            return "";
        }
        String nom = Paths.get(nomOriginal).getFileName().toString().trim();
        return nom.length() > 255 ? nom.substring(0, 255) : nom;
    }

    /** Cle d'objet MinIO : arborescence projet/jalon + version + suffixe unique pour eviter toute collision. */
    private String genererCheminObjet(Long projetId, Long etapeId, int version, String nom) {
        String dossier = etapeId != null ? "jalons/" + etapeId : "generaux";
        String nomSanitise = nom.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "projets/" + projetId + "/" + dossier + "/v" + version + "_" + UUID.randomUUID() + "_" + nomSanitise;
    }
}
