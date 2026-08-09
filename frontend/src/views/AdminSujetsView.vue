<script setup>
import { ref, onMounted } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'
import { LIBELLES_STATUT_SUJET, COULEURS_STATUT_SUJET } from '@/utils/statuts'

const sujets = ref([])
const filtre = ref(null)
const chargement = ref(true)
const erreur = ref(null)

const filtres = [
  { titre: 'Tous', valeur: null },
  { titre: 'Proposés', valeur: 'PROPOSE' },
  { titre: 'En validation', valeur: 'EN_VALIDATION' },
  { titre: 'À corriger', valeur: 'A_CORRIGER' },
  { titre: 'Validés', valeur: 'VALIDE' },
  { titre: 'Rejetés', valeur: 'REJETE' },
  { titre: 'Affectés', valeur: 'AFFECTE' },
]

const dialogueDecision = ref(false)
const sujetEnDecision = ref(null)
const typeDecision = ref('')
const commentaire = ref('')

async function charger() {
  chargement.value = true
  erreur.value = null
  try {
    const params = filtre.value ? { statut: filtre.value } : {}
    const { data } = await api.get('/sujets', { params })
    sujets.value = data
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

async function demarrerValidation(s) {
  erreur.value = null
  try {
    await api.patch(`/sujets/${s.id}/demarrer-validation`)
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

async function valider(s) {
  erreur.value = null
  try {
    await api.patch(`/sujets/${s.id}/valider`)
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

function ouvrirDecision(s, type) {
  sujetEnDecision.value = s
  typeDecision.value = type
  commentaire.value = ''
  dialogueDecision.value = true
}

async function confirmerDecision() {
  if (!commentaire.value.trim()) return
  erreur.value = null
  try {
    const route = typeDecision.value === 'rejeter' ? 'rejeter' : 'demander-correction'
    await api.patch(`/sujets/${sujetEnDecision.value.id}/${route}`, { commentaire: commentaire.value })
    dialogueDecision.value = false
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}
</script>

<template>
  <LayoutDashboard titre="Validation des sujets" icone="mdi-check-circle-outline" couleur="deep-purple-darken-2">
    <div class="d-flex justify-space-between align-center mb-4 flex-wrap ga-2">
      <v-btn variant="text" to="/admin" prepend-icon="mdi-arrow-left">Retour</v-btn>
      <v-select
        v-model="filtre"
        :items="filtres"
        item-title="titre"
        item-value="valeur"
        label="Filtrer par statut"
        variant="outlined"
        density="compact"
        hide-details
        style="max-width: 240px"
        @update:model-value="charger"
      />
    </div>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />

    <v-progress-linear v-if="chargement" indeterminate color="deep-purple-darken-2" class="mb-4" />

    <v-alert v-if="!chargement && sujets.length === 0" type="info" variant="tonal">
      Aucun sujet pour ce filtre.
    </v-alert>

    <v-row>
      <v-col v-for="s in sujets" :key="s.id" cols="12" md="6">
        <v-card variant="outlined" rounded="lg">
          <v-card-item>
            <template #append>
              <v-chip :color="COULEURS_STATUT_SUJET[s.statut]" size="small" variant="flat">
                {{ LIBELLES_STATUT_SUJET[s.statut] }}
              </v-chip>
            </template>
            <v-card-title>{{ s.titre }}</v-card-title>
            <v-card-subtitle>{{ s.encadrantNom }}</v-card-subtitle>
          </v-card-item>
          <v-card-text>
            <p class="mb-2">{{ s.description }}</p>
            <p class="text-caption text-medium-emphasis">Capacité : {{ s.capaciteMax }} étudiant(s)</p>
            <v-alert v-if="s.commentaireValidation" type="warning" variant="tonal" density="compact" class="mt-2">
              {{ s.commentaireValidation }}
            </v-alert>
          </v-card-text>
          <v-card-actions v-if="s.statut === 'PROPOSE'">
            <v-btn variant="tonal" color="deep-purple-darken-2" @click="demarrerValidation(s)">
              Démarrer l'examen
            </v-btn>
          </v-card-actions>
          <v-card-actions v-else-if="s.statut === 'EN_VALIDATION'" class="flex-wrap ga-1">
            <v-btn size="small" variant="tonal" color="success" @click="valider(s)">Valider</v-btn>
            <v-btn size="small" variant="tonal" color="warning" @click="ouvrirDecision(s, 'corriger')">
              Demander correction
            </v-btn>
            <v-btn size="small" variant="tonal" color="error" @click="ouvrirDecision(s, 'rejeter')">
              Rejeter
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>

    <v-dialog v-model="dialogueDecision" max-width="500">
      <v-card rounded="lg">
        <v-card-title class="pt-4">
          {{ typeDecision === 'rejeter' ? 'Rejeter le sujet' : 'Demander une correction' }}
        </v-card-title>
        <v-card-text>
          <v-textarea
            v-model="commentaire"
            label="Commentaire (obligatoire)"
            variant="outlined"
            rows="3"
            :rules="[(v) => !!v.trim() || 'Le commentaire est obligatoire']"
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialogueDecision = false">Annuler</v-btn>
          <v-btn color="deep-purple-darken-2" :disabled="!commentaire.trim()" @click="confirmerDecision">
            Confirmer
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </LayoutDashboard>
</template>
