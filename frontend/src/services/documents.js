import api from './api'

/**
 * Declenche le telechargement d'un document dans le navigateur (Lot 4).
 * Passe par un blob (et non un lien direct) car l'endpoint exige le jeton
 * d'authentification, que seul l'intercepteur Axios sait fournir.
 */
export async function telechargerDocument(documentId, nomSecours = 'document') {
  const reponse = await api.get(`/documents/${documentId}/telecharger`, {
    responseType: 'blob',
  })

  const nom = extraireNomFichier(reponse.headers['content-disposition']) || nomSecours

  const url = window.URL.createObjectURL(reponse.data)
  const lien = document.createElement('a')
  lien.href = url
  lien.download = nom
  document.body.appendChild(lien)
  lien.click()
  lien.remove()
  window.URL.revokeObjectURL(url)
}

function extraireNomFichier(contentDisposition) {
  if (!contentDisposition) return null
  const correspondance = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(contentDisposition)
  return correspondance ? decodeURIComponent(correspondance[1]) : null
}

/** Un v-file-input Vuetify peut renvoyer un File seul ou un tableau selon le contexte : normalise. */
export function premierFichier(valeur) {
  if (Array.isArray(valeur)) return valeur[0] ?? null
  return valeur ?? null
}

/** Formate une taille en octets pour affichage (o / Ko / Mo). */
export function formatTaille(octets) {
  if (octets == null) return '—'
  if (octets < 1024) return `${octets} o`
  if (octets < 1024 * 1024) return `${(octets / 1024).toFixed(1)} Ko`
  return `${(octets / (1024 * 1024)).toFixed(1)} Mo`
}
