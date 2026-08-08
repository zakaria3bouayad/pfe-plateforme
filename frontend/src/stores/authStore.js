import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api, { CLE_ACCESS, CLE_REFRESH, messageErreur } from '@/services/api'

/**
 * Etat d'authentification partage par toute l'application.
 *
 * Les jetons vivent dans localStorage (source de verite, lue par l'intercepteur
 * Axios) ; le store expose l'utilisateur courant et les helpers de role.
 */
export const useAuthStore = defineStore('auth', () => {
  // ------------------------------------------------------------ etat
  const utilisateur = ref(null)
  const chargement = ref(false)
  const erreur = ref(null)

  // ------------------------------------------------------------ getters
  const estConnecte = computed(() => !!utilisateur.value)
  const role = computed(() => utilisateur.value?.role ?? null)
  const estEtudiant = computed(() => role.value === 'ETUDIANT')
  const estEncadrant = computed(() => role.value === 'ENCADRANT')
  const estAdmin = computed(() => role.value === 'ADMINISTRATEUR')
  const nomComplet = computed(() => utilisateur.value?.nomComplet ?? '')

  // ------------------------------------------------------------ actions

  function enregistrerSession(donnees) {
    localStorage.setItem(CLE_ACCESS, donnees.accessToken)
    localStorage.setItem(CLE_REFRESH, donnees.refreshToken)
    utilisateur.value = donnees.utilisateur
  }

  async function connecter(email, motDePasse) {
    chargement.value = true
    erreur.value = null
    try {
      const { data } = await api.post('/auth/login', { email, motDePasse })
      enregistrerSession(data)
      return true
    } catch (e) {
      erreur.value = messageErreur(e)
      return false
    } finally {
      chargement.value = false
    }
  }

  async function inscrire(formulaire) {
    chargement.value = true
    erreur.value = null
    try {
      const { data } = await api.post('/auth/register', formulaire)
      enregistrerSession(data)
      return true
    } catch (e) {
      erreur.value = messageErreur(e)
      return false
    } finally {
      chargement.value = false
    }
  }

  /** Recharge le profil depuis /auth/me : utilise au demarrage de l'application. */
  async function chargerProfil() {
    if (!localStorage.getItem(CLE_ACCESS)) {
      return false
    }
    try {
      const { data } = await api.get('/auth/me')
      utilisateur.value = data
      return true
    } catch {
      deconnecter()
      return false
    }
  }

  function deconnecter() {
    localStorage.removeItem(CLE_ACCESS)
    localStorage.removeItem(CLE_REFRESH)
    utilisateur.value = null
    erreur.value = null
  }

  /** Route d'accueil selon le role, utilisee apres connexion. */
  function accueilSelonRole() {
    switch (role.value) {
      case 'ETUDIANT':
        return '/etudiant'
      case 'ENCADRANT':
        return '/encadrant'
      case 'ADMINISTRATEUR':
        return '/admin'
      default:
        return '/login'
    }
  }

  return {
    utilisateur,
    chargement,
    erreur,
    estConnecte,
    role,
    estEtudiant,
    estEncadrant,
    estAdmin,
    nomComplet,
    connecter,
    inscrire,
    chargerProfil,
    deconnecter,
    accueilSelonRole,
  }
})
