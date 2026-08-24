package com.pfe.pfe_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Donnees de creation d'une equipe (EF-11). Reservee a l'etudiant qui devient chef.
 */
public record EquipeRequest(

        @NotBlank(message = "Le nom de l'equipe est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas depasser 100 caracteres")
        String nom,

        @NotNull(message = "La taille maximale est obligatoire")
        @Min(value = 1, message = "La taille minimale est de 1 etudiant")
        @Max(value = 4, message = "La taille maximale est de 4 etudiants")
        Integer tailleMax
) {}
