<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const router = useRouter()
const auth = useAuthStore()

const email = ref('')
const motDePasse = ref('')
const afficherMdp = ref(false)
const formulaireValide = ref(false)

const reglesEmail = [
  (v) => !!v || 'Email obligatoire',
  (v) => /.+@.+\..+/.test(v) || 'Format invalide',
]
const reglesMdp = [(v) => !!v || 'Mot de passe obligatoire']

async function soumettre() {
  if (!formulaireValide.value) return
  const ok = await auth.connecter(email.value, motDePasse.value)
  if (ok) {
    router.push(auth.accueilSelonRole())
  }
}
</script>

<template>
  <v-container class="fill-height" fluid>
    <v-row justify="center" align="center">
      <v-col cols="12" sm="8" md="5" lg="4">
        <v-card elevation="4" rounded="lg" class="pa-2">
          <v-card-item class="text-center pt-6">
            <v-icon icon="mdi-school-outline" size="48" color="primary" />
            <v-card-title class="text-h5 mt-2">Plateforme PFE</v-card-title>
            <v-card-subtitle>Connexion à votre espace</v-card-subtitle>
          </v-card-item>

          <v-card-text>
            <v-alert
              v-if="auth.erreur"
              type="error"
              variant="tonal"
              density="compact"
              class="mb-4"
              :text="auth.erreur"
            />

            <v-form v-model="formulaireValide" @submit.prevent="soumettre">
              <v-text-field
                v-model="email"
                label="Adresse e-mail"
                type="email"
                variant="outlined"
                density="comfortable"
                prepend-inner-icon="mdi-email-outline"
                autocomplete="email"
                :rules="reglesEmail"
                class="mb-2"
              />

              <v-text-field
                v-model="motDePasse"
                label="Mot de passe"
                :type="afficherMdp ? 'text' : 'password'"
                variant="outlined"
                density="comfortable"
                prepend-inner-icon="mdi-lock-outline"
                :append-inner-icon="afficherMdp ? 'mdi-eye-off' : 'mdi-eye'"
                autocomplete="current-password"
                :rules="reglesMdp"
                @click:append-inner="afficherMdp = !afficherMdp"
              />

              <v-btn
                type="submit"
                color="primary"
                size="large"
                block
                class="mt-4"
                :loading="auth.chargement"
                :disabled="!formulaireValide"
              >
                Se connecter
              </v-btn>
            </v-form>
          </v-card-text>

          <v-divider />

          <v-card-actions class="justify-center py-3">
            <span class="text-body-2 text-medium-emphasis">Pas encore de compte ?</span>
            <v-btn variant="text" color="primary" to="/register">Créer un compte</v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
