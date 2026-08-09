<script setup>
import { ref, onMounted } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'
import { LIBELLES_STATUT_SUJET, COULEURS_STATUT_SUJET } from '@/utils/statuts'

const sujets = ref([])
const filieres = ref([])
const chargement = ref(true)
const erreur = ref(null)

const dialogueOuvert = ref(false)
const modeEdition = ref(false)
const sujetEnEdition = ref(null)
const formulaireValide = ref(false)

const f = ref({ titre: '', description: '', motsCles: '', capaciteMax: 1, filiereId: null })

const obligatoire = [(v) => !!v || 'Champ obligatoire']

async function charger() {
  chargement.value = true
  erreur.value = null
  try {
    const [rs, rf] = await Promise.all([api.get('/sujets/moi'), api.get('/filieres')])
    sujets.value = rs.data
    filieres.value = rf.data
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

function ouvrirCreation() {
  modeEdition.value = false
  sujetEnEdition.value = null
  f.value = { titre: '', description: '', motsCles: '', capaciteMax: 1, filiereId: null }
  dialogueOuvert.value = true
}

function ouvrirEdition(sujet) {
  modeEdition.value = true
  sujetEnEdition.value = sujet
  f.value = {
    titre: sujet.titre,
    description: sujet.description,
    motsCles: sujet.motsCles,
    capaciteMax: sujet.capaciteMax,
    filiereId: sujet.filiereId,
  }
  dialogueOuvert.value = true
}

async function soumettre() {
  if (!formulaireValide.value) return
  erreur.value = null
  try {
    if (modeEdition.value) {
      await api.put(`/sujets/${sujetEnEdition.value.id}`, f.value)
    } else {
      await api.post('/sujets', f.value)
    }
    dialogueOuvert.value = false
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

async function supprimer(sujet) {
  if (!confirm(`Supprimer le sujet "${sujet.titre}" ?`)) return
  erreur.value = null
  try {
    await api.delete(`/sujets/${sujet.id}`)
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

function modifiable(sujet) {
  return sujet.statut === 'PROPOSE' || sujet.statut === 'A_CORRIGER'
}
</script>

<template>
  <LayoutDashboard titre="Mes sujets proposés" icone="mdi-lightbulb-on-outline" couleur="teal-darken-2">
    <div class="d-flex justify-space-between align-center mb-4">
      <v-btn variant="text" to="/encadrant" prepend-icon="mdi-arrow-left">Retour</v-btn>
      <v-btn color="teal-darken-2" prepend-icon="mdi-plus" @click="ouvrirCreation">Proposer un sujet</v-btn>
    </div>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />

    <v-progress-linear v-if="chargement" indeterminate color="teal-darken-2" class="mb-4" />

    <v-alert v-if="!chargement && sujets.length === 0" type="info" variant="tonal">
      Aucun sujet proposé pour l'instant.
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
            <v-card-subtitle v-if="s.filiereLibelle">{{ s.filiereLibelle }}</v-card-subtitle>
          </v-card-item>
          <v-card-text>
            <p class="mb-2">{{ s.description }}</p>
            <p v-if="s.motsCles" class="text-caption text-medium-emphasis mb-1">Mots-clés : {{ s.motsCles }}</p>
            <p class="text-caption text-medium-emphasis mb-1">Capacité : {{ s.capaciteMax }} étudiant(s)</p>
            <v-alert v-if="s.commentaireValidation" type="warning" variant="tonal" density="compact" class="mt-2">
              {{ s.commentaireValidation }}
            </v-alert>
          </v-card-text>
          <v-card-actions v-if="modifiable(s)">
            <v-btn variant="text" size="small" prepend-icon="mdi-pencil-outline" @click="ouvrirEdition(s)">
              Modifier
            </v-btn>
            <v-btn variant="text" size="small" color="error" prepend-icon="mdi-delete-outline" @click="supprimer(s)">
              Supprimer
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>

    <v-dialog v-model="dialogueOuvert" max-width="600">
      <v-card rounded="lg">
        <v-card-title class="pt-4">{{ modeEdition ? 'Modifier le sujet' : 'Proposer un sujet' }}</v-card-title>
        <v-card-text>
          <v-form v-model="formulaireValide" @submit.prevent="soumettre">
            <v-text-field
              v-model="f.titre"
              label="Titre"
              variant="outlined"
              density="comfortable"
              :rules="obligatoire"
              class="mb-2"
            />
            <v-textarea
              v-model="f.description"
              label="Description"
              variant="outlined"
              density="comfortable"
              :rules="obligatoire"
              rows="4"
              class="mb-2"
            />
            <v-text-field
              v-model="f.motsCles"
              label="Mots-clés (facultatif)"
              variant="outlined"
              density="comfortable"
              class="mb-2"
            />
            <v-row dense>
              <v-col cols="6">
                <v-text-field
                  v-model.number="f.capaciteMax"
                  type="number"
                  min="1"
                  max="10"
                  label="Capacité (étudiants)"
                  variant="outlined"
                  density="comfortable"
                  :rules="obligatoire"
                />
              </v-col>
              <v-col cols="6">
                <v-select
                  v-model="f.filiereId"
                  :items="filieres"
                  item-title="libelle"
                  item-value="id"
                  label="Filière (facultatif)"
                  variant="outlined"
                  density="comfortable"
                  clearable
                />
              </v-col>
            </v-row>
            <v-card-actions class="px-0">
              <v-spacer />
              <v-btn variant="text" @click="dialogueOuvert = false">Annuler</v-btn>
              <v-btn color="teal-darken-2" type="submit" :disabled="!formulaireValide">
                {{ modeEdition ? 'Enregistrer' : 'Proposer' }}
              </v-btn>
            </v-card-actions>
          </v-form>
        </v-card-text>
      </v-card>
    </v-dialog>
  </LayoutDashboard>
</template>
