package com.pfe.pfe_backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Representation d'une equipe renvoyee au frontend (EF-11).
 */
public record EquipeDto(
        Long id,
        String nom,
        int tailleMax,
        Long chefId,
        String chefNom,
        List<MembreEquipeDto> membres,
        LocalDateTime dateCreation
) {}
