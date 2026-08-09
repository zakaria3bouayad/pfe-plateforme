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
