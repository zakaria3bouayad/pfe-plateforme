<script setup>
import { ref, onMounted, computed } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'

const auth = useAuthStore()

const sujets = ref([])
const equipe = ref(null)
const chargement = ref(true)
const erreur = ref(null)
const succes = ref(null)

const estChef = computed(() => equipe.value && equipe.value.chefId === auth.utilisateur?.id)

async function chargerEquipe() {
  try {
    const { data } = await api.get('/equipes/moi')
    equipe.value = data
  } catch (e) {
    if (e.response?.status === 404) {
      equipe.value = null
    } else {
      throw e
    }
  }
}

async function charger() {
  chargement.value = true
  erreur.value = null
  try {
    const [rs] = await Promise.all([api.get('/sujets', { params: { statut: 'VALIDE' } }), chargerEquipe()])
    sujets.value = rs.data
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

async function demander(sujet) {
  erreur.value = null
  succes.value = null
  try {
    await api.post('/projets', { sujetId: sujet.id })
    succes.value = `Sujet "${sujet.titre}" affecté à votre équipe !`
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}
</script>

<template>
  <LayoutDashboard titre="Sujets disponibles" icone="mdi-lightbulb-outline" couleur="primary">
    <v-btn variant="text" to="/etudiant" prepend-icon="mdi-arrow-left" class="mb-4">Retour</v-btn>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />
    <v-alert v-if="succes" type="success" variant="tonal" density="compact" class="mb-4" :text="succes" />

    <v-alert v-if="!chargement && !equipe" type="warning" variant="tonal" class="mb-4">
      Vous devez appartenir à une équipe avant de pouvoir demander un sujet.
    </v-alert>
    <v-alert v-else-if="!chargement && !estChef" type="info" variant="tonal" class="mb-4">
      Seul le chef de votre équipe peut demander l'affectation d'un sujet.
    </v-alert>

    <v-progress-linear v-if="chargement" indeterminate color="primary" class="mb-4" />

    <v-alert v-if="!chargement && sujets.length === 0" type="info" variant="tonal">
      Aucun sujet validé disponible pour l'instant.
    </v-alert>

    <v-row>
      <v-col v-for="s in sujets" :key="s.id" cols="12" md="6">
        <v-card variant="outlined" rounded="lg">
          <v-card-item>
            <v-card-title>{{ s.titre }}</v-card-title>
            <v-card-subtitle>
              {{ s.encadrantNom }}<span v-if="s.filiereLibelle"> · {{ s.filiereLibelle }}</span>
            </v-card-subtitle>
          </v-card-item>
          <v-card-text>
            <p class="mb-2">{{ s.description }}</p>
            <p v-if="s.motsCles" class="text-caption text-medium-emphasis">Mots-clés : {{ s.motsCles }}</p>
            <p class="text-caption text-medium-emphasis">Capacité : {{ s.capaciteMax }} étudiant(s)</p>
          </v-card-text>
          <v-card-actions v-if="estChef">
            <v-btn color="primary" variant="tonal" @click="demander(s)">Demander ce sujet</v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
  </LayoutDashboard>
</template>
