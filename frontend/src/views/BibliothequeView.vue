<script setup>
import { ref, computed, onMounted } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'
import { telechargerRessource } from '@/services/ressources'
import { premierFichier, formatTaille } from '@/services/documents'
import { useAuthStore } from '@/stores/authStore'

const auth = useAuthStore()

const ressources = ref([])
const chargement = ref(true)
const erreur = ref(null)
const succes = ref(null)
const filtreCategorie = ref(null)

const peutCreer = computed(() => auth.estEncadrant || auth.estAdmin)

const categories = computed(() => {
  const vues = new Set(ressources.value.map((r) => r.categorie))
  return [...vues].sort()
})

const ressourcesFiltrees = computed(() => {
  if (!filtreCategorie.value) return ressources.value
  return ressources.value.filter((r) => r.categorie === filtreCategorie.value)
})

function peutGerer(r) {
  return auth.estAdmin || r.auteurId === auth.utilisateur?.id
}

async function charger() {
  chargement.value = true
  erreur.value = null
  try {
    const { data } = await api.get('/ressources')
    ressources.value = data
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

// ------------------------------------------------------------ creation / edition

const dialogueOuvert = ref(false)
const envoiEnCours = ref(false)
const modeEdition = ref(false)
const ressourceEnEdition = ref(null)
const formulaire = ref({ titre: '', description: '', categorie: '', lien: '', fichier: null })

function ouvrirCreation() {
  modeEdition.value = false
  ressourceEnEdition.value = null
  formulaire.value = { titre: '', description: '', categorie: '', lien: '', fichier: null }
  erreur.value = null
  dialogueOuvert.value = true
}

function ouvrirEdition(r) {
  modeEdition.value = true
  ressourceEnEdition.value = r
  formulaire.value = {
    titre: r.titre,
    description: r.description || '',
    categorie: r.categorie,
    lien: r.lien || '',
    fichier: null,
  }
  erreur.value = null
  dialogueOuvert.value = true
}

const formulaireValide = computed(() => {
  const f = formulaire.value
  const aUnFichierExistant = modeEdition.value && ressourceEnEdition.value?.fichierNom
  return (
    f.titre.trim() &&
    f.categorie.trim() &&
    (f.lien.trim() || premierFichier(f.fichier) || aUnFichierExistant)
  )
})

async function enregistrer() {
  if (!formulaireValide.value) return

  envoiEnCours.value = true
  erreur.value = null
  try {
    const donnees = new FormData()
    donnees.append('titre', formulaire.value.titre.trim())
    if (formulaire.value.description.trim()) {
      donnees.append('description', formulaire.value.description.trim())
    }
    donnees.append('categorie', formulaire.value.categorie.trim())
    if (formulaire.value.lien.trim()) {
      donnees.append('lien', formulaire.value.lien.trim())
    }
    const fichier = premierFichier(formulaire.value.fichier)
    if (fichier) {
      donnees.append('fichier', fichier)
    }

    if (modeEdition.value) {
      await api.put(`/ressources/${ressourceEnEdition.value.id}`, donnees, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      succes.value = 'Ressource modifiée.'
    } else {
      await api.post('/ressources', donnees, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      succes.value = 'Ressource publiée.'
    }

    dialogueOuvert.value = false
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    envoiEnCours.value = false
  }
}

// ------------------------------------------------------------ suppression

async function supprimer(r) {
  if (!confirm(`Confirmer la suppression de « ${r.titre} » ?`)) return

  erreur.value = null
  succes.value = null
  try {
    await api.delete(`/ressources/${r.id}`)
    succes.value = 'Ressource supprimée.'
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

// ------------------------------------------------------------ telechargement

async function telecharger(r) {
  erreur.value = null
  try {
    await telechargerRessource(r.id, r.fichierNom)
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('fr-FR')
}
</script>

<template>
  <LayoutDashboard titre="Bibliothèque" icone="mdi-library-outline" couleur="indigo-darken-2">
    <div class="d-flex justify-space-between align-center mb-4 flex-wrap ga-2">
      <v-btn variant="text" :to="auth.accueilSelonRole()" prepend-icon="mdi-arrow-left">Retour</v-btn>
      <v-btn v-if="peutCreer" color="indigo-darken-2" variant="tonal" prepend-icon="mdi-plus" @click="ouvrirCreation">
        Nouvelle ressource
      </v-btn>
    </div>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />
    <v-alert v-if="succes" type="success" variant="tonal" density="compact" class="mb-4" :text="succes" />

    <v-select
      v-if="categories.length > 0"
      v-model="filtreCategorie"
      :items="categories"
      label="Filtrer par catégorie"
      variant="outlined"
      density="compact"
      clearable
      class="mb-4"
      style="max-width: 320px"
    />

    <v-progress-linear v-if="chargement" indeterminate color="indigo-darken-2" class="mb-4" />

    <v-alert v-if="!chargement && ressourcesFiltrees.length === 0" type="info" variant="tonal">
      Aucune ressource pour l'instant.
    </v-alert>

    <v-row v-else>
      <v-col v-for="r in ressourcesFiltrees" :key="r.id" cols="12" sm="6" md="4">
        <v-card variant="outlined" rounded="lg" class="h-100 d-flex flex-column">
          <v-card-item>
            <v-chip size="small" color="indigo-darken-2" variant="tonal" class="mb-2">{{ r.categorie }}</v-chip>
            <v-card-title class="text-subtitle-1">{{ r.titre }}</v-card-title>
            <v-card-subtitle>{{ r.auteurNom }} — {{ formatDate(r.dateCreation) }}</v-card-subtitle>
          </v-card-item>
          <v-card-text class="flex-grow-1">
            <p v-if="r.description" class="text-body-2 text-wrap">{{ r.description }}</p>
          </v-card-text>
          <v-card-actions>
            <v-btn v-if="r.lien" variant="text" size="small" prepend-icon="mdi-open-in-new" :href="r.lien" target="_blank">
              Ouvrir le lien
            </v-btn>
            <v-btn
              v-if="r.fichierNom"
              variant="text"
              size="small"
              prepend-icon="mdi-download"
              @click="telecharger(r)"
            >
              {{ formatTaille(r.fichierTaille) }}
            </v-btn>
            <v-spacer />
            <template v-if="peutGerer(r)">
              <v-btn icon="mdi-pencil" variant="text" size="small" title="Modifier" @click="ouvrirEdition(r)" />
              <v-btn icon="mdi-delete-outline" variant="text" size="small" color="error" title="Supprimer" @click="supprimer(r)" />
            </template>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>

    <!-- Dialogue creation / edition -->
    <v-dialog v-model="dialogueOuvert" max-width="560">
      <v-card rounded="lg">
        <v-card-title>{{ modeEdition ? 'Modifier la ressource' : 'Nouvelle ressource' }}</v-card-title>
        <v-card-text>
          <v-text-field v-model="formulaire.titre" label="Titre" variant="outlined" density="comfortable" class="mb-2" />
          <v-textarea
            v-model="formulaire.description"
            label="Description (facultatif)"
            variant="outlined"
            density="comfortable"
            rows="2"
            class="mb-2"
          />
          <v-text-field
            v-model="formulaire.categorie"
            label="Catégorie"
            variant="outlined"
            density="comfortable"
            class="mb-2"
          />
          <v-text-field
            v-model="formulaire.lien"
            label="Lien (URL, facultatif si un fichier est fourni)"
            variant="outlined"
            density="comfortable"
            class="mb-2"
          />
          <v-file-input
            v-model="formulaire.fichier"
            :label="modeEdition && ressourceEnEdition?.fichierNom ? `Remplacer le fichier (actuel : ${ressourceEnEdition.fichierNom})` : 'Fichier (facultatif si un lien est fourni)'"
            variant="outlined"
            density="comfortable"
            show-size
            prepend-icon="mdi-paperclip"
          />
          <p v-if="!formulaireValide" class="text-caption text-medium-emphasis">
            Titre, catégorie et au moins un lien ou un fichier sont obligatoires.
          </p>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialogueOuvert = false">Annuler</v-btn>
          <v-btn
            color="indigo-darken-2"
            variant="tonal"
            :loading="envoiEnCours"
            :disabled="!formulaireValide"
            @click="enregistrer"
          >
            {{ modeEdition ? 'Enregistrer' : 'Publier' }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </LayoutDashboard>
</template>
