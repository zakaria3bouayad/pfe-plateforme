<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'
import { telechargerDocument, formatTaille } from '@/services/documents'

const projets = ref([])
const projetId = ref(null)
const jalons = ref([])
const documents = ref([])
const chargement = ref(true)
const chargementProjet = ref(false)
const erreur = ref(null)

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

async function chargerProjet() {
  if (!projetId.value) {
    jalons.value = []
    documents.value = []
    return
  }
  chargementProjet.value = true
  erreur.value = null
  try {
    const [{ data: e }, { data: d }] = await Promise.all([
      api.get(`/projets/${projetId.value}/etapes`),
      api.get(`/projets/${projetId.value}/documents`),
    ])
    jalons.value = e
    documents.value = d
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargementProjet.value = false
  }
}

onMounted(async () => {
  await chargerProjets()
  await chargerProjet()
})

watch(projetId, chargerProjet)

// ------------------------------------------------------------ regroupement

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

// ------------------------------------------------------------ commentaires (document ou jalon)

const commentairesOuvert = ref(false)
const commentairesChargement = ref(false)
const commentairesCible = ref(null) // { type: 'document' | 'etape', id, titre }
const commentaires = ref([])
const nouveauCommentaire = ref('')
const envoiCommentaireEnCours = ref(false)

function urlCommentaires({ type, id }) {
  return type === 'document' ? `/documents/${id}/commentaires` : `/etapes/${id}/commentaires`
}

async function ouvrirCommentaires(type, item) {
  commentairesCible.value = { type, id: item.id, titre: item.nom ?? item.titre }
  commentaires.value = []
  nouveauCommentaire.value = ''
  commentairesOuvert.value = true
  commentairesChargement.value = true
  try {
    const { data } = await api.get(urlCommentaires(commentairesCible.value))
    commentaires.value = data
  } catch (e) {
    erreur.value = messageErreur(e)
    commentairesOuvert.value = false
  } finally {
    commentairesChargement.value = false
  }
}

async function publierCommentaire() {
  if (!nouveauCommentaire.value.trim() || !commentairesCible.value) return
  envoiCommentaireEnCours.value = true
  erreur.value = null
  try {
    const { data } = await api.post(urlCommentaires(commentairesCible.value), {
      contenu: nouveauCommentaire.value.trim(),
    })
    commentaires.value.push(data)
    nouveauCommentaire.value = ''
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    envoiCommentaireEnCours.value = false
  }
}

function formatDate(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('fr-FR')
}
</script>

