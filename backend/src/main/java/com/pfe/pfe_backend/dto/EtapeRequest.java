package com.pfe.pfe_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Donnees de creation/modification d'un jalon par l'encadrant (Lot 3, bloc A).
 */
public record EtapeRequest(

        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 150, message = "Le titre ne doit pas depasser 150 caracteres")
        String titre,

        @NotBlank(message = "La description est obligatoire")
        @Size(max = 2000, message = "La description ne doit pas depasser 2000 caracteres")
        String description,

        @NotNull(message = "La date d'echeance est obligatoire")
        LocalDate dateEcheance,

        @NotNull(message = "L'ordre est obligatoire")
        @Min(value = 1, message = "L'ordre doit etre d'au moins 1")
        Integer ordre
) {}
