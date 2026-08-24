package com.pfe.pfe_backend.domain.enums;

/**
 * Categorise une {@link com.pfe.pfe_backend.domain.Notification}. Correspond
 * exactement aux evenements branches a l'etape 7.4 : sujet valide/rejete,
 * jalon soumis puis valide, document depose, nouveau message, similarite
 * SUSPECT.
 *
 * Sous-ensemble restreint declenchant en plus un mail (etape 7.3/7.4, decision
 * assumee) : SUJET_VALIDE, SUJET_REJETE, JALON_VALIDE, SIMILARITE_SUSPECT.
 */
public enum TypeNotification {

    /** Sujet propose par l'encadrant valide par l'administrateur. */
    SUJET_VALIDE,

    /** Sujet propose par l'encadrant rejete par l'administrateur. */
    SUJET_REJETE,

    /** Jalon soumis par l'equipe, a destination de l'encadrant. */
    JALON_SOUMIS,

    /** Jalon valide par l'encadrant, a destination de l'equipe. */
    JALON_VALIDE,

    /** Nouveau document depose sur un projet. */
    DOCUMENT_DEPOSE,

    /** Nouveau message recu dans la messagerie d'un projet. */
    NOUVEAU_MESSAGE,

    /** Rapport de similarite au niveau SUSPECT, a destination de l'encadrant. */
    SIMILARITE_SUSPECT
}
