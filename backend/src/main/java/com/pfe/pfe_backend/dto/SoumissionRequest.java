package com.pfe.pfe_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Soumission d'un jalon par le chef d'equipe (Lot 3, bloc A). Pas de stockage
 * de fichier avant le lot 4 : le livrable est reference par un lien (ex. Drive,
 * GitHub) plutot que televerse.
 */
public record SoumissionRequest(

        @NotBlank(message = "Le lien vers le livrable est obligatoire")
        @Size(max = 500, message = "Le lien ne doit pas depasser 500 caracteres")
        String lienLivrable,

        @Size(max = 1000, message = "Le commentaire ne doit pas depasser 1000 caracteres")
        String commentaire
) {}
