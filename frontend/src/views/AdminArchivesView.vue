<script setup>
/**
 * Vue "Corpus de reference" cote administrateur (Lot 6, etape 6.8).
 *
 * Deux responsabilites :
 *  1. constituer le corpus - marquer ou demarquer un document comme rapport
 *     de reference archive
 *  2. en surveiller la sante - un document archive dont l'indexation a echoue
 *     ne participe a aucune comparaison, et rien ne le signalerait autrement
 *
 * Le second point justifie a lui seul l'existence de cette vue. Sans lui, un
 * corpus pourrait paraitre fourni tout en etant a moitie inexploitable, et
 * les analyses rendraient des resultats faussement rassurants.
 */
import { ref, computed, onMounted, watch } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'
import { telechargerDocument, formatTaille } from '@/services/documents'
import {
  libelleStatutIndexation,
  couleurStatutIndexation,
  formatDate,
} from '@/services/similarite'

const COULEUR = 'deep-purple-darken-2'

/**
 * Le marquage declenche extraction puis vectorisation : meme raison qu'a
 * l'etape 6.7 de depasser le timeout global de 15 secondes d'Axios.
 */
const DELAI_INDEXATION_MS = 300000

const archives = ref([])
const indexations = ref(new Map())
const projets = ref([])
const projetId = ref(null)
const documents = ref([])

const chargement = ref(true)
const chargementProjet = ref(false)
const actionEnCours = ref(null) // id du document en cours de traitement
const erreur = ref(null)
const succes = ref(null)

// ------------------------------------------------------------ chargement

async function chargerArchives() {
  const { data } = await api.get('/admin/documents/archives')
  archives.value = data

  // L'etat d'indexation se recupere archive par archive : il n'existe pas
  // d'endpoint de lot, et le corpus reste de taille modeste.
  const etats = await Promise.all(
    data.map(async (a) => {
      try {
        const { data: i } = await api.get(`/admin/documents/${a.id}/indexation`)
        return [a.id, i]
      } catch {
        // 404 attendu si le document n'a jamais ete indexe : ce n'est pas
        // une erreur a remonter, c'est un etat a afficher.
        return [a.id, null]
      }
    }),
  )
  indexations.value = new Map(etats)
}

async function chargerTout() {
  chargement.value = true
  erreur.value = null
  try {
    const [{ data: p }] = await Promise.all([api.get('/projets'), chargerArchives()])
    projets.value = p
    if (p.length > 0 && !projetId.value) {
      projetId.value = p[0].id
    }
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargement.value = false
  }
}

async function chargerProjet() {
  if (!projetId.value) {
    documents.value = []
    return
  }
  chargementProjet.value = true
  erreur.value = null
  try {
    const { data } = await api.get(`/projets/${projetId.value}/documents`)
    documents.value = data
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargementProjet.value = false
  }
}

onMounted(async () => {
  await chargerTout()
  await chargerProjet()
})

watch(projetId, chargerProjet)

// ------------------------------------------------------------ etat du corpus

/** Ne garde que la version courante de chaque document. */
const documentsCourants = computed(() => {
  const parCle = new Map()
  for (const d of documents.value) {
    const cle = `${d.etapeId ?? 'general'}::${d.nom}`
    const existant = parCle.get(cle)
    if (!existant || d.version > existant.version) {
      parCle.set(cle, d)
    }
  }
  return [...parCle.values()].sort((a, b) => new Date(b.dateUpload) - new Date(a.dateUpload))
})

function indexationDe(documentId) {
  return indexations.value.get(documentId) ?? null
}

/** Une archive n'entre dans les comparaisons que si son statut est VECTORISE. */
function estExploitable(documentId) {
  return indexationDe(documentId)?.statut === 'VECTORISE'
}

const nbExploitables = computed(
  () => archives.value.filter((a) => estExploitable(a.id)).length,
)

const nbDefaillantes = computed(() => archives.value.length - nbExploitables.value)

// ------------------------------------------------------------ actions

