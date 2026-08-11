<script setup>
import { ref, computed, onMounted } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'
import { telechargerDocument, premierFichier, formatTaille } from '@/services/documents'
import { useAuthStore } from '@/stores/authStore'

const auth = useAuthStore()

const projet = ref(null)
const equipe = ref(null)
const jalons = ref([])
const documents = ref([])
const chargement = ref(true)
const erreur = ref(null)
const succes = ref(null)

const estChef = computed(() => equipe.value && equipe.value.chefId === auth.utilisateur?.id)

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

// ------------------------------------------------------------ regroupement

/** Ne garde que la derniere version de chaque document (cle = jalon + nom). */
const documentsActuels = computed(() => {
  const parCle = new Map()
  for (const d of documents.value) {
    const cle = `${d.etapeId ?? 'general'}::${d.nom}`
    const existant = parCle.get(cle)
    if (!existant || d.version > existant.version) {
      parCle.set(cle, d)
    }
  }
  return [...parCle.values()]
})

function parDate(a, b) {
  return new Date(b.dateUpload) - new Date(a.dateUpload)
}

const documentsGeneraux = computed(() =>
  documentsActuels.value.filter((d) => !d.etapeId).sort(parDate),
)

function documentsDuJalon(jalonId) {
  return documentsActuels.value.filter((d) => d.etapeId === jalonId).sort(parDate)
}

// ------------------------------------------------------------ upload

const dialogueOuvert = ref(false)
const envoiEnCours = ref(false)
const formulaire = ref({ etapeId: null, fichier: null })

const optionsJalons = computed(() => [
  { id: null, titre: 'Aucun (document général)' },
  ...jalons.value.map((j) => ({ id: j.id, titre: j.titre })),
])

function ouvrirDialogue() {
  formulaire.value = { etapeId: null, fichier: null }
  erreur.value = null
  dialogueOuvert.value = true
}

async function envoyer() {
  const fichier = premierFichier(formulaire.value.fichier)
  if (!fichier || !projet.value) return

  envoiEnCours.value = true
  erreur.value = null
  try {
    const donnees = new FormData()
    donnees.append('fichier', fichier)
    const params = formulaire.value.etapeId ? { etapeId: formulaire.value.etapeId } : {}

    await api.post(`/projets/${projet.value.id}/documents`, donnees, {
      params,
      headers: { 'Content-Type': 'multipart/form-data' },
    })

    succes.value = 'Document déposé.'
    dialogueOuvert.value = false
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    envoiEnCours.value = false
  }
}

// ------------------------------------------------------------ telechargement

