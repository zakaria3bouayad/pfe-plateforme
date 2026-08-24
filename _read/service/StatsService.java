package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.Etape;
import com.pfe.pfe_backend.domain.Etudiant;
import com.pfe.pfe_backend.domain.Projet;
import com.pfe.pfe_backend.domain.Superviseur;
import com.pfe.pfe_backend.domain.enums.StatutEtape;
import com.pfe.pfe_backend.domain.enums.StatutProjet;
import com.pfe.pfe_backend.domain.enums.StatutSujet;
import com.pfe.pfe_backend.dto.StatsAdminDto;
import com.pfe.pfe_backend.dto.StatsEncadrantDto;
import com.pfe.pfe_backend.dto.StatsEtudiantDto;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.EquipeRepository;
import com.pfe.pfe_backend.repository.EtapeRepository;
import com.pfe.pfe_backend.repository.EtudiantRepository;
import com.pfe.pfe_backend.repository.ProjetRepository;
import com.pfe.pfe_backend.repository.SujetRepository;
import com.pfe.pfe_backend.repository.SuperviseurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Statistiques agregees pour les tableaux de bord (Lot 3, bloc B).
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    /** Sujets pas encore decides par l'administrateur (EF-09). */
    private static final Set<StatutSujet> SUJETS_EN_ATTENTE =
            EnumSet.of(StatutSujet.PROPOSE, StatutSujet.EN_VALIDATION);

    private final EtapeRepository etapeRepository;
    private final ProjetRepository projetRepository;
    private final SujetRepository sujetRepository;
    private final EquipeRepository equipeRepository;
    private final EtudiantRepository etudiantRepository;
    private final SuperviseurRepository superviseurRepository;

    @Transactional(readOnly = true)
    public StatsEtudiantDto statsEtudiant(String emailEtudiant) {
        Etudiant etudiant = etudiantRepository.findByEmail(emailEtudiant)
                .orElseThrow(() -> BusinessException.introuvable("Etudiant introuvable"));

        if (etudiant.getEquipe() == null) {
            return statsEtudiantVide();
        }

        Optional<Projet> projetOpt = projetRepository.findByEquipeId(etudiant.getEquipe().getId());
        if (projetOpt.isEmpty()) {
            return statsEtudiantVide();
        }

        Projet projet = projetOpt.get();
        long total = etapeRepository.countByProjetId(projet.getId());
        long valides = etapeRepository.countByProjetIdAndStatut(projet.getId(), StatutEtape.VALIDEE);
        long enRetard = etapeRepository.countByProjetIdAndStatut(projet.getId(), StatutEtape.EN_RETARD);
        long restants = total - valides - enRetard;

        Etape prochaine = etapeRepository
                .findFirstByProjetIdAndStatutNotOrderByDateEcheanceAsc(projet.getId(), StatutEtape.VALIDEE)
                .orElse(null);

        return new StatsEtudiantDto(
                projet.getId(),
                projet.getStatut().name(),
                (int) total,
                (int) valides,
                (int) enRetard,
                (int) restants,
                prochaine != null ? prochaine.getDateEcheance() : null
        );
    }

    @Transactional(readOnly = true)
    public StatsEncadrantDto statsEncadrant(String emailEncadrant) {
        Superviseur encadrant = superviseurRepository.findByEmail(emailEncadrant)
                .orElseThrow(() -> BusinessException.introuvable("Encadrant introuvable"));

        long sujetsEnAttente = sujetRepository.countByEncadrantIdAndStatutIn(
                encadrant.getId(), SUJETS_EN_ATTENTE);
        long projetsEnCours = projetRepository.countByEncadrantIdAndStatut(
                encadrant.getId(), StatutProjet.EN_COURS);
        long jalonsAValider = etapeRepository.countByProjetEncadrantIdAndStatut(
                encadrant.getId(), StatutEtape.SOUMISE);
        long jalonsEnRetard = etapeRepository.countByProjetEncadrantIdAndStatut(
                encadrant.getId(), StatutEtape.EN_RETARD);

        return new StatsEncadrantDto(
                (int) sujetsEnAttente, (int) projetsEnCours, (int) jalonsAValider, (int) jalonsEnRetard);
    }

    @Transactional(readOnly = true)
    public StatsAdminDto statsAdmin() {
        return new StatsAdminDto(
                etudiantRepository.count(),
                superviseurRepository.count(),
                equipeRepository.count(),
                sujetRepository.count(),
                sujetRepository.countByStatutIn(SUJETS_EN_ATTENTE),
                projetRepository.count(),
                projetRepository.countByStatut(StatutProjet.EN_COURS),
                etapeRepository.countByStatut(StatutEtape.EN_RETARD)
        );
    }

    private StatsEtudiantDto statsEtudiantVide() {
        return new StatsEtudiantDto(null, null, 0, 0, 0, 0, null);
    }
}
