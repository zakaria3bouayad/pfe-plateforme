<script setup>
import { ref, computed, onMounted } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'
import { telechargerDocument, premierFichier } from '@/services/documents'
import { useAuthStore } from '@/stores/authStore'
import { LIBELLES_STATUT_ETAPE, COULEURS_STATUT_ETAPE } from '@/utils/statuts'

const auth = useAuthStore()

const projet = ref(null)
const equipe = ref(null)
const jalons = ref([])
const documents = ref([])
const chargement = ref(true)
const erreur = ref(null)
const succes = ref(null)

const estChef = computed(() => equipe.value && equipe.value.chefId === auth.utilisateur?.id)

// Un jalon peut etre soumis tant qu'il n'a pas deja ete rendu (EF-26).
const STATUTS_SOUMETTABLES = ['A_FAIRE', 'EN_COURS', 'EN_RETARD']

async function chargerEquipe() {
  try {
    const { data } = await api.get('/equipes/moi')
    equipe.value = data
  } catch (e) {
    if (e.response?.status === 404) {
      equipe.value = null
    } else {
      throw e
    }
  }
}

async function charger() {
  chargement.value = true
  erreur.value = null
  try {
    await chargerEquipe()

    const { data: p } = await api.get('/projets/moi')
    projet.value = p

    const [{ data: e }, { data: d }] = await Promise.all([
      api.get(`/projets/${p.id}/etapes`),
      api.get(`/projets/${p.id}/documents`),
    ])
    jalons.value = e
    documents.value = d
  } catch (e) {
    if (e.response?.status === 404) {
      projet.value = null
      jalons.value = []
      documents.value = []
    } else {
      erreur.value = messageErreur(e)
    }
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

/** Dernier livrable depose pour ce jalon (Lot 4 : remplace le lien texte du lot 3). */
function livrableDuJalon(jalonId) {
  return documents.value
    .filter((d) => d.etapeId === jalonId)
    .sort((a, b) => b.version - a.version)[0] ?? null
}

async function telecharger(doc) {
  erreur.value = null
  try {
    await telechargerDocument(doc.id, doc.nom)
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

// ------------------------------------------------------------ soumission

const dialogueOuvert = ref(false)
const jalonCible = ref(null)
const envoiEnCours = ref(false)
const formulaire = ref({ fichier: null, commentaire: '' })

function ouvrirDialogue(jalon) {
  jalonCible.value = jalon
  formulaire.value = { fichier: null, commentaire: '' }
  erreur.value = null
  dialogueOuvert.value = true
}

async function soumettre() {
  const fichier = premierFichier(formulaire.value.fichier)
  if (!jalonCible.value || !fichier) return

  envoiEnCours.value = true
  erreur.value = null
  try {
    const donnees = new FormData()
    donnees.append('fichier', fichier)

    const { data: doc } = await api.post(`/projets/${projet.value.id}/documents`, donnees, {
      params: { etapeId: jalonCible.value.id },
      headers: { 'Content-Type': 'multipart/form-data' },
    })

    await api.patch(`/etapes/${jalonCible.value.id}/soumettre`, {
      lienLivrable: `Document déposé : ${doc.nom} (v${doc.version})`,
      commentaire: formulaire.value.commentaire.trim() || null,
    })
    succes.value = `Livrable soumis pour "${jalonCible.value.titre}".`
    dialogueOuvert.value = false
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    envoiEnCours.value = false
  }
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('fr-FR')
}
</script>

<template>
  <LayoutDashboard titre="Checkpoints du projet" icone="mdi-flag-checkered" couleur="primary">
    <v-btn variant="text" to="/etudiant" prepend-icon="mdi-arrow-left" class="mb-4">Retour</v-btn>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />
    <v-alert v-if="succes" type="success" variant="tonal" density="compact" class="mb-4" :text="succes" />

    <v-progress-linear v-if="chargement" indeterminate color="primary" class="mb-4" />

    <v-alert v-if="!chargement && !projet" type="info" variant="tonal">
      Aucun projet pour l'instant. Les checkpoints apparaîtront ici une fois votre équipe affectée à un sujet.
    </v-alert>

    <v-alert v-else-if="!chargement && !estChef" type="info" variant="tonal" class="mb-4">
      Seul le chef d'équipe peut soumettre un livrable. Vous pouvez consulter l'avancement ci-dessous.
    </v-alert>

    <template v-if="!chargement && projet">
      <v-alert v-if="jalons.length === 0" type="info" variant="tonal">
        Votre encadrant n'a pas encore créé de checkpoint pour ce projet.
      </v-alert>

      <v-card v-for="j in jalons" :key="j.id" variant="outlined" rounded="lg" class="mb-4">
        <v-card-item>
          <v-card-title class="d-flex align-center">
            {{ j.titre }}
            <v-chip :color="COULEURS_STATUT_ETAPE[j.statut]" size="small" class="ml-3">
              {{ LIBELLES_STATUT_ETAPE[j.statut] }}
            </v-chip>
          </v-card-title>
          <v-card-subtitle>Échéance : {{ formatDate(j.dateEcheance) }}</v-card-subtitle>
        </v-card-item>

        <v-card-text>
          <p class="mb-2">{{ j.description }}</p>

          <div v-if="livrableDuJalon(j.id)" class="mb-2 d-flex align-center">
            <strong class="mr-2">Livrable soumis :</strong>
            <v-btn
              variant="text"
              size="small"
              color="primary"
              prepend-icon="mdi-download"
              @click="telecharger(livrableDuJalon(j.id))"
            >
              {{ livrableDuJalon(j.id).nom }} (v{{ livrableDuJalon(j.id).version }})
            </v-btn>
            <span class="text-caption text-medium-emphasis"> — le {{ formatDate(j.dateSoumission) }}</span>
          </div>
          <p v-if="j.commentaireSoumission" class="text-caption text-medium-emphasis mb-2">
            Votre commentaire : {{ j.commentaireSoumission }}
          </p>

          <v-alert v-if="j.statut === 'VALIDEE'" type="success" variant="tonal" density="compact" class="mt-2">
            Validé le {{ formatDate(j.dateValidation) }}
            <span v-if="j.commentaireValidation"> — {{ j.commentaireValidation }}</span>
          </v-alert>
          <v-alert v-else-if="j.statut === 'EN_RETARD'" type="error" variant="tonal" density="compact" class="mt-2">
            Échéance dépassée sans soumission.
          </v-alert>
        </v-card-text>

        <v-card-actions v-if="estChef && STATUTS_SOUMETTABLES.includes(j.statut)">
          <v-btn color="primary" variant="tonal" @click="ouvrirDialogue(j)">Soumettre un livrable</v-btn>
        </v-card-actions>
      </v-card>
    </template>

    <v-dialog v-model="dialogueOuvert" max-width="520">
      <v-card rounded="lg">
        <v-card-title>Soumettre : {{ jalonCible?.titre }}</v-card-title>
        <v-card-text>
          <v-file-input v-model="formulaire.fichier" label="Fichier du livrable" show-size prepend-icon="mdi-paperclip" />
          <v-textarea v-model="formulaire.commentaire" label="Commentaire (optionnel)" rows="3" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialogueOuvert = false">Annuler</v-btn>
          <v-btn
            color="primary"
            variant="tonal"
            :loading="envoiEnCours"
            :disabled="!premierFichier(formulaire.fichier)"
            @click="soumettre"
          >
            Envoyer
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </LayoutDashboard>
</template>
