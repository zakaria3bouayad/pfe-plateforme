<script setup>
import { ref, onMounted, watch } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'
import { LIBELLES_STATUT_ETAPE, COULEURS_STATUT_ETAPE } from '@/utils/statuts'

const projets = ref([])
const projetId = ref(null)
const jalons = ref([])
const chargement = ref(true)
const chargementJalons = ref(false)
const erreur = ref(null)
const succes = ref(null)

const obligatoire = [(v) => !!v || 'Champ obligatoire']

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

async function chargerJalons() {
  if (!projetId.value) {
    jalons.value = []
    return
  }
  chargementJalons.value = true
  erreur.value = null
  try {
    const { data } = await api.get(`/projets/${projetId.value}/etapes`)
    jalons.value = data
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargementJalons.value = false
  }
}

onMounted(async () => {
  await chargerProjets()
  await chargerJalons()
})

watch(projetId, chargerJalons)

function modifiable(jalon) {
  return jalon.statut !== 'SOUMISE' && jalon.statut !== 'VALIDEE'
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('fr-FR')
}

// ------------------------------------------------------------ creation / edition

const dialogueOuvert = ref(false)
const modeEdition = ref(false)
const jalonEnEdition = ref(null)
const formulaireValide = ref(false)
const f = ref({ titre: '', description: '', dateEcheance: '', ordre: 1 })

function ouvrirCreation() {
  modeEdition.value = false
  jalonEnEdition.value = null
  f.value = { titre: '', description: '', dateEcheance: '', ordre: jalons.value.length + 1 }
  dialogueOuvert.value = true
}

function ouvrirEdition(jalon) {
  modeEdition.value = true
  jalonEnEdition.value = jalon
  f.value = {
    titre: jalon.titre,
    description: jalon.description,
    dateEcheance: jalon.dateEcheance,
    ordre: jalon.ordre,
  }
  dialogueOuvert.value = true
}

