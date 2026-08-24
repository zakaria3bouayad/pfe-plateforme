package com.pfe.pfe_backend.dto;

import java.time.LocalDate;

/**
 * Statistiques du tableau de bord etudiant (Lot 3, bloc B) : avancement des
 * jalons de son projet. Nulle si l'etudiant n'a pas encore de projet.
 */
public record StatsEtudiantDto(
        Long projetId,
        String projetStatut,
        int totalJalons,
        int jalonsValides,
        int jalonsEnRetard,
        int jalonsRestants,
        LocalDate prochaineEcheance
) {}
