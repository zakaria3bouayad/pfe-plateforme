<script setup>
import { ref, onMounted } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'

const projet = ref(null)
const chargement = ref(true)
const erreur = ref(null)

async function charger() {
  chargement.value = true
  erreur.value = null
  try {
    const { data } = await api.get('/projets/moi')
    projet.value = data
  } catch (e) {
    if (e.response?.status === 404) {
      projet.value = null
    } else {
      erreur.value = messageErreur(e)
    }
  } finally {
    chargement.value = false
  }
}

onMounted(charger)
</script>

<template>
  <LayoutDashboard titre="Mon projet" icone="mdi-folder-outline" couleur="primary">
    <v-btn variant="text" to="/etudiant" prepend-icon="mdi-arrow-left" class="mb-4">Retour</v-btn>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />

    <v-progress-linear v-if="chargement" indeterminate color="primary" class="mb-4" />

    <v-alert v-if="!chargement && !projet" type="info" variant="tonal">
      Aucun projet pour l'instant. Rejoignez ou créez une équipe, puis demandez un sujet validé depuis
      <router-link to="/etudiant/sujets">Sujets disponibles</router-link>.
    </v-alert>

    <v-card v-if="projet" variant="outlined" rounded="lg">
      <v-card-item>
        <v-card-title>{{ projet.sujetTitre }}</v-card-title>
        <v-card-subtitle>Encadrant : {{ projet.encadrantNom }}</v-card-subtitle>
      </v-card-item>
      <v-card-text>
        <p><strong>Équipe :</strong> {{ projet.equipeNom }}</p>
        <p><strong>Statut :</strong> {{ projet.statut }}</p>
        <p class="text-caption text-medium-emphasis">
          Affecté le {{ new Date(projet.dateAffectation).toLocaleDateString('fr-FR') }}
        </p>
      </v-card-text>
    </v-card>
  </LayoutDashboard>
</template>