<template>
  <LayoutDashboard titre="Documents" icone="mdi-file-document-outline" couleur="teal-darken-2">
    <v-btn variant="text" to="/encadrant" prepend-icon="mdi-arrow-left" class="mb-4">Retour</v-btn>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />

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

    <v-progress-linear v-if="chargementProjet" indeterminate color="teal-darken-2" class="mb-4" />

    <template v-if="!chargementProjet && projetId">
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
                <v-icon icon="mdi-file-outline" color="teal-darken-2" />
              </template>
              <v-list-item-subtitle>
                v{{ d.version }} · {{ formatTaille(d.taille) }} · {{ d.uploadeurNom }} — {{ formatDate(d.dateUpload) }}
              </v-list-item-subtitle>
              <template #append>
                <v-btn icon="mdi-comment-text-outline" variant="text" size="small" title="Commentaires" @click="ouvrirCommentaires('document', d)" />
                <v-btn icon="mdi-history" variant="text" size="small" title="Historique des versions" @click="ouvrirHistorique(d)" />
                <v-btn icon="mdi-download" variant="text" size="small" color="teal-darken-2" title="Télécharger" @click="telecharger(d)" />
              </template>
            </v-list-item>
          </v-list>
        </v-card-text>
      </v-card>

      <!-- Documents par jalon -->
      <v-card v-for="j in jalons" :key="j.id" variant="outlined" rounded="lg" class="mb-4">
        <v-card-item>
          <div class="d-flex align-center justify-space-between">
            <div>
              <v-card-title>{{ j.titre }}</v-card-title>
              <v-card-subtitle>Documents rattachés à ce checkpoint</v-card-subtitle>
            </div>
            <v-btn variant="text" size="small" prepend-icon="mdi-comment-text-outline" @click="ouvrirCommentaires('etape', j)">
              Commentaires du checkpoint
            </v-btn>
          </div>
        </v-card-item>
        <v-card-text>
          <p v-if="documentsDuJalon(j.id).length === 0" class="text-caption text-medium-emphasis">
            Aucun document déposé pour ce checkpoint.
          </p>
          <v-list v-else density="comfortable">
            <v-list-item v-for="d in documentsDuJalon(j.id)" :key="d.id" :title="d.nom">
              <template #prepend>
                <v-icon icon="mdi-file-outline" color="teal-darken-2" />
              </template>
              <v-list-item-subtitle>
                v{{ d.version }} · {{ formatTaille(d.taille) }} · {{ d.uploadeurNom }} — {{ formatDate(d.dateUpload) }}
              </v-list-item-subtitle>
              <template #append>
                <v-btn icon="mdi-comment-text-outline" variant="text" size="small" title="Commentaires" @click="ouvrirCommentaires('document', d)" />
                <v-btn icon="mdi-history" variant="text" size="small" title="Historique des versions" @click="ouvrirHistorique(d)" />
                <v-btn icon="mdi-download" variant="text" size="small" color="teal-darken-2" title="Télécharger" @click="telecharger(d)" />
              </template>
            </v-list-item>
          </v-list>
        </v-card-text>
      </v-card>
    </template>

    <!-- Dialogue historique -->
    <v-dialog v-model="historiqueOuvert" max-width="560">
      <v-card rounded="lg">
        <v-card-title>Historique — {{ historiqueTitre }}</v-card-title>
        <v-card-text>
          <v-progress-linear v-if="historiqueChargement" indeterminate color="teal-darken-2" class="mb-2" />
          <v-list v-else density="comfortable">
            <v-list-item v-for="v in historique" :key="v.id" :title="`Version ${v.version}`">
              <v-list-item-subtitle>
                {{ formatTaille(v.taille) }} · {{ v.uploadeurNom }} — {{ formatDate(v.dateUpload) }}
              </v-list-item-subtitle>
              <template #append>
                <v-btn icon="mdi-download" variant="text" size="small" color="teal-darken-2" title="Télécharger cette version" @click="telecharger(v)" />
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

    <!-- Dialogue commentaires -->
    <v-dialog v-model="commentairesOuvert" max-width="560">
      <v-card rounded="lg">
        <v-card-title>Commentaires — {{ commentairesCible?.titre }}</v-card-title>
        <v-card-text>
          <v-progress-linear v-if="commentairesChargement" indeterminate color="teal-darken-2" class="mb-2" />

          <p v-else-if="commentaires.length === 0" class="text-caption text-medium-emphasis mb-3">
            Aucun commentaire pour l'instant.
          </p>

          <v-list v-else density="comfortable" class="mb-3">
            <v-list-item v-for="c in commentaires" :key="c.id" :title="c.auteurNom">
              <v-list-item-subtitle class="text-wrap">{{ c.contenu }}</v-list-item-subtitle>
              <template #append>
                <span class="text-caption text-medium-emphasis">{{ formatDate(c.dateCreation) }}</span>
              </template>
            </v-list-item>
          </v-list>

          <v-textarea
            v-model="nouveauCommentaire"
            label="Ajouter un commentaire"
            variant="outlined"
            density="comfortable"
            rows="2"
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="commentairesOuvert = false">Fermer</v-btn>
          <v-btn
            color="teal-darken-2"
            variant="tonal"
            :loading="envoiCommentaireEnCours"
            :disabled="!nouveauCommentaire.trim()"
            @click="publierCommentaire"
          >
            Publier
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </LayoutDashboard>
</template>
