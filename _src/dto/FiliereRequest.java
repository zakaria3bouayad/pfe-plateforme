package com.pfe.pfe_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Donnees de creation/modification d'une filiere (EF-07).
 * Reservee a l'administrateur.
 */
public record FiliereRequest(

        @NotBlank(message = "Le code est obligatoire")
        @Size(max = 10, message = "Le code ne doit pas depasser 10 caracteres")
        String code,

        @NotBlank(message = "Le libelle est obligatoire")
        @Size(max = 100, message = "Le libelle ne doit pas depasser 100 caracteres")
        String libelle,

        @Size(max = 100, message = "Le departement ne doit pas depasser 100 caracteres")
        String departement
) {}
