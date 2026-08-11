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
    path: '/etudiant/equipe',
    name: 'etudiant-equipe',
    component: () => import('@/views/EtudiantEquipeView.vue'),
    meta: { roles: ['ETUDIANT'] },
  },
  {
    path: '/etudiant/sujets',
    name: 'etudiant-sujets',
    component: () => import('@/views/EtudiantSujetsView.vue'),
    meta: { roles: ['ETUDIANT'] },
  },
  {
    path: '/etudiant/projet',
    name: 'etudiant-projet',
    component: () => import('@/views/EtudiantProjetView.vue'),
    meta: { roles: ['ETUDIANT'] },
  },
  {
    path: '/etudiant/jalons',
    name: 'etudiant-jalons',
    component: () => import('@/views/EtudiantJalonsView.vue'),
    meta: { roles: ['ETUDIANT'] },
  },
  {
    path: '/etudiant/documents',
    name: 'etudiant-documents',
    component: () => import('@/views/EtudiantDocumentsView.vue'),
    meta: { roles: ['ETUDIANT'] },
  },
  {
    path: '/encadrant',
    name: 'dashboard-encadrant',
    component: () => import('@/views/DashboardEncadrant.vue'),
    meta: { roles: ['ENCADRANT'] },
  },
  {
    path: '/encadrant/sujets',
    name: 'encadrant-sujets',
    component: () => import('@/views/EncadrantSujetsView.vue'),
    meta: { roles: ['ENCADRANT'] },
  },
  {
    path: '/encadrant/projets',
    name: 'encadrant-projets',
    component: () => import('@/views/EncadrantProjetsView.vue'),
    meta: { roles: ['ENCADRANT'] },
  },
  {
    path: '/encadrant/jalons',
    name: 'encadrant-jalons',
    component: () => import('@/views/EncadrantJalonsView.vue'),
    meta: { roles: ['ENCADRANT'] },
  },
  {
    path: '/encadrant/documents',
    name: 'encadrant-documents',
    component: () => import('@/views/EncadrantDocumentsView.vue'),
    meta: { roles: ['ENCADRANT'] },
  },
  {
    path: '/admin',
    name: 'dashboard-admin',
    component: () => import('@/views/DashboardAdmin.vue'),
    meta: { roles: ['ADMINISTRATEUR'] },
  },
  {
    path: '/admin/sujets',
    name: 'admin-sujets',
    component: () => import('@/views/AdminSujetsView.vue'),
    meta: { roles: ['ADMINISTRATEUR'] },
  },
  {
    path: '/admin/projets',
    name: 'admin-projets',
    component: () => import('@/views/AdminProjetsView.vue'),
    meta: { roles: ['ADMINISTRATEUR'] },
  },
  {
    path: '/admin/utilisateurs',
    name: 'admin-utilisateurs',
    component: () => import('@/views/AdminUtilisateursView.vue'),
    meta: { roles: ['ADMINISTRATEUR'] },
  },
  {
    path: '/admin/referentiel',
    name: 'admin-referentiel',
    component: () => import('@/views/AdminReferentielView.vue'),
    meta: { roles: ['ADMINISTRATEUR'] },
  },
  {
    path: '/admin/stats',
    name: 'admin-stats',
    component: () => import('@/views/AdminStatsView.vue'),
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
