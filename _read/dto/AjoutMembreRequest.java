package com.pfe.pfe_backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Ajout d'un membre a une equipe, identifie par son numero etudiant (EF-11).
 * Reserve au chef d'equipe.
 */
public record AjoutMembreRequest(

        @NotBlank(message = "Le numero etudiant est obligatoire")
        String numeroEtudiant
) {}
