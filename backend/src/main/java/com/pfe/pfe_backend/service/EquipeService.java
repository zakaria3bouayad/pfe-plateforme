package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.Equipe;
import com.pfe.pfe_backend.domain.Etudiant;
import com.pfe.pfe_backend.dto.AjoutMembreRequest;
import com.pfe.pfe_backend.dto.EquipeDto;
import com.pfe.pfe_backend.dto.EquipeRequest;
import com.pfe.pfe_backend.dto.MembreEquipeDto;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.EquipeRepository;
import com.pfe.pfe_backend.repository.EtudiantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Constitution et gestion des equipes d'etudiants (EF-11).
 *
 * Un etudiant appartient a au plus une equipe a la fois (Etudiant.equipe).
 * Le chef est celui qui a cree l'equipe ; il est seul habilite a en retirer
 * des membres, et ne peut la quitter que via dissoudre(). L'ajout d'un
 * membre peut venir soit du chef (ajouterMembre), soit du candidat
 * lui-meme (rejoindre, hors plan initial). L'affectation d'un sujet a une
 * equipe fait l'objet d'une etape ulterieure du lot 2.
 */
@Service
@RequiredArgsConstructor
public class EquipeService {

    private final EquipeRepository equipeRepository;
    private final EtudiantRepository etudiantRepository;

    @Transactional(readOnly = true)
    public List<EquipeDto> lister() {
        return equipeRepository.findAllByOrderByDateCreationDesc().stream()
                .map(this::versDto).toList();
    }

    @Transactional(readOnly = true)
    public EquipeDto trouver(Long id) {
        return versDto(trouverEntite(id));
    }

    @Transactional(readOnly = true)
    public EquipeDto trouverParEtudiant(String email) {
        Etudiant etudiant = trouverEtudiant(email);
        if (etudiant.getEquipe() == null) {
            throw BusinessException.introuvable("Vous n'appartenez a aucune equipe");
        }
        return versDto(etudiant.getEquipe());
    }

    @Transactional
    public EquipeDto creer(String emailChef, EquipeRequest requete) {
        Etudiant chef = trouverEtudiant(emailChef);

        if (chef.getEquipe() != null) {
            throw BusinessException.conflit("Vous appartenez deja a une equipe");
        }

        Equipe equipe = Equipe.builder()
                .nom(requete.nom().trim())
                .tailleMax(requete.tailleMax())
                .chef(chef)
                .build();
        equipe = equipeRepository.save(equipe);

        chef.setEquipe(equipe);

        return versDto(equipe);
    }

    @Transactional
    public EquipeDto ajouterMembre(Long equipeId, String emailChef, AjoutMembreRequest requete) {
        Equipe equipe = trouverEntite(equipeId);
        verifierChef(equipe, emailChef);

        Etudiant candidat = etudiantRepository.findByNumeroEtudiant(requete.numeroEtudiant())
                .orElseThrow(() -> BusinessException.introuvable("Aucun etudiant avec ce numero"));

        if (candidat.getEquipe() != null) {
            throw BusinessException.conflit("Cet etudiant appartient deja a une equipe");
        }

        Etudiant chef = equipe.getChef();
        if (!candidat.getFiliere().getId().equals(chef.getFiliere().getId())
                || !candidat.getPromotion().getId().equals(chef.getPromotion().getId())) {
            throw new BusinessException(
                    "L'etudiant doit etre de la meme filiere et de la meme promotion que l'equipe");
        }

        if (etudiantRepository.countByEquipeId(equipeId) >= equipe.getTailleMax()) {
            throw BusinessException.conflit("L'equipe a atteint sa taille maximale");
        }

        candidat.setEquipe(equipe);
        return versDto(equipe);
    }

