<script setup>
import { ref, onMounted } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'
import { LIBELLES_STATUT_PROJET, COULEURS_STATUT_PROJET } from '@/utils/statuts'

const projets = ref([])
const equipesParProjet = ref({})
const chargement = ref(true)
const erreur = ref(null)

async function charger() {
  chargement.value = true
  erreur.value = null
  try {
    const { data } = await api.get('/projets/mes-projets')
    projets.value = data

    const resultats = await Promise.all(
      data.map((p) => api.get(`/equipes/${p.equipeId}`).then((r) => [p.equipeId, r.data])),
    )
    equipesParProjet.value = Object.fromEntries(resultats)
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('fr-FR')
}
</script>

<template>
  <LayoutDashboard titre="Projets encadrés" icone="mdi-folder-multiple-outline" couleur="teal-darken-2">
    <v-btn variant="text" to="/encadrant" prepend-icon="mdi-arrow-left" class="mb-4">Retour</v-btn>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />

    <v-progress-linear v-if="chargement" indeterminate color="teal-darken-2" class="mb-4" />

    <v-alert v-if="!chargement && projets.length === 0" type="info" variant="tonal">
      Aucun projet encadré pour l'instant.
    </v-alert>

    <v-card v-for="p in projets" :key="p.id" variant="outlined" rounded="lg" class="mb-4">
      <v-card-item>
        <v-card-title class="d-flex align-center">
          {{ p.sujetTitre }}
          <v-chip :color="COULEURS_STATUT_PROJET[p.statut]" size="small" class="ml-3">
            {{ LIBELLES_STATUT_PROJET[p.statut] }}
          </v-chip>
        </v-card-title>
        <v-card-subtitle>Équipe : {{ p.equipeNom }} · affecté le {{ formatDate(p.dateAffectation) }}</v-card-subtitle>
      </v-card-item>

      <v-card-text>
        <p class="mb-2">
          <strong>Chef d'équipe :</strong> {{ equipesParProjet[p.equipeId]?.chefNom ?? '—' }}
        </p>
        <v-list v-if="equipesParProjet[p.equipeId]" density="compact">
          <v-list-item
            v-for="m in equipesParProjet[p.equipeId].membres"
            :key="m.id"
            :title="m.nomComplet"
            :subtitle="m.numeroEtudiant"
          >
            <template #prepend>
              <v-icon icon="mdi-account-outline" size="small" />
            </template>
          </v-list-item>
        </v-list>
      </v-card-text>

      <v-card-actions>
        <v-btn variant="text" size="small" prepend-icon="mdi-flag-checkered" to="/encadrant/jalons">
          Checkpoints
        </v-btn>
        <v-btn variant="text" size="small" prepend-icon="mdi-file-document-outline" to="/encadrant/documents">
          Documents
        </v-btn>
      </v-card-actions>
    </v-card>
  </LayoutDashboard>
</template>
