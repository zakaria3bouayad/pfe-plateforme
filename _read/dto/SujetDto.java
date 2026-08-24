package com.pfe.pfe_backend.dto;

import com.pfe.pfe_backend.domain.Sujet;
import com.pfe.pfe_backend.domain.enums.StatutSujet;

import java.time.LocalDateTime;

/**
 * Representation d'un sujet renvoyee au frontend (EF-08).
 */
public record SujetDto(
        Long id,
        String titre,
        String description,
        String motsCles,
        int capaciteMax,
        StatutSujet statut,
        Long encadrantId,
        String encadrantNom,
        Long filiereId,
        String filiereLibelle,
        String commentaireValidation,
        LocalDateTime dateProposition
) {
    public static SujetDto from(Sujet s) {
        return new SujetDto(
                s.getId(),
                s.getTitre(),
                s.getDescription(),
                s.getMotsCles(),
                s.getCapaciteMax(),
                s.getStatut(),
                s.getEncadrant().getId(),
                s.getEncadrant().getNomComplet(),
                s.getFiliere() != null ? s.getFiliere().getId() : null,
                s.getFiliere() != null ? s.getFiliere().getLibelle() : null,
                s.getCommentaireValidation(),
                s.getDateProposition()
        );
    }
}
