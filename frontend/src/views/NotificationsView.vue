<script setup>
import { ref, computed, onMounted } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import { useAuthStore } from '@/stores/authStore'
import api, { messageErreur } from '@/services/api'
import { libelleTypeNotification, iconeTypeNotification, couleurTypeNotification, formatDate } from '@/services/notifications'

/**
 * Page recapitulative des notifications (Lot 7, etape 7.11).
 *
 * Ouverte a tout compte authentifie, quel que soit son role - meme regle que
 * NotificationController et la cloche (etape 7.10), dont cette vue reprend
 * les memes actions (marquer lu / tout marquer lu) pour l'historique complet,
 * la cloche ne montrant que les plus recentes.
 */

const auth = useAuthStore()

const notifications = ref([])
const chargement = ref(true)
const erreur = ref(null)
const nonLuesSeulement = ref(false)

const notificationsAffichees = computed(() =>
  nonLuesSeulement.value ? notifications.value.filter((n) => !n.lue) : notifications.value,
)
const aDesNonLues = computed(() => notifications.value.some((n) => !n.lue))

async function charger() {
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

onMounted(charger)

async function marquerLu(notification) {
  if (notification.lue) return
  try {
    await api.patch(`/notifications/${notification.id}/lu`)
    notification.lue = true
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

async function toutMarquerLu() {
  try {
    await api.patch('/notifications/lu')
    notifications.value.forEach((n) => (n.lue = true))
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}
</script>

<template>
  <LayoutDashboard titre="Mes notifications" icone="mdi-bell-outline" couleur="primary">
    <div class="d-flex flex-wrap align-center justify-space-between mb-4 ga-2">
      <v-btn variant="text" :to="auth.accueilSelonRole()" prepend-icon="mdi-arrow-left">Retour</v-btn>
      <div class="d-flex align-center ga-4">
        <v-switch
          v-model="nonLuesSeulement"
          label="Non lues seulement"
          density="compact"
          color="primary"
          hide-details
        />
        <v-btn v-if="aDesNonLues" variant="tonal" color="primary" @click="toutMarquerLu">Tout marquer lu</v-btn>
      </div>
    </div>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />

    <v-progress-linear v-if="chargement" indeterminate color="primary" class="mb-4" />

    <v-alert v-if="!chargement && notificationsAffichees.length === 0" type="info" variant="tonal">
      {{ nonLuesSeulement ? 'Aucune notification non lue.' : 'Aucune notification.' }}
    </v-alert>

    <v-list v-if="!chargement && notificationsAffichees.length" lines="two" class="py-0">
      <v-list-item
        v-for="n in notificationsAffichees"
        :key="n.id"
        :variant="n.lue ? 'plain' : 'tonal'"
        rounded="lg"
        class="mb-2"
        @click="marquerLu(n)"
      >
        <template #prepend>
          <v-icon :icon="iconeTypeNotification(n.type)" :color="couleurTypeNotification(n.type)" />
        </template>
        <v-list-item-title class="text-wrap" :class="{ 'font-weight-bold': !n.lue }">
          {{ libelleTypeNotification(n.type) }}
        </v-list-item-title>
        <v-list-item-subtitle class="text-wrap">{{ n.message }}</v-list-item-subtitle>
        <template #append>
          <span class="text-caption text-medium-emphasis">{{ formatDate(n.dateCreation) }}</span>
        </template>
      </v-list-item>
    </v-list>
  </LayoutDashboard>
</template>
