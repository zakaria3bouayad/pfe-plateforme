package com.pfe.pfe_backend.dto;

import com.pfe.pfe_backend.domain.Projet;
import com.pfe.pfe_backend.domain.enums.StatutProjet;

import java.time.LocalDateTime;

/**
 * Representation d'un projet renvoyee au frontend (EF-12).
 */
public record ProjetDto(
        Long id,
        Long sujetId,
        String sujetTitre,
        Long equipeId,
        String equipeNom,
        Long encadrantId,
        String encadrantNom,
        StatutProjet statut,
        LocalDateTime dateAffectation
) {
    public static ProjetDto from(Projet p) {
        return new ProjetDto(
                p.getId(),
                p.getSujet().getId(),
                p.getSujet().getTitre(),
                p.getEquipe().getId(),
                p.getEquipe().getNom(),
                p.getEncadrant().getId(),
                p.getEncadrant().getNomComplet(),
                p.getStatut(),
                p.getDateAffectation()
        );
    }
}
