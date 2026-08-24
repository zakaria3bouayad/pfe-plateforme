package com.pfe.pfe_backend.dto;

/**
 * Membre d'une equipe, tel qu'affiche dans la liste des membres (EF-11).
 */
public record MembreEquipeDto(
        Long id,
        String nomComplet,
        String numeroEtudiant
) {}
