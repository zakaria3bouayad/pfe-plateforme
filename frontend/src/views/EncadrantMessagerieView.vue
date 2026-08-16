<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'

const auth = useAuthStore()

const projets = ref([])
const projetId = ref(null)
const messages = ref([])
const chargement = ref(true)
const chargementProjet = ref(false)
const erreur = ref(null)
const envoiEnCours = ref(false)
const nouveauMessage = ref('')
const zoneMessages = ref(null)

let intervalId = null

const projetActuel = computed(() => projets.value.find((p) => p.id === projetId.value) ?? null)

async function chargerProjets() {
  chargement.value = true
  erreur.value = null
  try {
    const { data } = await api.get('/projets/mes-projets')
    projets.value = data
    if (data.length > 0 && !projetId.value) {
      projetId.value = data[0].id
    }
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargement.value = false
  }
}

async function chargerMessages({ silencieux = false } = {}) {
  if (!projetId.value) {
    messages.value = []
    return
  }
  if (!silencieux) chargementProjet.value = true
  try {
    const { data } = await api.get(`/projets/${projetId.value}/messages`, { silencieux })
    messages.value = data
    if (!silencieux) {
      await defilerVersLeBas()
    }
  } catch (e) {
    if (!silencieux) {
      erreur.value = messageErreur(e)
    }
  } finally {
    if (!silencieux) chargementProjet.value = false
  }
}

onMounted(async () => {
  await chargerProjets()
  await chargerMessages()
  // Pas de WebSocket (choix assume, Lot 5) : rafraichissement leger par sondage.
  intervalId = setInterval(() => chargerMessages({ silencieux: true }), 8000)
})

onUnmounted(() => {
  if (intervalId) clearInterval(intervalId)
})

watch(projetId, () => chargerMessages())

async function envoyer() {
  const contenu = nouveauMessage.value.trim()
  if (!contenu || !projetId.value) return

  envoiEnCours.value = true
  erreur.value = null
  try {
    const { data } = await api.post(`/projets/${projetId.value}/messages`, { contenu })
    messages.value.push(data)
    nouveauMessage.value = ''
    await defilerVersLeBas()
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    envoiEnCours.value = false
  }
}

function surEntree(e) {
  if (e.shiftKey) return
  e.preventDefault()
  envoyer()
}

async function defilerVersLeBas() {
  await nextTick()
  if (zoneMessages.value) {
    zoneMessages.value.scrollTop = zoneMessages.value.scrollHeight
  }
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('fr-FR')
}

const estMoi = (m) => m.auteurId === auth.utilisateur?.id
</script>

<template>
  <LayoutDashboard titre="Messagerie" icone="mdi-forum-outline" couleur="teal-darken-2">
    <v-btn variant="text" to="/encadrant" prepend-icon="mdi-arrow-left" class="mb-4">Retour</v-btn>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />

    <v-progress-linear v-if="chargement" indeterminate color="teal-darken-2" class="mb-4" />

    <v-alert v-if="!chargement && projets.length === 0" type="info" variant="tonal">
      Aucun projet encadré pour l'instant.
    </v-alert>

    <v-select
      v-if="projets.length > 0"
      v-model="projetId"
      :items="projets"
      item-title="sujetTitre"
      item-value="id"
      label="Projet"
      variant="outlined"
      density="comfortable"
      class="mb-4"
    />

    <v-progress-linear v-if="chargementProjet" indeterminate color="teal-darken-2" class="mb-4" />

    <v-card v-if="!chargementProjet && projetId" variant="outlined" rounded="lg">
      <v-card-item>
        <v-card-title>Discussion — {{ projetActuel?.equipeNom }}</v-card-title>
        <v-card-subtitle>{{ projetActuel?.sujetTitre }}</v-card-subtitle>
      </v-card-item>

      <v-divider />

      <div ref="zoneMessages" class="messagerie-zone pa-4">
        <p v-if="messages.length === 0" class="text-caption text-medium-emphasis text-center">
          Aucun message pour l'instant. Écrivez le premier ci-dessous.
        </p>

        <div
          v-for="m in messages"
          :key="m.id"
          class="d-flex mb-3"
          :class="estMoi(m) ? 'justify-end' : 'justify-start'"
        >
          <div class="bulle" :class="estMoi(m) ? 'bulle-moi bg-teal-darken-2 text-white' : 'bulle-autre'">
            <div v-if="!estMoi(m)" class="text-caption font-weight-medium mb-1">{{ m.auteurNom }}</div>
            <div class="text-body-2 text-wrap">{{ m.contenu }}</div>
            <div class="text-caption text-right mt-1 opacity-70">{{ formatDate(m.dateEnvoi) }}</div>
          </div>
        </div>
      </div>

      <v-divider />

      <v-card-text class="d-flex align-end ga-2">
        <v-textarea
          v-model="nouveauMessage"
          label="Votre message"
          variant="outlined"
          density="comfortable"
          rows="1"
          auto-grow
          max-rows="4"
          hide-details
          @keydown.enter="surEntree"
        />
        <v-btn
          icon="mdi-send"
          color="teal-darken-2"
          :loading="envoiEnCours"
          :disabled="!nouveauMessage.trim()"
          @click="envoyer"
        />
      </v-card-text>
    </v-card>
  </LayoutDashboard>
</template>

<style scoped>
.messagerie-zone {
  max-height: 480px;
  min-height: 200px;
  overflow-y: auto;
}

.bulle {
  max-width: 75%;
  padding: 8px 12px;
  border-radius: 12px;
}

.bulle-autre {
  background-color: rgba(var(--v-theme-on-surface), 0.06);
}
</style>
