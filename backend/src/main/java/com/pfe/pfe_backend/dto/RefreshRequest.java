package com.pfe.pfe_backend.dto;

import jakarta.validation.constraints.NotBlank;

/** Demande de renouvellement du jeton d'acces (EF-01). */
public record RefreshRequest(

        @NotBlank(message = "Le refresh token est obligatoire")
        String refreshToken
) {}
