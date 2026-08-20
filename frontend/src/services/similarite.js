/**
 * Helpers d'affichage des rapports de similarite (Lot 6, etapes 6.7 et 6.8).
 *
 * Regroupes ici plutot que dupliques dans chaque vue : le vocabulaire employe
 * pour qualifier un rapprochement doit rester rigoureusement identique cote
 * encadrant et cote administrateur. Un meme score ne peut pas s'afficher
 * "suspect" a l'un et "eleve" a l'autre.
 */

/** Couleur Vuetify associee a un niveau. Le rouge est reserve au seul niveau SUSPECT. */
export function couleurNiveau(niveau) {
  switch (niveau) {
    case 'SUSPECT':
      return 'error'
    case 'ATTENTION':
      return 'warning'
    default:
      return 'success'
  }
}

export function iconeNiveau(niveau) {
  switch (niveau) {
    case 'SUSPECT':
      return 'mdi-alert-octagon-outline'
    case 'ATTENTION':
      return 'mdi-alert-outline'
    default:
      return 'mdi-check-circle-outline'
  }
}

/**
 * Libelles volontairement prudents : aucun ne prononce le mot "plagiat".
 * Un rapport signale une proximite de formulation, il ne qualifie pas une
 * faute - cette qualification appartient a l'encadrant, apres lecture.
 */
export function libelleNiveau(niveau) {
  switch (niveau) {
    case 'SUSPECT':
      return 'Proximité forte'
    case 'ATTENTION':
      return 'À vérifier'
    default:
      return 'Rien à signaler'
  }
}

/** Phrase d'explication affichee sous le score, pour eviter toute lecture hative. */
export function explicationNiveau(niveau) {
  switch (niveau) {
    case 'SUSPECT':
      return 'Des passages se ressemblent trop pour que ce soit fortuit. À lire avant tout jugement.'
    case 'ATTENTION':
      return 'Proximité réelle, mais qui peut venir d’un domaine ou de sources communes.'
    default:
      return 'Aucune proximité notable avec les rapports archivés.'
  }
}

/** Statuts d'indexation, employes cote administrateur (etape 6.8). */
export function libelleStatutIndexation(statut) {
  switch (statut) {
    case 'VECTORISE':
      return 'Indexé'
    case 'EXTRAIT':
      return 'Texte extrait, vectorisation en attente'
    case 'ECHEC_EMBEDDING':
      return 'Échec du calcul des vecteurs'
    case 'VIDE':
      return 'Aucun texte (PDF scanné)'
    case 'TYPE_NON_SUPPORTE':
      return 'Format non traité'
    case 'ECHEC':
      return 'Fichier illisible'
    default:
      return 'Non indexé'
  }
}

export function couleurStatutIndexation(statut) {
  switch (statut) {
    case 'VECTORISE':
      return 'success'
    case 'EXTRAIT':
      return 'info'
    case 'VIDE':
    case 'TYPE_NON_SUPPORTE':
      return 'warning'
    default:
      return 'error'
  }
}

export function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('fr-FR')
}
