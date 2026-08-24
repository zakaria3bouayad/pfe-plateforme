package com.pfe.pfe_backend.dto;

/**
 * Statistiques du tableau de bord administrateur (Lot 3, bloc B) : vue
 * globale de la plateforme.
 */
public record StatsAdminDto(
        long totalEtudiants,
        long totalEncadrants,
        long totalEquipes,
        long totalSujets,
        long sujetsEnAttenteValidation,
        long totalProjets,
        long projetsEnCours,
        long totalJalonsEnRetard
) {}
