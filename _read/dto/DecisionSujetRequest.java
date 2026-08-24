package com.pfe.pfe_backend.dto;

/**
 * Decision de l'administrateur sur un sujet en cours de validation (EF-09).
 *
 * Le commentaire est obligatoire pour un rejet ou une demande de correction,
 * facultatif pour une validation : cette regle conditionnelle est verifiee
 * dans SujetService, pas ici.
 */
public record DecisionSujetRequest(String commentaire) {}
