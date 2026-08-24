package com.pfe.pfe_backend.dto;

import com.pfe.pfe_backend.domain.Promotion;

/**
 * Representation d'une promotion renvoyee au frontend (EF-07).
 */
public record PromotionDto(
        Long id,
        int annee,
        String libelle
) {
    public static PromotionDto from(Promotion p) {
        return new PromotionDto(p.getId(), p.getAnnee(), p.getLibelle());
    }
}
