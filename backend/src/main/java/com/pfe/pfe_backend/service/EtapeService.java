package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.Etape;
import com.pfe.pfe_backend.domain.Etudiant;
import com.pfe.pfe_backend.domain.Projet;
import com.pfe.pfe_backend.domain.enums.StatutEtape;
import com.pfe.pfe_backend.dto.EtapeDto;
import com.pfe.pfe_backend.dto.EtapeRequest;
import com.pfe.pfe_backend.dto.SoumissionRequest;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.EtapeRepository;
import com.pfe.pfe_backend.repository.EtudiantRepository;
import com.pfe.pfe_backend.repository.ProjetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Gestion des jalons d'un projet (Lot 3, bloc A).
 *
 * Cycle de vie d'un jalon : A_FAIRE/EN_COURS -> SOUMISE (par le chef
 * d'equipe) -> VALIDEE (par l'encadrant). EN_RETARD n'est jamais choisi par
 * un utilisateur : il est recalcule automatiquement (EF-26).
 */
@Service
@RequiredArgsConstructor
public class EtapeService {

    /** Statuts pour lesquels un jalon peut etre soumis. */
    private static final Set<StatutEtape> STATUTS_SOUMETTABLES =
            EnumSet.of(StatutEtape.A_FAIRE, StatutEtape.EN_COURS, StatutEtape.EN_RETARD);

    /** Statuts qu'une echeance depassee ne fait pas basculer en EN_RETARD : le travail est deja rendu. */
    private static final Set<StatutEtape> STATUTS_SOLDES =
            EnumSet.of(StatutEtape.SOUMISE, StatutEtape.VALIDEE, StatutEtape.EN_RETARD);

    private final EtapeRepository etapeRepository;
    private final ProjetRepository projetRepository;
    private final EtudiantRepository etudiantRepository;

    @Transactional(readOnly = true)
    public List<EtapeDto> listerParProjet(Long projetId) {
        return etapeRepository.findByProjetIdOrderByOrdreAsc(projetId).stream()
                .map(EtapeDto::from).toList();
    }

    @Transactional(readOnly = true)
    public EtapeDto trouver(Long id) {
        return EtapeDto.from(trouverEntite(id));
    }

    @Transactional
    public EtapeDto creer(String emailEncadrant, Long projetId, EtapeRequest requete) {
        Projet projet = trouverProjet(projetId);
        verifierEncadrantDuProjet(projet, emailEncadrant);

        Etape etape = Etape.builder()
                .projet(projet)
                .titre(requete.titre().trim())
                .description(requete.description().trim())
                .dateEcheance(requete.dateEcheance())
                .ordre(requete.ordre())
                .statut(StatutEtape.A_FAIRE)
                .build();

        return EtapeDto.from(etapeRepository.save(etape));
    }

    @Transactional
    public EtapeDto modifier(Long id, String emailEncadrant, EtapeRequest requete) {
        Etape etape = trouverEntite(id);
        verifierEncadrantDuProjet(etape.getProjet(), emailEncadrant);
        verifierModifiable(etape);

        etape.setTitre(requete.titre().trim());
        etape.setDescription(requete.description().trim());
        etape.setDateEcheance(requete.dateEcheance());
        etape.setOrdre(requete.ordre());

        return EtapeDto.from(etape);
    }

    @Transactional
    public void supprimer(Long id, String emailEncadrant) {
        Etape etape = trouverEntite(id);
        verifierEncadrantDuProjet(etape.getProjet(), emailEncadrant);
        verifierModifiable(etape);
        etapeRepository.delete(etape);
    }

    // ------------------------------------------------------------ soumission / validation

    @Transactional
    public EtapeDto soumettre(String emailChef, Long id, SoumissionRequest requete) {
        Etape etape = trouverEntite(id);
        verifierChefDeLequipe(etape.getProjet(), emailChef);
        exigerStatut(etape, STATUTS_SOUMETTABLES);

        etape.setLienLivrable(requete.lienLivrable().trim());
        etape.setCommentaireSoumission(
                requete.commentaire() != null ? requete.commentaire().trim() : null);
        etape.setDateSoumission(LocalDateTime.now());
        etape.setStatut(StatutEtape.SOUMISE);

        return EtapeDto.from(etape);
    }

    @Transactional
    public EtapeDto valider(String emailEncadrant, Long id, String commentaire) {
        Etape etape = trouverEntite(id);
        verifierEncadrantDuProjet(etape.getProjet(), emailEncadrant);
        exigerStatut(etape, EnumSet.of(StatutEtape.SOUMISE));

        etape.setStatut(StatutEtape.VALIDEE);
        etape.setCommentaireValidation(commentaire != null ? commentaire.trim() : null);
        etape.setDateValidation(LocalDateTime.now());

        return EtapeDto.from(etape);
    }

    // ------------------------------------------------------------ calcul automatique EN_RETARD (EF-26)

    /**
     * Recalcule le statut EN_RETARD de tous les jalons dont l'echeance est
     * depassee et qui n'ont pas encore ete rendus. Tourne chaque nuit ; la
     * valeur de retour (nombre de jalons bascules) sert surtout aux tests.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public int recalculerRetards() {
        List<Etape> enRetard = etapeRepository.findByStatutNotInAndDateEcheanceBefore(
                STATUTS_SOLDES, LocalDate.now());
        enRetard.forEach(e -> e.setStatut(StatutEtape.EN_RETARD));
        return enRetard.size();
    }

    // ------------------------------------------------------------ prive

    private Etape trouverEntite(Long id) {
        return etapeRepository.findById(id)
                .orElseThrow(() -> BusinessException.introuvable("Jalon introuvable"));
    }

    private Projet trouverProjet(Long projetId) {
        return projetRepository.findById(projetId)
                .orElseThrow(() -> BusinessException.introuvable("Projet introuvable"));
    }

    private void verifierEncadrantDuProjet(Projet projet, String emailEncadrant) {
        if (!projet.getEncadrant().getEmail().equalsIgnoreCase(emailEncadrant)) {
            throw BusinessException.interdit("Ce projet ne vous est pas confie");
        }
    }

    private void verifierChefDeLequipe(Projet projet, String emailEtudiant) {
        Etudiant etudiant = etudiantRepository.findByEmail(emailEtudiant)
                .orElseThrow(() -> BusinessException.introuvable("Etudiant introuvable"));

        Long chefId = projet.getEquipe().getChef().getId();
        if (!chefId.equals(etudiant.getId())) {
            throw BusinessException.interdit("Seul le chef d'equipe peut soumettre un livrable");
        }
    }

    private void verifierModifiable(Etape etape) {
        if (etape.getStatut() == StatutEtape.SOUMISE || etape.getStatut() == StatutEtape.VALIDEE) {
            throw BusinessException.conflit(
                    "Ce jalon a deja ete soumis, il ne peut plus etre modifie ou supprime");
        }
    }

    private void exigerStatut(Etape etape, Set<StatutEtape> statutsAttendus) {
        if (!statutsAttendus.contains(etape.getStatut())) {
            throw BusinessException.conflit(
                    "Ce jalon est au statut " + etape.getStatut() + ", cette action n'est pas possible");
        }
    }
}
