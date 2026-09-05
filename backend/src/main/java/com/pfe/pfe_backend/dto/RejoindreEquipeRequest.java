package com.pfe.pfe_backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de POST /api/equipes/rejoindre : remplace l'auto-inscription par
 * liste ouverte par une adhesion sur code d'invitation (hors plan initial,
 * ajoute en cours de lot 8).
 */
public record RejoindreEquipeRequest(
        @NotBlank(message = "Le code d'equipe est obligatoire")
        String code
) {}