async function changerArchive(doc, archive) {
  actionEnCours.value = doc.id
  erreur.value = null
  succes.value = null
  try {
    await api.patch(
      `/admin/documents/${doc.id}/archive`,
      { archive },
      { timeout: DELAI_INDEXATION_MS },
    )
    await Promise.all([chargerArchives(), chargerProjet()])
    succes.value = archive
      ? `« ${doc.nom} » ajouté au corpus de référence.`
      : `« ${doc.nom} » retiré du corpus.`
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    actionEnCours.value = null
  }
}

async function reindexer(documentId, nom) {
  actionEnCours.value = documentId
  erreur.value = null
  succes.value = null
  try {
    const { data } = await api.post(
      `/admin/documents/${documentId}/indexer`,
      null,
      { timeout: DELAI_INDEXATION_MS },
    )
    indexations.value = new Map(indexations.value).set(documentId, data)
    succes.value =
      data.statut === 'VECTORISE'
        ? `« ${nom} » indexé : ${data.nbMorceaux} passage(s) vectorisé(s).`
        : `« ${nom} » : ${libelleStatutIndexation(data.statut)}.`
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    actionEnCours.value = null
  }
}

async function telecharger(documentId, nom) {
  erreur.value = null
  try {
    await telechargerDocument(documentId, nom)
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

// ------------------------------------------------------------ apercu du texte extrait

const apercuOuvert = ref(false)
const apercu = ref(null)

function ouvrirApercu(documentId) {
  apercu.value = indexationDe(documentId)
  apercuOuvert.value = true
}
</script>

<template>
  <LayoutDashboard titre="Corpus de référence" icone="mdi-archive-outline" :couleur="COULEUR">
    <v-btn variant="text" to="/admin" prepend-icon="mdi-arrow-left" class="mb-4">Retour</v-btn>

    <v-alert type="info" variant="tonal" density="comfortable" class="mb-4" icon="mdi-information-outline">
      <div class="text-body-2">
        Seuls les documents marqués ici servent de référence à la détection de similarité.
        Les rapports simplement déposés sur un projet n'entrent pas dans les comparaisons.
        Marquer un document déclenche l'extraction de son texte puis le calcul de ses vecteurs.
      </div>
    </v-alert>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />
    <v-alert v-if="succes" type="success" variant="tonal" density="compact" class="mb-4" :text="succes" />

    <v-progress-linear v-if="chargement" indeterminate :color="COULEUR" class="mb-4" />

    <!-- ============================================ etat du corpus -->
    <v-row dense class="mb-2">
      <v-col cols="12" sm="4">
        <v-card variant="outlined" rounded="lg">
          <v-card-item>
            <div class="text-caption text-medium-emphasis">Documents archivés</div>
            <div class="text-h5">{{ archives.length }}</div>
          </v-card-item>
        </v-card>
      </v-col>
      <v-col cols="12" sm="4">
        <v-card variant="outlined" rounded="lg">
          <v-card-item>
            <div class="text-caption text-medium-emphasis">Exploitables</div>
            <div class="text-h5 text-success">{{ nbExploitables }}</div>
          </v-card-item>
        </v-card>
      </v-col>
      <v-col cols="12" sm="4">
        <v-card variant="outlined" rounded="lg">
          <v-card-item>
            <div class="text-caption text-medium-emphasis">À corriger</div>
            <div class="text-h5" :class="nbDefaillantes > 0 ? 'text-error' : ''">
              {{ nbDefaillantes }}
            </div>
          </v-card-item>
        </v-card>
      </v-col>
    </v-row>

    <!--
      Un corpus vide n'est pas neutre : toute analyse lancee dans cet etat
      rendra "rien a signaler", ce qu'un encadrant lira comme un feu vert.
    -->
    <v-alert
      v-if="!chargement && archives.length === 0"
      type="warning"
      variant="tonal"
      density="comfortable"
      class="mb-4"
    >
      Le corpus est vide. Tant qu'aucun rapport n'est archivé, les analyses de similarité
      ne peuvent rien détecter et rendront systématiquement « rien à signaler ».
    </v-alert>

    <v-alert
      v-else-if="nbDefaillantes > 0"
      type="warning"
      variant="tonal"
      density="comfortable"
      class="mb-4"
    >
      {{ nbDefaillantes }} document(s) archivé(s) ne sont pas exploitables et ne participent
      à aucune comparaison. Relancer leur indexation ci-dessous.
    </v-alert>

    <!-- ============================================ corpus actuel -->
    <v-card variant="outlined" rounded="lg" class="mb-6">
      <v-card-item>
        <v-card-title>Corpus actuel</v-card-title>
        <v-card-subtitle>Rapports servant de référence aux comparaisons</v-card-subtitle>
      </v-card-item>
      <v-card-text>
        <p v-if="archives.length === 0" class="text-caption text-medium-emphasis">
          Aucun document archivé.
        </p>
        <v-list v-else density="comfortable">
          <v-list-item v-for="a in archives" :key="a.id" :title="a.nom">
            <template #prepend>
              <v-icon icon="mdi-archive-outline" :color="COULEUR" />
            </template>

            <v-list-item-subtitle>
              <v-chip
                :color="couleurStatutIndexation(indexationDe(a.id)?.statut)"
                size="x-small"
                variant="tonal"
                label
                class="mr-2"
              >
                {{ libelleStatutIndexation(indexationDe(a.id)?.statut) }}
              </v-chip>
              <template v-if="indexationDe(a.id)?.statut === 'VECTORISE'">
                {{ indexationDe(a.id).nbCaracteres.toLocaleString('fr-FR') }} caractères ·
                {{ indexationDe(a.id).nbMorceaux }} passage(s)
              </template>
              <template v-else-if="indexationDe(a.id)?.message">
                {{ indexationDe(a.id).message }}
              </template>
              <span class="text-medium-emphasis">
                · archivé par {{ a.archiveParNom }} le {{ formatDate(a.dateArchivage) }}
              </span>
            </v-list-item-subtitle>

            <template #append>
              <v-btn
                v-if="indexationDe(a.id)"
                icon="mdi-text-search"
                variant="text"
                size="small"
                title="Aperçu du texte extrait"
                @click="ouvrirApercu(a.id)"
              />
              <v-btn
                icon="mdi-refresh"
                variant="text"
                size="small"
                title="Relancer l'indexation"
                :loading="actionEnCours === a.id"
                :disabled="actionEnCours !== null"
                @click="reindexer(a.id, a.nom)"
              />
              <v-btn
                icon="mdi-download"
                variant="text"
                size="small"
                title="Télécharger"
                @click="telecharger(a.id, a.nom)"
              />
              <v-btn
                variant="text"
                size="small"
                color="error"
                class="ml-1"
                :loading="actionEnCours === a.id"
                :disabled="actionEnCours !== null"
                @click="changerArchive(a, false)"
              >
                Retirer
              </v-btn>
            </template>
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>

    <!-- ============================================ ajout au corpus -->
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

    <v-progress-linear v-if="chargementProjet" indeterminate :color="COULEUR" class="mb-4" />

    <v-card v-if="!chargementProjet && projetId" variant="outlined" rounded="lg">
      <v-card-item>
        <v-card-title>Documents du projet</v-card-title>
        <v-card-subtitle>Sélectionner les rapports finaux à verser au corpus</v-card-subtitle>
      </v-card-item>
      <v-card-text>
        <p v-if="documentsCourants.length === 0" class="text-caption text-medium-emphasis">
          Aucun document déposé sur ce projet.
        </p>
        <v-list v-else density="comfortable">
          <v-list-item v-for="d in documentsCourants" :key="d.id" :title="d.nom">
            <template #prepend>
              <v-icon
                :icon="d.archive ? 'mdi-archive-check-outline' : 'mdi-file-outline'"
                :color="d.archive ? 'success' : COULEUR"
              />
            </template>
            <v-list-item-subtitle>
              v{{ d.version }} · {{ formatTaille(d.taille) }} · {{ d.uploadeurNom }} —
              {{ formatDate(d.dateUpload) }}
            </v-list-item-subtitle>
            <template #append>
              <v-btn
                icon="mdi-download"
                variant="text"
                size="small"
                title="Télécharger"
                @click="telecharger(d.id, d.nom)"
              />
              <v-btn
                :variant="d.archive ? 'text' : 'tonal'"
                size="small"
                :color="d.archive ? 'error' : COULEUR"
                class="ml-2"
                :loading="actionEnCours === d.id"
                :disabled="actionEnCours !== null"
                @click="changerArchive(d, !d.archive)"
              >
                {{ d.archive ? 'Retirer du corpus' : 'Archiver' }}
              </v-btn>
            </template>
          </v-list-item>
        </v-list>

        <v-alert
          v-if="actionEnCours !== null"
          type="info"
          variant="tonal"
          density="compact"
          class="mt-3"
        >
          Traitement en cours — extraction du texte puis calcul des vecteurs.
          Cela peut prendre une à deux minutes.
        </v-alert>
      </v-card-text>
    </v-card>

    <!-- ============================================ apercu du texte extrait -->
    <v-dialog v-model="apercuOuvert" max-width="720" scrollable>
      <v-card rounded="lg">
        <v-card-title>Texte extrait — {{ apercu?.documentNom }}</v-card-title>
        <v-card-text>
          <template v-if="apercu">
            <v-chip
              :color="couleurStatutIndexation(apercu.statut)"
              size="small"
              variant="tonal"
              label
              class="mb-3"
            >
              {{ libelleStatutIndexation(apercu.statut) }}
            </v-chip>

            <v-row dense class="mb-3">
              <v-col cols="6" sm="3">
                <div class="text-caption text-medium-emphasis">Caractères</div>
                <div class="text-body-2">{{ apercu.nbCaracteres.toLocaleString('fr-FR') }}</div>
              </v-col>
              <v-col cols="6" sm="3">
                <div class="text-caption text-medium-emphasis">Pages</div>
                <div class="text-body-2">{{ apercu.nbPages ?? '—' }}</div>
              </v-col>
              <v-col cols="6" sm="3">
                <div class="text-caption text-medium-emphasis">Passages</div>
                <div class="text-body-2">{{ apercu.nbMorceaux }}</div>
              </v-col>
              <v-col cols="6" sm="3">
                <div class="text-caption text-medium-emphasis">Indexé le</div>
                <div class="text-body-2">{{ formatDate(apercu.dateExtraction) }}</div>
              </v-col>
            </v-row>

            <v-alert v-if="apercu.tronque" type="warning" variant="tonal" density="compact" class="mb-3">
              Le texte a été tronqué à la limite configurée : la fin du document n'est pas indexée.
            </v-alert>

            <v-alert v-if="apercu.message" type="warning" variant="tonal" density="compact" class="mb-3">
              {{ apercu.message }}
            </v-alert>

            <!--
              L'apercu sert de controle visuel : si le texte ressort illisible
              ou entrelace, l'extraction a echoue silencieusement et les
              vecteurs calcules dessus n'ont aucune valeur.
            -->
            <div v-if="apercu.apercu" class="text-caption text-medium-emphasis mb-1">
              Début du texte tel qu'il a été indexé
            </div>
            <div v-if="apercu.apercu" class="extrait">{{ apercu.apercu }}</div>
          </template>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="apercuOuvert = false">Fermer</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </LayoutDashboard>
</template>

<style scoped>
.extrait {
  font-size: 0.8125rem;
  line-height: 1.6;
  white-space: pre-wrap;
  background: rgb(var(--v-theme-surface-light, 245, 245, 245));
  border-left: 3px solid rgb(var(--v-theme-primary));
  border-radius: 4px;
  padding: 10px 12px;
  max-height: 300px;
  overflow-y: auto;
}
</style>
