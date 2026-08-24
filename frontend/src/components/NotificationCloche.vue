<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import api, { messageErreur } from '@/services/api'
import {
  libelleTypeNotification,
  iconeTypeNotification,
  couleurTypeNotification,
  formatDate,
} from '@/services/notifications'

/**
 * Cloche de notifications (Lot 7, etape 7.10), presente sur tous les
 * tableaux de bord via LayoutDashboard : les notifications sont ouvertes a
 * tout compte authentifie, quel que soit son role (cf. NotificationController).
 *
 * Le compteur est interroge en continu (silencieux : un echec ne doit jamais
 * gener la navigation) pour rester a jour meme si l'utilisateur reste sur la
 * meme page ; la liste complete n'est chargee qu'a l'ouverture du menu.
 */

const notifications = ref([])
const compteur = ref(0)
const chargement = ref(false)
const erreur = ref(null)

const compteurAffiche = computed(() => (compteur.value > 99 ? '99+' : compteur.value))
const aDesNonLues = computed(() => notifications.value.some((n) => !n.lue))

let intervalle = null

async function chargerCompteur() {
  try {
    const { data } = await api.get('/notifications/compteur', { silencieux: true })
    compteur.value = data
  } catch {
    // le badge n'est qu'un confort d'affichage : une erreur ici reste silencieuse
  }
}

async function chargerListe() {
  chargement.value = true
  erreur.value = null
  try {
    const { data } = await api.get('/notifications')
    notifications.value = data
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargement.value = false
  }
}

function surOuverture(ouvert) {
  if (ouvert) chargerListe()
}

async function marquerLu(notification) {
  if (notification.lue) return
  try {
    await api.patch(`/notifications/${notification.id}/lu`)
    notification.lue = true
    compteur.value = Math.max(0, compteur.value - 1)
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

async function toutMarquerLu() {
  try {
    await api.patch('/notifications/lu')
    notifications.value.forEach((n) => (n.lue = true))
    compteur.value = 0
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

onMounted(() => {
  chargerCompteur()
  intervalle = setInterval(chargerCompteur, 30000)
})
onUnmounted(() => {
  if (intervalle) clearInterval(intervalle)
})
</script>

<template>
  <v-menu location="bottom end" :close-on-content-click="false" max-width="380" @update:model-value="surOuverture">
    <template #activator="{ props }">
      <v-btn icon variant="text" v-bind="props" title="Notifications">
        <v-badge :content="compteurAffiche" :model-value="compteur > 0" color="error" floating>
          <v-icon icon="mdi-bell-outline" />
        </v-badge>
      </v-btn>
    </template>

    <v-card min-width="320" max-width="380">
      <v-card-title class="d-flex align-center justify-space-between text-subtitle-1">
        Notifications
        <v-btn v-if="aDesNonLues" variant="text" size="small" density="compact" @click="toutMarquerLu">
          Tout marquer lu
        </v-btn>
      </v-card-title>
      <v-divider />

      <v-progress-linear v-if="chargement" indeterminate color="primary" />
      <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="ma-2" :text="erreur" />

      <v-list
        v-if="!chargement && notifications.length"
        density="compact"
        class="py-0"
        style="max-height: 360px; overflow-y: auto"
      >
        <v-list-item v-for="n in notifications" :key="n.id" @click="marquerLu(n)">
          <template #prepend>
            <v-icon :icon="iconeTypeNotification(n.type)" :color="couleurTypeNotification(n.type)" size="20" />
          </template>
          <v-list-item-title class="text-wrap text-body-2" :class="{ 'font-weight-bold': !n.lue }">
            {{ libelleTypeNotification(n.type) }}
          </v-list-item-title>
          <v-list-item-subtitle class="text-wrap">{{ n.message }}</v-list-item-subtitle>
          <template #append>
            <span class="text-caption text-medium-emphasis">{{ formatDate(n.dateCreation) }}</span>
          </template>
        </v-list-item>
      </v-list>

      <v-card-text v-else-if="!chargement" class="text-center text-medium-emphasis">
        Aucune notification.
      </v-card-text>
    </v-card>
  </v-menu>
</template>