async function soumettreFormulaire() {
  if (!formulaireValide.value) return
  erreur.value = null
  try {
    if (modeEdition.value) {
      await api.put(`/etapes/${jalonEnEdition.value.id}`, f.value)
    } else {
      await api.post(`/projets/${projetId.value}/etapes`, f.value)
    }
    dialogueOuvert.value = false
    succes.value = modeEdition.value ? 'Checkpoint modifié.' : 'Checkpoint créé.'
    await chargerJalons()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

async function supprimer(jalon) {
  if (!confirm(`Supprimer le checkpoint "${jalon.titre}" ?`)) return
  erreur.value = null
  try {
    await api.delete(`/etapes/${jalon.id}`)
    await chargerJalons()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

// ------------------------------------------------------------ validation

const dialogueValidation = ref(false)
const jalonAValider = ref(null)
const commentaireValidation = ref('')
const validationEnCours = ref(false)

function ouvrirValidation(jalon) {
  jalonAValider.value = jalon
  commentaireValidation.value = ''
  dialogueValidation.value = true
}

async function valider() {
  if (!jalonAValider.value) return
  validationEnCours.value = true
  erreur.value = null
  try {
    await api.patch(`/etapes/${jalonAValider.value.id}/valider`, {
      commentaire: commentaireValidation.value.trim() || null,
    })
    succes.value = `Checkpoint "${jalonAValider.value.titre}" validé.`
    dialogueValidation.value = false
    await chargerJalons()
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    validationEnCours.value = false
  }
}
</script>

<template>
  <LayoutDashboard titre="Checkpoints" icone="mdi-flag-checkered" couleur="teal-darken-2">
    <div class="d-flex justify-space-between align-center mb-4">
      <v-btn variant="text" to="/encadrant" prepend-icon="mdi-arrow-left">Retour</v-btn>
      <v-btn
        v-if="projetId"
        color="teal-darken-2"
        prepend-icon="mdi-plus"
        @click="ouvrirCreation"
      >
        Créer un checkpoint
      </v-btn>
    </div>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />
    <v-alert v-if="succes" type="success" variant="tonal" density="compact" class="mb-4" :text="succes" />

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

    <v-progress-linear v-if="chargementJalons" indeterminate color="teal-darken-2" class="mb-4" />

    <v-alert v-if="!chargementJalons && projetId && jalons.length === 0" type="info" variant="tonal">
      Aucun checkpoint créé pour ce projet.
    </v-alert>

    <v-card v-for="j in jalons" :key="j.id" variant="outlined" rounded="lg" class="mb-4">
      <v-card-item>
        <v-card-title class="d-flex align-center">
          {{ j.titre }}
          <v-chip :color="COULEURS_STATUT_ETAPE[j.statut]" size="small" class="ml-3">
            {{ LIBELLES_STATUT_ETAPE[j.statut] }}
          </v-chip>
        </v-card-title>
        <v-card-subtitle>Échéance : {{ formatDate(j.dateEcheance) }} · ordre {{ j.ordre }}</v-card-subtitle>
      </v-card-item>

      <v-card-text>
        <p class="mb-2">{{ j.description }}</p>

        <div v-if="j.lienLivrable" class="mb-2">
          <strong>Livrable soumis :</strong>
          <a :href="j.lienLivrable" target="_blank" rel="noopener">{{ j.lienLivrable }}</a>
          <span class="text-caption text-medium-emphasis"> — le {{ formatDate(j.dateSoumission) }}</span>
          <p v-if="j.commentaireSoumission" class="text-caption text-medium-emphasis mt-1">
            Commentaire de l'équipe : {{ j.commentaireSoumission }}
          </p>
        </div>

        <v-alert v-if="j.statut === 'VALIDEE'" type="success" variant="tonal" density="compact" class="mt-2">
          Validé le {{ formatDate(j.dateValidation) }}
          <span v-if="j.commentaireValidation"> — {{ j.commentaireValidation }}</span>
        </v-alert>
      </v-card-text>

      <v-card-actions>
        <v-btn v-if="j.statut === 'SOUMISE'" color="success" variant="tonal" @click="ouvrirValidation(j)">
          Valider
        </v-btn>
        <template v-if="modifiable(j)">
          <v-btn variant="text" size="small" prepend-icon="mdi-pencil-outline" @click="ouvrirEdition(j)">
            Modifier
          </v-btn>
          <v-btn variant="text" size="small" color="error" prepend-icon="mdi-delete-outline" @click="supprimer(j)">
            Supprimer
          </v-btn>
        </template>
      </v-card-actions>
    </v-card>

    <!-- Creation / edition -->
    <v-dialog v-model="dialogueOuvert" max-width="560">
      <v-card rounded="lg">
        <v-card-title class="pt-4">{{ modeEdition ? 'Modifier le checkpoint' : 'Créer un checkpoint' }}</v-card-title>
        <v-card-text>
          <v-form v-model="formulaireValide" @submit.prevent="soumettreFormulaire">
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
              rows="3"
              class="mb-2"
            />
            <v-row dense>
              <v-col cols="7">
                <v-text-field
                  v-model="f.dateEcheance"
                  type="date"
                  label="Date d'échéance"
                  variant="outlined"
                  density="comfortable"
                  :rules="obligatoire"
                />
              </v-col>
              <v-col cols="5">
                <v-text-field
                  v-model.number="f.ordre"
                  type="number"
                  min="1"
                  label="Ordre"
                  variant="outlined"
                  density="comfortable"
                  :rules="obligatoire"
                />
              </v-col>
            </v-row>
            <v-card-actions class="px-0">
              <v-spacer />
              <v-btn variant="text" @click="dialogueOuvert = false">Annuler</v-btn>
              <v-btn color="teal-darken-2" type="submit" :disabled="!formulaireValide">
                {{ modeEdition ? 'Enregistrer' : 'Créer' }}
              </v-btn>
            </v-card-actions>
          </v-form>
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- Validation -->
    <v-dialog v-model="dialogueValidation" max-width="480">
      <v-card rounded="lg">
        <v-card-title class="pt-4">Valider : {{ jalonAValider?.titre }}</v-card-title>
        <v-card-text>
          <v-textarea
            v-model="commentaireValidation"
            label="Commentaire (optionnel)"
            variant="outlined"
            density="comfortable"
            rows="3"
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialogueValidation = false">Annuler</v-btn>
          <v-btn color="success" variant="tonal" :loading="validationEnCours" @click="valider">Valider</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </LayoutDashboard>
</template>
