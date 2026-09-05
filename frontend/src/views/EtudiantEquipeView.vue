<script setup>
import { ref, onMounted, computed } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'
import { useAuthStore } from '@/stores/authStore'

/**
 * Adhesion sur code d'invitation (hors plan, remplace en cours de lot 8
 * l'ancienne liste ouverte de toutes les equipes rejoignables de la
 * filiere/promotion, retiree a la demande de Zakaria) : le chef partage le
 * code affiche sur la fiche de son equipe, l'etudiant le saisit ici pour
 * rejoindre directement, sans parcourir de liste.
 */

const auth = useAuthStore()

const equipe = ref(null)
const chargement = ref(true)
const erreur = ref(null)
const succes = ref(null)

const dialogueCreation = ref(false)
const formulaireValide = ref(false)
const f = ref({ nom: '', tailleMax: 3 })

const numeroAjout = ref('')
const codeRejoindre = ref('')
const rejoindreEnCours = ref(false)
const codeCopie = ref(false)

const obligatoire = [(v) => !!v || 'Champ obligatoire']

const estChef = computed(() => equipe.value && equipe.value.chefId === auth.utilisateur?.id)

async function charger() {
  chargement.value = true
  erreur.value = null
  try {
    const { data } = await api.get('/equipes/moi')
    equipe.value = data
  } catch (e) {
    if (e.response?.status === 404) {
      equipe.value = null
    } else {
      erreur.value = messageErreur(e)
    }
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

async function rejoindre() {
  const code = codeRejoindre.value.trim()
  if (!code) return

  rejoindreEnCours.value = true
  erreur.value = null
  succes.value = null
  try {
    const { data } = await api.post('/equipes/rejoindre', { code })
    succes.value = `Vous avez rejoint « ${data.nom} ».`
    codeRejoindre.value = ''
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    rejoindreEnCours.value = false
  }
}

async function copierCode() {
  if (!equipe.value?.codeInvitation) return
  try {
    await navigator.clipboard.writeText(equipe.value.codeInvitation)
    codeCopie.value = true
    setTimeout(() => (codeCopie.value = false), 2000)
  } catch {
    // Copie manuelle possible via le champ affiche : pas bloquant si l'API clipboard est indisponible.
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
        Vous n'appartenez à aucune équipe pour l'instant. Créez-en une, ou rejoignez celle d'un
        camarade grâce au code que son chef vous a communiqué.
      </v-alert>

      <v-card variant="outlined" rounded="lg">
        <v-card-text class="d-flex align-end ga-2">
          <v-text-field
            v-model="codeRejoindre"
            label="Code d'équipe"
            variant="outlined"
            density="comfortable"
            hide-details
            @keydown.enter="rejoindre"
          />
          <v-btn color="primary" :loading="rejoindreEnCours" :disabled="!codeRejoindre.trim()" @click="rejoindre">
            Rejoindre
          </v-btn>
        </v-card-text>
      </v-card>
    </template>

    <v-card v-if="equipe" variant="outlined" rounded="lg">
      <v-card-item>
        <v-card-title>{{ equipe.nom }}</v-card-title>
        <v-card-subtitle>Chef : {{ equipe.chefNom }} · Taille max : {{ equipe.tailleMax }}</v-card-subtitle>
      </v-card-item>
      <v-card-text>
        <v-alert v-if="estChef" type="info" variant="tonal" density="compact" class="mb-4">
          <div class="d-flex align-center justify-space-between flex-wrap ga-2">
            <span>
              Code à transmettre à vos camarades pour qu'ils rejoignent l'équipe :
              <strong class="text-h6 ml-1">{{ equipe.codeInvitation }}</strong>
            </span>
            <v-btn size="small" variant="tonal" :prepend-icon="codeCopie ? 'mdi-check' : 'mdi-content-copy'" @click="copierCode">
              {{ codeCopie ? 'Copié' : 'Copier' }}
            </v-btn>
          </div>
        </v-alert>

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
          <p class="text-subtitle-2 mb-2">Ajouter un membre par son numéro étudiant</p>
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
