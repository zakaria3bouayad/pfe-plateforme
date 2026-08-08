import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const routes = [
  { path: '/', redirect: '/login' },

  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { anonyme: true },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { anonyme: true },
  },

  {
    path: '/etudiant',
    name: 'dashboard-etudiant',
    component: () => import('@/views/DashboardEtudiant.vue'),
    meta: { roles: ['ETUDIANT'] },
  },
  {
    path: '/encadrant',
    name: 'dashboard-encadrant',
    component: () => import('@/views/DashboardEncadrant.vue'),
    meta: { roles: ['ENCADRANT'] },
  },
  {
    path: '/admin',
    name: 'dashboard-admin',
    component: () => import('@/views/DashboardAdmin.vue'),
    meta: { roles: ['ADMINISTRATEUR'] },
  },

  { path: '/:pathMatch(.*)*', redirect: '/login' },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

/**
 * Garde globale.
 *
 * Rappel : ce controle est un confort d'interface, pas une mesure de securite.
 * La protection reelle est appliquee par Spring Security cote serveur (ENF-08) :
 * modifier ce fichier dans le navigateur ne donne acces a aucune donnee.
 */
router.beforeEach(async (to) => {
  const auth = useAuthStore()

  // Restaure la session apres un rafraichissement de page (F5).
  if (!auth.estConnecte && localStorage.getItem('pfe_access_token')) {
    await auth.chargerProfil()
  }

  // Route publique : un utilisateur deja connecte est renvoye vers son accueil.
  if (to.meta.anonyme) {
    return auth.estConnecte ? auth.accueilSelonRole() : true
  }

  if (!auth.estConnecte) {
    return { name: 'login' }
  }

  // Role insuffisant : redirection vers son propre espace.
  if (to.meta.roles && !to.meta.roles.includes(auth.role)) {
    return auth.accueilSelonRole()
  }

  return true
})

export default router
