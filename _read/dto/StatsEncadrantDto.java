package com.pfe.pfe_backend.dto;

/**
 * Statistiques du tableau de bord encadrant (Lot 3, bloc B) : etat de ses
 * sujets et des projets qu'il encadre, toutes equipes confondues.
 */
public record StatsEncadrantDto(
        int sujetsEnAttenteValidation,
        int projetsEnCours,
        int jalonsAValider,
        int jalonsEnRetard
) {}
