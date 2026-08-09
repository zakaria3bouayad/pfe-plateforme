package com.pfe.pfe_backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Demande d'affectation d'un sujet valide a l'equipe du chef connecte (EF-12).
 */
public record ProjetRequest(

        @NotNull(message = "Le sujet est obligatoire")
        Long sujetId
) {}
