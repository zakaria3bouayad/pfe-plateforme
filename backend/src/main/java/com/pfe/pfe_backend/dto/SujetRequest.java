package com.pfe.pfe_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Donnees de proposition/modification d'un sujet (EF-08).
 * Reservee a l'encadrant proprietaire.
 */
public record SujetRequest(

        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 150, message = "Le titre ne doit pas depasser 150 caracteres")
        String titre,

        @NotBlank(message = "La description est obligatoire")
        @Size(max = 2000, message = "La description ne doit pas depasser 2000 caracteres")
        String description,

        @Size(max = 200, message = "Les mots-cles ne doivent pas depasser 200 caracteres")
        String motsCles,

        @NotNull(message = "La capacite est obligatoire")
        @Min(value = 1, message = "La capacite doit etre d'au moins 1 etudiant")
        Integer capaciteMax,

        Long filiereId
) {}
