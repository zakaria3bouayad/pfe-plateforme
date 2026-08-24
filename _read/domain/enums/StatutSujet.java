package com.pfe.pfe_backend.domain.enums;

/**
 * Cycle de vie d'un sujet de PFE (figure 13 de la conception UML).
 */
public enum StatutSujet {
    PROPOSE,
    EN_VALIDATION,
    A_CORRIGER,
    VALIDE,
    REJETE,
    AFFECTE,
    CLOTURE
}
