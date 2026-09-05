package com.pfe.pfe_backend.dto;

/**
 * Reponse de POST /api/assistant/escalade (etape 8.9) : de quoi afficher
 * cote frontend (etape 8.11) les coordonnees de l'encadrant a contacter.
 */
public record EscaladeResponse(
        String message,
        String encadrantNom,
        String encadrantEmail
) {}
