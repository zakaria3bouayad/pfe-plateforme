<script setup>
import { ref, onMounted } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'
import { LIBELLES_ROLE, COULEURS_ROLE } from '@/utils/statuts'

const auth = useAuthStore()

const utilisateurs = ref([])
const filtreRole = ref(null)
const chargement = ref(true)
const erreur = ref(null)
const succes = ref(null)

const filtres = [
  { titre: 'Tous', valeur: null },
  { titre: 'Étudiants', valeur: 'ETUDIANT' },
  { titre: 'Encadrants', valeur: 'ENCADRANT' },
  { titre: 'Administrateurs', valeur: 'ADMINISTRATEUR' },
]

async function charger() {
  chargement.value = true
  erreur.value = null
  try {
    const params = filtreRole.value ? { role: filtreRole.value } : {}
    const { data } = await api.get('/admin/utilisateurs', { params })
    utilisateurs.value = data
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

async function basculerActif(u) {
  const action = u.actif ? 'désactiver' : 'réactiver'
  if (!confirm(`Confirmer : ${action} le compte de ${u.nomComplet} ?`)) return

  erreur.value = null
  succes.value = null
  try {
    await api.patch(`/admin/utilisateurs/${u.id}/actif`, { actif: !u.actif })
    succes.value = `Compte de ${u.nomComplet} ${u.actif ? 'désactivé' : 'réactivé'}.`
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}
</script>

<template>
  <LayoutDashboard titre="Utilisateurs" icone="mdi-account-multiple-outline" couleur="deep-purple-darken-2">
    <div class="d-flex justify-space-between align-center mb-4 flex-wrap ga-2">
      <v-btn variant="text" to="/admin" prepend-icon="mdi-arrow-left">Retour</v-btn>
      <v-select
        v-model="filtreRole"
        :items="filtres"
        item-title="titre"
        item-value="valeur"
        label="Filtrer par rôle"
        variant="outlined"
        density="compact"
        hide-details
        style="max-width: 240px"
        @update:model-value="charger"
      />
    </div>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />
    <v-alert v-if="succes" type="success" variant="tonal" density="compact" class="mb-4" :text="succes" />

    <v-progress-linear v-if="chargement" indeterminate color="deep-purple-darken-2" class="mb-4" />

    <v-alert v-if="!chargement && utilisateurs.length === 0" type="info" variant="tonal">
      Aucun utilisateur pour ce filtre.
    </v-alert>

    <v-table v-else density="comfortable">
      <thead>
        <tr>
          <th>Nom</th>
          <th>Email</th>
          <th>Rôle</th>
          <th>Téléphone</th>
          <th>Statut</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="u in utilisateurs" :key="u.id">
          <td>{{ u.nomComplet }}</td>
          <td>{{ u.email }}</td>
          <td>
            <v-chip :color="COULEURS_ROLE[u.role]" size="small" variant="flat">
              {{ LIBELLES_ROLE[u.role] }}
            </v-chip>
          </td>
          <td>{{ u.telephone || '—' }}</td>
          <td>
            <v-chip :color="u.actif ? 'green' : 'grey'" size="small">
              {{ u.actif ? 'Actif' : 'Désactivé' }}
            </v-chip>
          </td>
          <td class="text-right">
            <v-btn
              v-if="u.email.toLowerCase() !== auth.utilisateur?.email?.toLowerCase()"
              size="small"
              variant="text"
              :color="u.actif ? 'error' : 'success'"
              @click="basculerActif(u)"
            >
              {{ u.actif ? 'Désactiver' : 'Réactiver' }}
            </v-btn>
            <span v-else class="text-caption text-medium-emphasis">Vous</span>
          </td>
        </tr>
      </tbody>
    </v-table>
  </LayoutDashboard>
</template>
