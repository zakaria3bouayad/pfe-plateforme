<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import api from '@/services/api'

const router = useRouter()
const auth = useAuthStore()

const formulaireValide = ref(false)
const afficherMdp = ref(false)
const filieres = ref([])
const promotions = ref([])

const f = ref({
  nom: '',
  prenom: '',
  email: '',
  motDePasse: '',
  telephone: '',
  role: 'ETUDIANT',
  numeroEtudiant: '',
  filiereId: null,
  promotionId: null,
  specialite: '',
  grade: '',
  departement: '',
})

const roles = [
  { titre: 'Étudiant', valeur: 'ETUDIANT' },
  { titre: 'Encadrant', valeur: 'ENCADRANT' },
]

const estEtudiant = computed(() => f.value.role === 'ETUDIANT')

const obligatoire = [(v) => !!v || 'Champ obligatoire']
const reglesEmail = [
  (v) => !!v || 'Email obligatoire',
  (v) => /.+@.+\..+/.test(v) || 'Format invalide',
]
const reglesMdp = [
  (v) => !!v || 'Mot de passe obligatoire',
  (v) => (v && v.length >= 8) || 'Au moins 8 caractères',
]

/**
 * Le referentiel n'est pas encore expose par une route publique.
 * En attendant le lot 2, on saisit les identifiants a la main si le
 * chargement echoue.
 */
onMounted(async () => {
  try {
    const [rf, rp] = await Promise.all([
      api.get('/filieres', { silencieux: true }),
      api.get('/promotions', { silencieux: true }),
    ])
    filieres.value = rf.data
    promotions.value = rp.data
  } catch {
    filieres.value = []
    promotions.value = []
  }
})

async function soumettre() {
  if (!formulaireValide.value) return

  const donnees = { ...f.value }
  if (estEtudiant.value) {
    donnees.specialite = null
    donnees.grade = null
    donnees.departement = null
  } else {
    donnees.numeroEtudiant = null
    donnees.filiereId = null
    donnees.promotionId = null
  }

  const ok = await auth.inscrire(donnees)
  if (ok) {
    router.push(auth.accueilSelonRole())
  }
}
</script>

<template>
  <v-container class="py-8">
    <v-row justify="center">
      <v-col cols="12" sm="10" md="8" lg="6">
        <v-card elevation="4" rounded="lg" class="pa-2">
          <v-card-item class="text-center pt-6">
            <v-icon icon="mdi-account-plus-outline" size="44" color="primary" />
            <v-card-title class="text-h5 mt-2">Créer un compte</v-card-title>
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
              <v-select
                v-model="f.role"
                :items="roles"
                item-title="titre"
                item-value="valeur"
                label="Je suis"
                variant="outlined"
                density="comfortable"
                prepend-inner-icon="mdi-account-cog-outline"
                class="mb-2"
              />

              <v-row dense>
                <v-col cols="12" sm="6">
                  <v-text-field
                    v-model="f.prenom"
                    label="Prénom"
                    variant="outlined"
                    density="comfortable"
                    :rules="obligatoire"
                  />
                </v-col>
                <v-col cols="12" sm="6">
                  <v-text-field
                    v-model="f.nom"
                    label="Nom"
                    variant="outlined"
                    density="comfortable"
                    :rules="obligatoire"
                  />
                </v-col>
              </v-row>

              <v-text-field
                v-model="f.email"
                label="Adresse e-mail"
                type="email"
                variant="outlined"
                density="comfortable"
                prepend-inner-icon="mdi-email-outline"
                :rules="reglesEmail"
              />

              <v-text-field
                v-model="f.motDePasse"
                label="Mot de passe"
                :type="afficherMdp ? 'text' : 'password'"
                variant="outlined"
                density="comfortable"
                prepend-inner-icon="mdi-lock-outline"
                :append-inner-icon="afficherMdp ? 'mdi-eye-off' : 'mdi-eye'"
                :rules="reglesMdp"
                hint="8 caractères minimum"
                @click:append-inner="afficherMdp = !afficherMdp"
              />

              <v-text-field
                v-model="f.telephone"
                label="Téléphone (facultatif)"
                variant="outlined"
                density="comfortable"
                prepend-inner-icon="mdi-phone-outline"
              />

              <!-- Champs specifiques ETUDIANT -->
              <template v-if="estEtudiant">
                <v-divider class="my-4" />
                <v-text-field
                  v-model="f.numeroEtudiant"
                  label="Numéro étudiant"
                  variant="outlined"
                  density="comfortable"
                  prepend-inner-icon="mdi-card-account-details-outline"
                  :rules="obligatoire"
                />
                <v-row dense>
                  <v-col cols="12" sm="6">
                    <v-select
                      v-if="filieres.length"
                      v-model="f.filiereId"
                      :items="filieres"
                      item-title="libelle"
                      item-value="id"
                      label="Filière"
                      variant="outlined"
                      density="comfortable"
                      :rules="obligatoire"
                    />
                    <v-text-field
                      v-else
                      v-model.number="f.filiereId"
                      label="Identifiant de filière"
                      type="number"
                      variant="outlined"
                      density="comfortable"
                      :rules="obligatoire"
                    />
                  </v-col>
                  <v-col cols="12" sm="6">
                    <v-select
                      v-if="promotions.length"
                      v-model="f.promotionId"
                      :items="promotions"
                      item-title="libelle"
                      item-value="id"
                      label="Promotion"
                      variant="outlined"
                      density="comfortable"
                      :rules="obligatoire"
                    />
                    <v-text-field
                      v-else
                      v-model.number="f.promotionId"
                      label="Identifiant de promotion"
                      type="number"
                      variant="outlined"
                      density="comfortable"
                      :rules="obligatoire"
                    />
                  </v-col>
                </v-row>
              </template>

              <!-- Champs specifiques ENCADRANT -->
              <template v-else>
                <v-divider class="my-4" />
                <v-text-field
                  v-model="f.specialite"
                  label="Spécialité"
                  variant="outlined"
                  density="comfortable"
                />
                <v-row dense>
                  <v-col cols="12" sm="6">
                    <v-text-field
                      v-model="f.grade"
                      label="Grade"
                      variant="outlined"
                      density="comfortable"
                    />
                  </v-col>
                  <v-col cols="12" sm="6">
                    <v-text-field
                      v-model="f.departement"
                      label="Département"
                      variant="outlined"
                      density="comfortable"
                    />
                  </v-col>
                </v-row>
              </template>

              <v-btn
                type="submit"
                color="primary"
                size="large"
                block
                class="mt-4"
                :loading="auth.chargement"
                :disabled="!formulaireValide"
              >
                Créer mon compte
              </v-btn>
            </v-form>
          </v-card-text>

          <v-divider />

          <v-card-actions class="justify-center py-3">
            <span class="text-body-2 text-medium-emphasis">Déjà inscrit ?</span>
            <v-btn variant="text" color="primary" to="/login">Se connecter</v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
