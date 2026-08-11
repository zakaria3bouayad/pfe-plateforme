<script setup>
import { ref, onMounted, computed } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'

const stats = ref(null)
const chargement = ref(true)
const erreur = ref(null)

async function charger() {
  chargement.value = true
  erreur.value = null
  try {
    const { data } = await api.get('/stats/admin')
    stats.value = data
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

const tuiles = computed(() => {
  const s = stats.value
  if (!s) return []
  return [
    { titre: 'Étudiants', valeur: s.totalEtudiants, icone: 'mdi-account-school-outline', couleur: 'deep-purple-darken-2' },
    { titre: 'Encadrants', valeur: s.totalEncadrants, icone: 'mdi-account-tie-outline', couleur: 'deep-purple-darken-2' },
    { titre: 'Équipes', valeur: s.totalEquipes, icone: 'mdi-account-group-outline', couleur: 'deep-purple-darken-2' },
    { titre: 'Sujets', valeur: s.totalSujets, icone: 'mdi-lightbulb-outline', couleur: 'deep-purple-darken-2' },
    {
      titre: 'Sujets en attente de validation',
      valeur: s.sujetsEnAttenteValidation,
      icone: 'mdi-clock-alert-outline',
      couleur: s.sujetsEnAttenteValidation > 0 ? 'orange-darken-2' : 'grey',
    },
    { titre: 'Projets', valeur: s.totalProjets, icone: 'mdi-folder-multiple-outline', couleur: 'deep-purple-darken-2' },
    { titre: 'Projets en cours', valeur: s.projetsEnCours, icone: 'mdi-progress-clock', couleur: 'deep-purple-darken-2' },
    {
      titre: 'Checkpoints en retard',
      valeur: s.totalJalonsEnRetard,
      icone: 'mdi-alert-circle-outline',
      couleur: s.totalJalonsEnRetard > 0 ? 'red-darken-1' : 'grey',
    },
  ]
})
</script>

<template>
  <LayoutDashboard titre="Statistiques" icone="mdi-chart-box-outline" couleur="deep-purple-darken-2">
    <v-btn variant="text" to="/admin" prepend-icon="mdi-arrow-left" class="mb-4">Retour</v-btn>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />

    <v-progress-linear v-if="chargement" indeterminate color="deep-purple-darken-2" class="mb-4" />

    <v-row v-if="!chargement && stats">
      <v-col v-for="t in tuiles" :key="t.titre" cols="12" sm="6" md="3">
        <v-card variant="outlined" rounded="lg" class="h-100">
          <v-card-item>
            <v-icon :icon="t.icone" size="28" :color="t.couleur" />
            <div class="text-h4 font-weight-bold mt-2">{{ t.valeur }}</div>
            <v-card-subtitle class="px-0">{{ t.titre }}</v-card-subtitle>
          </v-card-item>
        </v-card>
      </v-col>
    </v-row>
  </LayoutDashboard>
</template>
