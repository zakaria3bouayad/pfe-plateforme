package com.pfe.pfe_backend.dto;

import com.pfe.pfe_backend.domain.Filiere;

/**
 * Representation d'une filiere renvoyee au frontend (EF-07).
 */
public record FiliereDto(
        Long id,
        String code,
        String libelle,
        String departement
) {
    public static FiliereDto from(Filiere f) {
        return new FiliereDto(f.getId(), f.getCode(), f.getLibelle(), f.getDepartement());
    }
}
