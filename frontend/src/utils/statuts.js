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

/** Cycle de vie d'un projet (figure 12 de la conception UML). */
export const LIBELLES_STATUT_PROJET = {
  BROUILLON: 'Brouillon',
  SOUMIS: 'Soumis',
  EN_COURS: 'En cours',
  EN_REVISION: 'En révision',
  SOUTENU: 'Soutenu',
  ARCHIVE: 'Archivé',
  SUSPENDU: 'Suspendu',
  REJETE: 'Rejeté',
}

export const COULEURS_STATUT_PROJET = {
  BROUILLON: 'grey',
  SOUMIS: 'blue',
  EN_COURS: 'teal-darken-2',
  EN_REVISION: 'orange',
  SOUTENU: 'green',
  ARCHIVE: 'grey-darken-2',
  SUSPENDU: 'orange-darken-3',
  REJETE: 'red',
}

/** Roles applicatifs (gestion des comptes par l'admin, EF-03). */
export const LIBELLES_ROLE = {
  ETUDIANT: 'Étudiant',
  ENCADRANT: 'Encadrant',
  ADMINISTRATEUR: 'Administrateur',
}

export const COULEURS_ROLE = {
  ETUDIANT: 'blue',
  ENCADRANT: 'teal-darken-2',
  ADMINISTRATEUR: 'deep-purple-darken-2',
}
