import api from './api'

/**
 * Declenche le telechargement du fichier d'une ressource (Lot 5, bloc B).
 * Meme principe que telechargerDocument (services/documents.js) : passe par
 * un blob car l'endpoint exige le jeton d'authentification.
 */
export async function telechargerRessource(ressourceId, nomSecours = 'ressource') {
  const reponse = await api.get(`/ressources/${ressourceId}/telecharger`, {
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
