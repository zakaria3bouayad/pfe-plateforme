/**
 * Libelles et couleurs des statuts affiches dans l'interface.
 * Centralise ici pour eviter les incoherences entre les vues encadrant,
 * etudiant et admin qui affichent toutes des sujets.
 */
export const LIBELLES_STATUT_SUJET = {
  PROPOSE: 'Proposé',
  EN_VALIDATION: 'En validation',
  A_CORRIGER: 'À corriger',
  VALIDE: 'Validé',
  REJETE: 'Rejeté',
  AFFECTE: 'Affecté',
  CLOTURE: 'Clôturé',
}

export const COULEURS_STATUT_SUJET = {
  PROPOSE: 'grey',
  EN_VALIDATION: 'blue',
  A_CORRIGER: 'orange',
  VALIDE: 'green',
  REJETE: 'red',
  AFFECTE: 'teal',
  CLOTURE: 'grey-darken-2',
}

/** Statuts d'un jalon (Lot 3). EN_RETARD est calcule automatiquement (EF-26). */
export const LIBELLES_STATUT_ETAPE = {
  A_FAIRE: 'À faire',
  EN_COURS: 'En cours',
  SOUMISE: 'Soumis',
  VALIDEE: 'Validé',
  EN_RETARD: 'En retard',
}

export const COULEURS_STATUT_ETAPE = {
  A_FAIRE: 'grey',
  EN_COURS: 'blue',
  SOUMISE: 'orange',
  VALIDEE: 'green',
  EN_RETARD: 'red',
}
