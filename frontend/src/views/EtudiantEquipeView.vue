<script setup>
import { ref, onMounted, computed } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'

const auth = useAuthStore()

const equipe = ref(null)
const equipesDisponibles = ref([])
const chargement = ref(true)
const erreur = ref(null)
const succes = ref(null)
const rejoindreEnCours = ref(null)

const dialogueCreation = ref(false)
const formulaireValide = ref(false)
const f = ref({ nom: '', tailleMax: 3 })

const numeroAjout = ref('')

const obligatoire = [(v) => !!v || 'Champ obligatoire']

const estChef = computed(() => equipe.value && equipe.value.chefId === auth.utilisateur?.id)

/** Equipes pas encore pleines : proposees pour l'auto-inscription (hors plan initial). */
const equipesRejoignables = computed(() =>
  equipesDisponibles.value.filter((e) => e.membres.length < e.tailleMax),
)

async function charger() {
  chargement.value = true
  erreur.value = null
  try {
    const { data } = await api.get('/equipes/moi')
    equipe.value = data
  } catch (e) {
    if (e.response?.status === 404) {
      equipe.value = null
      await chargerEquipesDisponibles()
    } else {
      erreur.value = messageErreur(e)
    }
  } finally {
    chargement.value = false
  }
}

async function chargerEquipesDisponibles() {
  try {
    const { data } = await api.get('/equipes')
    equipesDisponibles.value = data
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

onMounted(charger)

async function rejoindre(equipeAJoindre) {
  if (!confirm(`Rejoindre l'équipe « ${equipeAJoindre.nom} » ?`)) return
  rejoindreEnCours.value = equipeAJoindre.id
  erreur.value = null
  succes.value = null
  try {
    await api.post(`/equipes/${equipeAJoindre.id}/rejoindre`)
    succes.value = `Vous avez rejoint « ${equipeAJoindre.nom} ».`
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    rejoindreEnCours.value = null
  }
}

async function creer() {
  if (!formulaireValide.value) return
  erreur.value = null
  try {
    await api.post('/equipes', f.value)
    dialogueCreation.value = false
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

async function ajouterMembre() {
  if (!numeroAjout.value) return
  erreur.value = null
  try {
    await api.post(`/equipes/${equipe.value.id}/membres`, { numeroEtudiant: numeroAjout.value })
    numeroAjout.value = ''
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

async function retirer(etudiantId) {
  if (!confirm('Retirer ce membre de l\'équipe ?')) return
  erreur.value = null
  try {
    await api.delete(`/equipes/${equipe.value.id}/membres/${etudiantId}`)
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

async function quitter() {
  if (!confirm('Quitter cette équipe ?')) return
  erreur.value = null
  try {
    await api.delete(`/equipes/${equipe.value.id}/membres/moi`)
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

async function dissoudre() {
  if (!confirm('Dissoudre définitivement cette équipe ?')) return
  erreur.value = null
  try {
    await api.delete(`/equipes/${equipe.value.id}`)
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}
</script>

<template>
  <LayoutDashboard titre="Mon équipe" icone="mdi-account-group-outline" couleur="primary">
    <div class="d-flex justify-space-between align-center mb-4">
      <v-btn variant="text" to="/etudiant" prepend-icon="mdi-arrow-left">Retour</v-btn>
      <v-btn v-if="!equipe && !chargement" color="primary" prepend-icon="mdi-plus" @click="dialogueCreation = true">
        Créer une équipe
      </v-btn>
    </div>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />
    <v-alert v-if="succes" type="success" variant="tonal" density="compact" class="mb-4" :text="succes" />

    <v-progress-linear v-if="chargement" indeterminate color="primary" class="mb-4" />

    <template v-if="!chargement && !equipe">
      <v-alert type="info" variant="tonal" class="mb-4">
        Vous n'appartenez à aucune équipe pour l'instant. Créez-en une, ou rejoignez directement une
        équipe existante ci-dessous (de votre filière et promotion, sous réserve de place disponible).
      </v-alert>

      <v-alert v-if="equipesRejoignables.length === 0" type="info" variant="tonal" density="compact">
        Aucune équipe avec de la place disponible pour l'instant.
      </v-alert>

      <v-list v-else density="comfortable" lines="two" class="mb-4">
        <v-list-item
          v-for="e in equipesRejoignables"
          :key="e.id"
          :title="e.nom"
          :subtitle="`Chef : ${e.chefNom} · ${e.membres.length}/${e.tailleMax} membres`"
        >
          <template #prepend>
            <v-icon icon="mdi-account-group-outline" />
          </template>
          <template #append>
            <v-btn
              size="small"
              color="primary"
              variant="tonal"
              :loading="rejoindreEnCours === e.id"
              @click="rejoindre(e)"
            >
              Rejoindre
            </v-btn>
          </template>
        </v-list-item>
      </v-list>
    </template>

    <v-card v-if="equipe" variant="outlined" rounded="lg">
      <v-card-item>
        <v-card-title>{{ equipe.nom }}</v-card-title>
        <v-card-subtitle>Chef : {{ equipe.chefNom }} · Taille max : {{ equipe.tailleMax }}</v-card-subtitle>
      </v-card-item>
      <v-card-text>
        <v-list density="comfortable">
          <v-list-item v-for="m in equipe.membres" :key="m.id" :title="m.nomComplet" :subtitle="m.numeroEtudiant">
            <template #prepend>
              <v-icon icon="mdi-account-circle-outline" />
            </template>
            <template v-if="estChef && m.id !== equipe.chefId" #append>
              <v-btn icon="mdi-close" size="small" variant="text" color="error" @click="retirer(m.id)" />
            </template>
          </v-list-item>
        </v-list>

        <template v-if="estChef">
          <v-divider class="my-4" />
          <p class="text-subtitle-2 mb-2">Ajouter un membre</p>
          <div class="d-flex ga-2">
            <v-text-field
              v-model="numeroAjout"
              label="Numéro étudiant"
              variant="outlined"
              density="comfortable"
              hide-details
            />
            <v-btn color="primary" @click="ajouterMembre">Ajouter</v-btn>
          </div>
        </template>
      </v-card-text>
      <v-card-actions>
        <v-btn v-if="!estChef" variant="text" color="error" @click="quitter">Quitter l'équipe</v-btn>
        <v-btn v-else variant="text" color="error" @click="dissoudre">Dissoudre l'équipe</v-btn>
      </v-card-actions>
    </v-card>

    <v-dialog v-model="dialogueCreation" max-width="500">
      <v-card rounded="lg">
        <v-card-title class="pt-4">Créer une équipe</v-card-title>
        <v-card-text>
          <v-form v-model="formulaireValide" @submit.prevent="creer">
            <v-text-field
              v-model="f.nom"
              label="Nom de l'équipe"
              variant="outlined"
              density="comfortable"
              :rules="obligatoire"
              class="mb-2"
            />
            <v-text-field
              v-model.number="f.tailleMax"
              type="number"
              min="1"
              max="4"
              label="Taille maximale"
              variant="outlined"
              density="comfortable"
              :rules="obligatoire"
            />
            <v-card-actions class="px-0">
              <v-spacer />
              <v-btn variant="text" @click="dialogueCreation = false">Annuler</v-btn>
              <v-btn color="primary" type="submit" :disabled="!formulaireValide">Créer</v-btn>
            </v-card-actions>
          </v-form>
        </v-card-text>
      </v-card>
    </v-dialog>
  </LayoutDashboard>
</template>
