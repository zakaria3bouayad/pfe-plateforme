/**
 * Helpers d'affichage des notifications (Lot 7, etapes 7.10 et 7.11).
 *
 * Regroupes ici plutot que dupliques entre la cloche (etape 7.10) et la page
 * recapitulative (etape 7.11), sur le meme principe que services/similarite.js :
 * le libelle et l'icone d'un type doivent rester rigoureusement identiques
 * partout ou une notification s'affiche.
 */

const LIBELLES = {
  SUJET_VALIDE: 'Sujet validé',
  SUJET_REJETE: 'Sujet rejeté',
  JALON_SOUMIS: 'Checkpoint soumis',
  JALON_VALIDE: 'Checkpoint validé',
  DOCUMENT_DEPOSE: 'Nouveau document',
  NOUVEAU_MESSAGE: 'Nouveau message',
  SIMILARITE_SUSPECT: 'Similarité suspecte',
}

const ICONES = {
  SUJET_VALIDE: 'mdi-check-circle-outline',
  SUJET_REJETE: 'mdi-close-circle-outline',
  JALON_SOUMIS: 'mdi-flag-outline',
  JALON_VALIDE: 'mdi-flag-checkered',
  DOCUMENT_DEPOSE: 'mdi-file-document-outline',
  NOUVEAU_MESSAGE: 'mdi-forum-outline',
  SIMILARITE_SUSPECT: 'mdi-alert-octagon-outline',
}

export function libelleTypeNotification(type) {
  return LIBELLES[type] ?? type
}

export function iconeTypeNotification(type) {
  return ICONES[type] ?? 'mdi-bell-outline'
}

/** Le rouge reste reserve au seul type SIMILARITE_SUSPECT, comme le niveau SUSPECT dans services/similarite.js. */
export function couleurTypeNotification(type) {
  switch (type) {
    case 'SIMILARITE_SUSPECT':
      return 'error'
    case 'SUJET_REJETE':
      return 'warning'
    default:
      return 'primary'
  }
}

export function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('fr-FR')
}
