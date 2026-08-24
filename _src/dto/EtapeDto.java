package com.pfe.pfe_backend.dto;

import com.pfe.pfe_backend.domain.Etape;
import com.pfe.pfe_backend.domain.enums.StatutEtape;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representation d'un jalon renvoyee au frontend (Lot 3, bloc A).
 */
public record EtapeDto(
        Long id,
        Long projetId,
        String titre,
        String description,
        LocalDate dateEcheance,
        int ordre,
        StatutEtape statut,
        String lienLivrable,
        String commentaireSoumission,
        LocalDateTime dateSoumission,
        String commentaireValidation,
        LocalDateTime dateValidation,
        LocalDateTime dateCreation
) {
    public static EtapeDto from(Etape e) {
        return new EtapeDto(
                e.getId(),
                e.getProjet().getId(),
                e.getTitre(),
                e.getDescription(),
                e.getDateEcheance(),
                e.getOrdre(),
                e.getStatut(),
                e.getLienLivrable(),
                e.getCommentaireSoumission(),
                e.getDateSoumission(),
                e.getCommentaireValidation(),
                e.getDateValidation(),
                e.getDateCreation()
        );
    }
}