    /**
     * Auto-inscription (hors plan, ajoute a la demande de Zakaria) : un
     * etudiant sans equipe peut rejoindre lui-meme une equipe existante,
     * sans passer par le chef. Memes regles metier que ajouterMembre (meme
     * filiere/promotion que le chef, equipe pas pleine), initiees par le
     * candidat plutot que par le chef.
     */
    @Transactional
    public EquipeDto rejoindre(Long equipeId, String emailEtudiant) {
        Equipe equipe = trouverEntite(equipeId);
        Etudiant candidat = trouverEtudiant(emailEtudiant);

        if (candidat.getEquipe() != null) {
            throw BusinessException.conflit("Vous appartenez deja a une equipe");
        }

        Etudiant chef = equipe.getChef();
        if (!candidat.getFiliere().getId().equals(chef.getFiliere().getId())
                || !candidat.getPromotion().getId().equals(chef.getPromotion().getId())) {
            throw new BusinessException(
                    "Vous devez etre de la meme filiere et de la meme promotion que cette equipe");
        }

        if (etudiantRepository.countByEquipeId(equipeId) >= equipe.getTailleMax()) {
            throw BusinessException.conflit("Cette equipe a atteint sa taille maximale");
        }

        candidat.setEquipe(equipe);
        return versDto(equipe);
    }

    @Transactional
    public void retirerMembre(Long equipeId, String emailChef, Long etudiantId) {
        Equipe equipe = trouverEntite(equipeId);
        verifierChef(equipe, emailChef);

        if (equipe.getChef().getId().equals(etudiantId)) {
            throw new BusinessException("Le chef ne peut pas se retirer lui-meme ; dissolvez l'equipe");
        }

        Etudiant membre = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> BusinessException.introuvable("Etudiant introuvable"));

        if (membre.getEquipe() == null || !membre.getEquipe().getId().equals(equipeId)) {
            throw BusinessException.conflit("Cet etudiant n'appartient pas a cette equipe");
        }

        membre.setEquipe(null);
    }

    @Transactional
    public void quitter(Long equipeId, String emailEtudiant) {
        Etudiant etudiant = trouverEtudiant(emailEtudiant);
        Equipe equipe = trouverEntite(equipeId);

        if (etudiant.getEquipe() == null || !etudiant.getEquipe().getId().equals(equipeId)) {
            throw BusinessException.conflit("Vous n'appartenez pas a cette equipe");
        }
        if (equipe.getChef().getId().equals(etudiant.getId())) {
            throw new BusinessException("Le chef ne peut pas quitter l'equipe ; dissolvez-la");
        }

        etudiant.setEquipe(null);
    }

    @Transactional
    public void dissoudre(Long equipeId, String emailChef, boolean estAdmin) {
        Equipe equipe = trouverEntite(equipeId);
        if (!estAdmin) {
            verifierChef(equipe, emailChef);
        }

        etudiantRepository.findByEquipeId(equipeId).forEach(m -> m.setEquipe(null));
        equipeRepository.delete(equipe);
    }

    // ------------------------------------------------------------ prive

    private Equipe trouverEntite(Long id) {
        return equipeRepository.findById(id)
                .orElseThrow(() -> BusinessException.introuvable("Equipe introuvable"));
    }

    private Etudiant trouverEtudiant(String email) {
        return etudiantRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.introuvable("Etudiant introuvable"));
    }

    private void verifierChef(Equipe equipe, String emailChef) {
        if (!equipe.getChef().getEmail().equalsIgnoreCase(emailChef)) {
            throw BusinessException.interdit("Seul le chef d'equipe peut effectuer cette action");
        }
    }

    private EquipeDto versDto(Equipe equipe) {
        List<MembreEquipeDto> membres = etudiantRepository.findByEquipeId(equipe.getId()).stream()
                .map(e -> new MembreEquipeDto(e.getId(), e.getNomComplet(), e.getNumeroEtudiant()))
                .toList();

        return new EquipeDto(
                equipe.getId(),
                equipe.getNom(),
                equipe.getTailleMax(),
                equipe.getChef().getId(),
                equipe.getChef().getNomComplet(),
                membres,
                equipe.getDateCreation()
        );
    }
}
