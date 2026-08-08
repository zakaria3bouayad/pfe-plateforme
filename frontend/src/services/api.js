import axios from 'axios'

/**
 * Instance Axios unique de l'application.
 *
 * Deux intercepteurs :
 *  - requete  : ajoute automatiquement le jeton d'acces
 *  - reponse  : sur un 401, tente un renouvellement puis rejoue la requete
 *
 * Les jetons sont lus dans localStorage plutot que dans le store Pinia pour
 * eviter une dependance circulaire (le store importe ce fichier).
 */

const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8081/api'

export const CLE_ACCESS = 'pfe_access_token'
export const CLE_REFRESH = 'pfe_refresh_token'

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// ---------------------------------------------------------------- requete

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(CLE_ACCESS)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// ---------------------------------------------------------------- reponse

// Routes sur lesquelles un 401 ne doit PAS declencher de renouvellement.
const ROUTES_AUTH = ['/auth/login', '/auth/register', '/auth/refresh']

// Evite plusieurs appels simultanes a /refresh : les requetes en echec
// attendent le meme renouvellement.
let renouvellementEnCours = null

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const requeteInitiale = error.config

    const estAuth = ROUTES_AUTH.some((r) => requeteInitiale?.url?.includes(r))

    if (error.response?.status !== 401 || estAuth || requeteInitiale?._rejouee) {
      return Promise.reject(error)
    }

    // Requete marquee "silencieuse" : l'appelant gere lui-meme l'echec.
    if (requeteInitiale?.silencieux) {
      return Promise.reject(error)
    }

    // Aucune session en cours : l'utilisateur n'etait pas connecte, il n'y a
    // donc rien a renouveler ni a interrompre. La garde du routeur s'en charge.
    if (!localStorage.getItem(CLE_ACCESS)) {
      return Promise.reject(error)
    }

    const refreshToken = localStorage.getItem(CLE_REFRESH)
    if (!refreshToken) {
      deconnecter()
      return Promise.reject(error)
    }

    requeteInitiale._rejouee = true

    try {
      if (!renouvellementEnCours) {
        renouvellementEnCours = axios
          .post(`${BASE_URL}/auth/refresh`, { refreshToken })
          .then((reponse) => {
            const { accessToken, refreshToken: nouveauRefresh } = reponse.data
            localStorage.setItem(CLE_ACCESS, accessToken)
            localStorage.setItem(CLE_REFRESH, nouveauRefresh)
            return accessToken
          })
          .finally(() => {
            renouvellementEnCours = null
          })
      }

      const nouveauToken = await renouvellementEnCours
      requeteInitiale.headers.Authorization = `Bearer ${nouveauToken}`
      return api(requeteInitiale)
    } catch (e) {
      deconnecter()
      return Promise.reject(e)
    }
  },
)

function deconnecter() {
  localStorage.removeItem(CLE_ACCESS)
  localStorage.removeItem(CLE_REFRESH)
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

/** Extrait un message lisible depuis la reponse d'erreur du backend. */
export function messageErreur(error) {
  const data = error.response?.data
  if (data?.champs) {
    return Object.values(data.champs).join(' · ')
  }
  return data?.message || error.message || 'Une erreur est survenue'
}

export default api
