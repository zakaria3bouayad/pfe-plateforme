package com.pfe.pfe_backend.domain.enums;

/**
 * Etat d'un jalon. EN_RETARD est calcule automatiquement (EF-26).
 */
public enum StatutEtape {
    A_FAIRE,
    EN_COURS,
    SOUMISE,
    VALIDEE,
    EN_RETARD
}