async function telecharger(doc) {
  erreur.value = null
  try {
    await telechargerDocument(doc.id, doc.nom)
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

// ------------------------------------------------------------ historique des versions

const historiqueOuvert = ref(false)
const historiqueChargement = ref(false)
const historique = ref([])
const historiqueTitre = ref('')

async function ouvrirHistorique(doc) {
  historiqueTitre.value = doc.nom
  historique.value = []
  historiqueOuvert.value = true
  historiqueChargement.value = true
  try {
    const { data } = await api.get(`/documents/${doc.id}/versions`)
    historique.value = data
  } catch (e) {
    erreur.value = messageErreur(e)
    historiqueOuvert.value = false
  } finally {
    historiqueChargement.value = false
  }
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('fr-FR')
}
</script>

<template>
  <LayoutDashboard titre="Documents" icone="mdi-file-document-outline" couleur="primary">
    <v-btn variant="text" to="/etudiant" prepend-icon="mdi-arrow-left" class="mb-4">Retour</v-btn>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />
    <v-alert v-if="succes" type="success" variant="tonal" density="compact" class="mb-4" :text="succes" />

    <v-progress-linear v-if="chargement" indeterminate color="primary" class="mb-4" />

    <v-alert v-if="!chargement && !projet" type="info" variant="tonal">
      Aucun projet pour l'instant. Les documents apparaîtront ici une fois votre équipe affectée à un sujet.
    </v-alert>

    <template v-if="!chargement && projet">
      <div class="d-flex align-center justify-space-between mb-4">
        <span v-if="!estChef" class="text-body-2 text-medium-emphasis">
          Seul le chef d'équipe peut déposer un document. Vous pouvez consulter et télécharger ci-dessous.
        </span>
        <v-spacer v-if="!estChef" />
        <v-btn v-if="estChef" color="primary" variant="tonal" prepend-icon="mdi-upload" @click="ouvrirDialogue">
          Déposer un document
        </v-btn>
      </div>

      <!-- Documents generaux -->
      <v-card variant="outlined" rounded="lg" class="mb-4">
        <v-card-item>
          <v-card-title>Documents généraux</v-card-title>
          <v-card-subtitle>Documents non rattachés à un checkpoint précis</v-card-subtitle>
        </v-card-item>
        <v-card-text>
          <p v-if="documentsGeneraux.length === 0" class="text-caption text-medium-emphasis">
            Aucun document déposé.
          </p>
          <v-list v-else density="comfortable">
            <v-list-item v-for="d in documentsGeneraux" :key="d.id" :title="d.nom">
              <template #prepend>
                <v-icon icon="mdi-file-outline" color="primary" />
              </template>
              <v-list-item-subtitle>
                v{{ d.version }} · {{ formatTaille(d.taille) }} · {{ d.uploadeurNom }} — {{ formatDate(d.dateUpload) }}
              </v-list-item-subtitle>
              <template #append>
                <v-btn icon="mdi-history" variant="text" size="small" title="Historique des versions" @click="ouvrirHistorique(d)" />
                <v-btn icon="mdi-download" variant="text" size="small" color="primary" title="Télécharger" @click="telecharger(d)" />
              </template>
            </v-list-item>
          </v-list>
        </v-card-text>
      </v-card>

      <!-- Documents par jalon -->
      <v-card v-for="j in jalons" :key="j.id" variant="outlined" rounded="lg" class="mb-4">
        <v-card-item>
          <v-card-title>{{ j.titre }}</v-card-title>
          <v-card-subtitle>Documents rattachés à ce checkpoint</v-card-subtitle>
        </v-card-item>
        <v-card-text>
          <p v-if="documentsDuJalon(j.id).length === 0" class="text-caption text-medium-emphasis">
            Aucun document déposé pour ce checkpoint.
          </p>
          <v-list v-else density="comfortable">
            <v-list-item v-for="d in documentsDuJalon(j.id)" :key="d.id" :title="d.nom">
              <template #prepend>
                <v-icon icon="mdi-file-outline" color="primary" />
              </template>
              <v-list-item-subtitle>
                v{{ d.version }} · {{ formatTaille(d.taille) }} · {{ d.uploadeurNom }} — {{ formatDate(d.dateUpload) }}
              </v-list-item-subtitle>
              <template #append>
                <v-btn icon="mdi-history" variant="text" size="small" title="Historique des versions" @click="ouvrirHistorique(d)" />
                <v-btn icon="mdi-download" variant="text" size="small" color="primary" title="Télécharger" @click="telecharger(d)" />
              </template>
            </v-list-item>
          </v-list>
        </v-card-text>
      </v-card>
    </template>

    <!-- Dialogue upload -->
    <v-dialog v-model="dialogueOuvert" max-width="520">
      <v-card rounded="lg">
        <v-card-title>Déposer un document</v-card-title>
        <v-card-text>
          <v-select
            v-model="formulaire.etapeId"
            :items="optionsJalons"
            item-title="titre"
            item-value="id"
            label="Checkpoint associé"
          />
          <v-file-input v-model="formulaire.fichier" label="Fichier" show-size prepend-icon="mdi-paperclip" />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="dialogueOuvert = false">Annuler</v-btn>
          <v-btn
            color="primary"
            variant="tonal"
            :loading="envoiEnCours"
            :disabled="!premierFichier(formulaire.fichier)"
            @click="envoyer"
          >
            Envoyer
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Dialogue historique -->
    <v-dialog v-model="historiqueOuvert" max-width="560">
      <v-card rounded="lg">
        <v-card-title>Historique — {{ historiqueTitre }}</v-card-title>
        <v-card-text>
          <v-progress-linear v-if="historiqueChargement" indeterminate color="primary" class="mb-2" />
          <v-list v-else density="comfortable">
            <v-list-item v-for="v in historique" :key="v.id" :title="`Version ${v.version}`">
              <v-list-item-subtitle>
                {{ formatTaille(v.taille) }} · {{ v.uploadeurNom }} — {{ formatDate(v.dateUpload) }}
              </v-list-item-subtitle>
              <template #append>
                <v-btn icon="mdi-download" variant="text" size="small" color="primary" title="Télécharger cette version" @click="telecharger(v)" />
              </template>
            </v-list-item>
          </v-list>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="historiqueOuvert = false">Fermer</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </LayoutDashboard>
</template>
