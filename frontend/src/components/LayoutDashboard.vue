<script setup>
import { useAuthStore } from '@/stores/authStore'
import { useRouter } from 'vue-router'

defineProps({
  titre: { type: String, required: true },
  icone: { type: String, default: 'mdi-view-dashboard-outline' },
  couleur: { type: String, default: 'primary' },
})

const auth = useAuthStore()
const router = useRouter()

function seDeconnecter() {
  auth.deconnecter()
  router.push('/login')
}
</script>

<template>
  <v-app-bar :color="couleur" flat>
    <v-app-bar-title>
      <v-icon :icon="icone" class="mr-2" />
      {{ titre }}
    </v-app-bar-title>

    <template #append>
      <span class="text-body-2 mr-4 d-none d-sm-inline">{{ auth.nomComplet }}</span>
      <v-btn icon="mdi-logout" variant="text" title="Se déconnecter" @click="seDeconnecter" />
    </template>
  </v-app-bar>

  <v-main>
    <v-container>
      <slot />
    </v-container>
  </v-main>
</template>
