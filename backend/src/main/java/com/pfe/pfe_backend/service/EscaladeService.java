package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.Etudiant;
import com.pfe.pfe_backend.domain.Projet;
import com.pfe.pfe_backend.domain.Superviseur;
import com.pfe.pfe_backend.dto.EscaladeResponse;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.EtudiantRepository;
import com.pfe.pfe_backend.repository.ProjetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mise en relation avec l'encadrant lorsque l'assistant ne sait pas
 * repondre (Lot 8, etape 8.9 ; POST /api/assistant/escalade, diagramme de
 * sequence 6). Volontairement separe de RagService (voir sa javadoc,
 * etape 8.7) : ce n'est pas une preoccupation RAG mais une resolution
 * etudiant -> equipe -> projet -> encadrant, deja etablie ailleurs
 * (ProjetService.trouverParEtudiant, StatsService).
 *
 * Decision de perimetre assumee : ne cree pas de Notification en base pour
 * l'encadrant. Cela exigerait d'ajouter une valeur a TypeNotification, or
 * la contrainte CHECK sur notification.type est figee depuis sa creation
 * (V6__notifications_audit.sql, ddl-auto=update ne la met jamais a jour -
 * meme lecon documentee dans ce fichier de migration) : l'etendre suppose
 * une migration Flyway a part entiere, hors de portee au milieu du lot.
 * Ce service se limite donc a restituer les coordonnees de l'encadrant, que
 * le frontend affichera (etape 8.11) ; seule une entree d'audit trace la
 * demande (etape 8.9, journalisee par ChatController comme pour 8.8),
 * sans ce probleme puisque entree_audit.action est un champ libre.
 * Notification effective a envisager plus tard, a la faveur de la
 * migration V7 (etape 8.14).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EscaladeService {

    private final EtudiantRepository etudiantRepository;
    private final ProjetRepository projetRepository;
    private final AnonymisationService anonymisationService;

    @Transactional(readOnly = true)
    public EscaladeResponse escalader(String email, String question) {
        Etudiant etudiant = etudiantRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.introuvable("Etudiant introuvable"));

        if (etudiant.getEquipe() == null) {
            throw BusinessException.introuvable(
                    "Vous n'appartenez a aucune equipe pour l'instant : contactez l'administration.");
        }

        Projet projet = projetRepository.findByEquipeId(etudiant.getEquipe().getId())
                .orElseThrow(() -> BusinessException.introuvable(
                        "Votre equipe n'a pas encore de projet : contactez l'administration."));

        Superviseur encadrant = projet.getEncadrant();

        if (question != null && !question.isBlank()) {
            log.info("Assistant : escalade demandee par {} vers {} - question : {}",
                    email, encadrant.getEmail(), anonymisationService.anonymiser(question));
        } else {
            log.info("Assistant : escalade demandee par {} vers {}", email, encadrant.getEmail());
        }

        return new EscaladeResponse(
                "Vous pouvez contacter directement votre encadrant, " + encadrant.getNomComplet() + ".",
                encadrant.getNomComplet(),
                encadrant.getEmail());
    }
}
