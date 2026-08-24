package com.pfe.pfe_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Donnees de creation/modification d'une promotion (EF-07).
 * Reservee a l'administrateur.
 */
public record PromotionRequest(

        @NotNull(message = "L'annee est obligatoire")
        @Min(value = 2000, message = "L'annee doit etre superieure ou egale a 2000")
        Integer annee,

        @NotBlank(message = "Le libelle est obligatoire")
        @Size(max = 60, message = "Le libelle ne doit pas depasser 60 caracteres")
        String libelle
) {}
