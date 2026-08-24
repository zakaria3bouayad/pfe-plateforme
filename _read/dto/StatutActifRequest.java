package com.pfe.pfe_backend.dto;

/**
 * Activation ou desactivation logique d'un compte par l'administrateur.
 * Un compte n'est jamais supprime, seulement desactive (EF-03).
 */
public record StatutActifRequest(boolean actif) {}
