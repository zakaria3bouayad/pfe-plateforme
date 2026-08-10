<script setup>
import { ref, onMounted, computed } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'
import { LIBELLES_STATUT_PROJET, COULEURS_STATUT_PROJET } from '@/utils/statuts'

const projets = ref([])
const filtreStatut = ref(null)
const chargement = ref(true)
const erreur = ref(null)

const filtres = [
  { titre: 'Tous', valeur: null },
  { titre: 'Brouillon', valeur: 'BROUILLON' },
  { titre: 'Soumis', valeur: 'SOUMIS' },
  { titre: 'En cours', valeur: 'EN_COURS' },
  { titre: 'En révision', valeur: 'EN_REVISION' },
  { titre: 'Soutenu', valeur: 'SOUTENU' },
  { titre: 'Archivé', valeur: 'ARCHIVE' },
  { titre: 'Suspendu', valeur: 'SUSPENDU' },
  { titre: 'Rejeté', valeur: 'REJETE' },
]

async function charger() {
  chargement.value = true
  erreur.value = null
  try {
    const { data } = await api.get('/projets')
    projets.value = data
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

const projetsFiltres = computed(() => {
  if (!filtreStatut.value) return projets.value
  return projets.value.filter((p) => p.statut === filtreStatut.value)
})

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('fr-FR')
}
</script>

<template>
  <LayoutDashboard titre="Tous les projets" icone="mdi-folder-multiple-outline" couleur="deep-purple-darken-2">
    <div class="d-flex justify-space-between align-center mb-4 flex-wrap ga-2">
      <v-btn variant="text" to="/admin" prepend-icon="mdi-arrow-left">Retour</v-btn>
      <v-select
        v-model="filtreStatut"
        :items="filtres"
        item-title="titre"
        item-value="valeur"
        label="Filtrer par statut"
        variant="outlined"
        density="compact"
        hide-details
        style="max-width: 240px"
      />
    </div>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />

    <v-progress-linear v-if="chargement" indeterminate color="deep-purple-darken-2" class="mb-4" />

    <v-alert v-if="!chargement && projetsFiltres.length === 0" type="info" variant="tonal">
      Aucun projet pour ce filtre.
    </v-alert>

    <v-table v-else density="comfortable">
      <thead>
        <tr>
          <th>Sujet</th>
          <th>Équipe</th>
          <th>Encadrant</th>
          <th>Statut</th>
          <th>Affecté le</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="p in projetsFiltres" :key="p.id">
          <td>{{ p.sujetTitre }}</td>
          <td>{{ p.equipeNom }}</td>
          <td>{{ p.encadrantNom }}</td>
          <td>
            <v-chip :color="COULEURS_STATUT_PROJET[p.statut]" size="small" variant="flat">
              {{ LIBELLES_STATUT_PROJET[p.statut] }}
            </v-chip>
          </td>
          <td>{{ formatDate(p.dateAffectation) }}</td>
        </tr>
      </tbody>
    </v-table>
  </LayoutDashboard>
</template>
