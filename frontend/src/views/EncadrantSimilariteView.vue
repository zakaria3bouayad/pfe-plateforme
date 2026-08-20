<script setup>
/**
 * Vue "Rapports de similarite" cote encadrant (Lot 6, etape 6.7).
 *
 * Trois zones :
 *  1. les cas a examiner, tous projets confondus, tries par score decroissant
 *  2. les documents d'un projet, avec pour chacun son dernier rapport
 *  3. le detail d'un rapport, passages en regard
 *
 * Parti pris d'affichage : le score n'est jamais montre seul. Il est toujours
 * accompagne d'un libelle prudent et de l'avertissement renvoye par l'API.
 * Un pourcentage isole se lit comme un verdict, ce qu'il n'est pas.
 */
import { ref, computed, onMounted, watch } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'
import { telechargerDocument } from '@/services/documents'
import {
  couleurNiveau,
  iconeNiveau,
  libelleNiveau,
  explicationNiveau,
  formatDate,
} from '@/services/similarite'

const projets = ref([])
const projetId = ref(null)
const documents = ref([])
const rapportsProjet = ref([])
const aExaminer = ref([])

const chargement = ref(true)
const chargementProjet = ref(false)
const erreur = ref(null)
const info = ref(null)

// ------------------------------------------------------------ chargement

async function chargerProjets() {
  chargement.value = true
  erreur.value = null
  try {
    const [{ data: p }, { data: a }] = await Promise.all([
      api.get('/projets/mes-projets'),
      api.get('/similarites/a-examiner'),
    ])
    projets.value = p
    aExaminer.value = a
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
    rapportsProjet.value = []
    return
  }
  chargementProjet.value = true
  erreur.value = null
  try {
    const [{ data: d }, { data: r }] = await Promise.all([
      api.get(`/projets/${projetId.value}/documents`),
      api.get(`/projets/${projetId.value}/similarites`),
    ])
    documents.value = d
    rapportsProjet.value = r
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

// ------------------------------------------------------------ croisement documents / rapports

/**
 * Dernier rapport par document. L'API renvoie les rapports du plus recent au
 * plus ancien : le premier rencontre pour un document est donc le bon.
 */
const dernierRapportParDocument = computed(() => {
  const parDocument = new Map()
  for (const r of rapportsProjet.value) {
    if (!parDocument.has(r.documentId)) {
      parDocument.set(r.documentId, r)
    }
  }
  return parDocument
})

/** Ne garde que la version courante de chaque document, comme la vue Documents. */
const documentsAnalysables = computed(() => {
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

function rapportDe(documentId) {
  return dernierRapportParDocument.value.get(documentId) ?? null
}

// ------------------------------------------------------------ analyse

const analyseEnCours = ref(null) // id du document en cours d'analyse

/**
 * Delai propre a l'analyse. L'instance Axios est reglee sur 15 secondes, ce
 * qui convient au reste de l'API mais pas ici : l'analyse enchaine
 * l'extraction du PDF et un appel a l'API d'embeddings par morceau, soit une
 * a deux minutes sur un rapport volumineux. Sans ce depassement, le front
 * abandonnerait pendant que le backend travaille encore, et l'encadrant
 * verrait une erreur alors que le rapport finit par etre correctement
 * enregistre.
 */
const DELAI_ANALYSE_MS = 300000

async function analyser(doc) {
  analyseEnCours.value = doc.id
  erreur.value = null
  info.value = null
  try {
    const { data } = await api.post(`/documents/${doc.id}/similarite`, null, {
      timeout: DELAI_ANALYSE_MS,
    })
    // Recharge plutot que d'inserer a la main : la liste "a examiner" et les
    // rapports du projet doivent rester coherents avec la base.
    await Promise.all([chargerProjet(), rechargerAExaminer()])
    ouvrirRapport(data)
    if (data.nbDocumentsCompares === 0) {
      info.value =
        "Aucun rapport archivé n'était disponible pour la comparaison : ce résultat n'est pas concluant."
    }
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    analyseEnCours.value = null
  }
}

async function rechargerAExaminer() {
  try {
    const { data } = await api.get('/similarites/a-examiner')
    aExaminer.value = data
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

// ------------------------------------------------------------ detail d'un rapport

const detailOuvert = ref(false)
const detailChargement = ref(false)
const rapport = ref(null)

function ouvrirRapport(r) {
  rapport.value = r
  detailOuvert.value = true
}

/**
 * Les listes ne transportent pas les correspondances (variante "resume" du
 * DTO) : il faut recharger le rapport complet pour afficher les extraits.
 */
async function ouvrirRapportParId(rapportId) {
  detailOuvert.value = true
  detailChargement.value = true
  rapport.value = null
  try {
    const { data } = await api.get(`/similarites/${rapportId}`)
    rapport.value = data
  } catch (e) {
    erreur.value = messageErreur(e)
    detailOuvert.value = false
  } finally {
    detailChargement.value = false
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
</script>

<template>
  <LayoutDashboard titre="Rapports de similarité" icone="mdi-file-compare" couleur="teal-darken-2">
    <v-btn variant="text" to="/encadrant" prepend-icon="mdi-arrow-left" class="mb-4">Retour</v-btn>

    <!--
      Avertissement permanent, et non repliable : c'est le garde-fou de toute
      la fonctionnalite. Un score eleve peut resulter d'une source commune
      correctement citee.
    -->
    <v-alert type="info" variant="tonal" density="comfortable" class="mb-4" icon="mdi-scale-balance">
      <div class="text-body-2">
        Ces rapports signalent des <strong>proximités de formulation</strong> avec les rapports
        archivés. Ils ne constituent pas une preuve de plagiat : une forte similarité peut venir
        d'une source commune citée par les deux travaux, d'un vocabulaire technique partagé ou
        d'une méthode standard du domaine. Chaque passage signalé doit être lu avant tout jugement.
      </div>
    </v-alert>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />
    <v-alert v-if="info" type="warning" variant="tonal" density="compact" class="mb-4" :text="info" />

    <v-progress-linear v-if="chargement" indeterminate color="teal-darken-2" class="mb-4" />

    <!-- ============================================ cas a examiner -->
    <v-card variant="outlined" rounded="lg" class="mb-6">
      <v-card-item>
        <v-card-title>À examiner</v-card-title>
        <v-card-subtitle>
          Analyses ayant dépassé le seuil de vigilance, sur l'ensemble de vos projets
        </v-card-subtitle>
      </v-card-item>
      <v-card-text>
        <p v-if="aExaminer.length === 0" class="text-caption text-medium-emphasis">
          Aucun rapport ne dépasse le seuil de vigilance.
        </p>
        <v-list v-else density="comfortable">
          <v-list-item
            v-for="r in aExaminer"
            :key="r.id"
            :title="r.documentNom"
            link
            @click="ouvrirRapportParId(r.id)"
          >
            <template #prepend>
              <v-icon :icon="iconeNiveau(r.niveau)" :color="couleurNiveau(r.niveau)" />
            </template>
            <v-list-item-subtitle>
              {{ libelleNiveau(r.niveau) }} · {{ r.pourcentageMax }} % ·
              analysé le {{ formatDate(r.dateAnalyse) }}
            </v-list-item-subtitle>
            <template #append>
              <v-chip :color="couleurNiveau(r.niveau)" size="small" variant="tonal" label>
                {{ r.pourcentageMax }} %
              </v-chip>
            </template>
          </v-list-item>
        </v-list>
      </v-card-text>
    </v-card>

    <!-- ============================================ documents d'un projet -->
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

    <v-card v-if="!chargementProjet && projetId" variant="outlined" rounded="lg">
      <v-card-item>
        <v-card-title>Documents du projet</v-card-title>
        <v-card-subtitle>
          Lancer une analyse compare le document au corpus des rapports archivés
        </v-card-subtitle>
      </v-card-item>
      <v-card-text>
        <p v-if="documentsAnalysables.length === 0" class="text-caption text-medium-emphasis">
          Aucun document déposé sur ce projet.
        </p>
        <v-list v-else density="comfortable">
          <v-list-item v-for="d in documentsAnalysables" :key="d.id" :title="d.nom">
            <template #prepend>
              <v-icon icon="mdi-file-outline" color="teal-darken-2" />
            </template>

            <v-list-item-subtitle>
              <template v-if="rapportDe(d.id)">
                <v-icon
                  :icon="iconeNiveau(rapportDe(d.id).niveau)"
                  :color="couleurNiveau(rapportDe(d.id).niveau)"
                  size="small"
                  class="mr-1"
                />
                {{ libelleNiveau(rapportDe(d.id).niveau) }} —
                {{ rapportDe(d.id).pourcentageMax }} % ·
                {{ formatDate(rapportDe(d.id).dateAnalyse) }}
              </template>
              <template v-else> Jamais analysé </template>
            </v-list-item-subtitle>

            <template #append>
              <v-btn
                v-if="rapportDe(d.id)"
                variant="text"
                size="small"
                prepend-icon="mdi-file-find-outline"
                @click="ouvrirRapportParId(rapportDe(d.id).id)"
              >
                Voir
              </v-btn>
              <v-btn
                variant="tonal"
                size="small"
                color="teal-darken-2"
                class="ml-2"
                :loading="analyseEnCours === d.id"
                :disabled="analyseEnCours !== null"
                @click="analyser(d)"
              >
                {{ rapportDe(d.id) ? 'Relancer' : 'Analyser' }}
              </v-btn>
            </template>
          </v-list-item>
        </v-list>

        <!--
          L'analyse enchaine extraction PDF et un appel a l'API d'embeddings
          par morceau : prevenir evite que l'encadrant croie l'interface figee.
        -->
        <v-alert
          v-if="analyseEnCours !== null"
          type="info"
          variant="tonal"
          density="compact"
          class="mt-3"
        >
          Analyse en cours — extraction du texte puis calcul des vecteurs.
          Cela peut prendre une à deux minutes sur un rapport volumineux.
        </v-alert>
      </v-card-text>
    </v-card>

    <!-- ============================================ detail d'un rapport -->
    <v-dialog v-model="detailOuvert" max-width="1000" scrollable>
      <v-card rounded="lg">
        <v-card-title class="d-flex align-center">
          <span class="text-truncate">{{ rapport?.documentNom ?? 'Rapport' }}</span>
          <v-spacer />
          <v-chip
            v-if="rapport"
            :color="couleurNiveau(rapport.niveau)"
            variant="tonal"
            label
            :prepend-icon="iconeNiveau(rapport.niveau)"
          >
            {{ libelleNiveau(rapport.niveau) }} — {{ rapport.pourcentageMax }} %
          </v-chip>
        </v-card-title>

        <v-card-text>
          <v-progress-linear v-if="detailChargement" indeterminate color="teal-darken-2" />

          <template v-else-if="rapport">
            <p class="text-body-2 mb-3">{{ explicationNiveau(rapport.niveau) }}</p>

            <v-alert type="info" variant="tonal" density="compact" class="mb-4">
              {{ rapport.avertissement }}
            </v-alert>

            <!--
              Les conditions de l'analyse sont affichees avec le resultat :
              un score n'a de sens que rapporte au corpus compare et aux
              seuils alors en vigueur.
            -->
            <v-row dense class="mb-2">
              <v-col cols="6" sm="3">
                <div class="text-caption text-medium-emphasis">Archives comparées</div>
                <div class="text-body-2">{{ rapport.nbDocumentsCompares }}</div>
              </v-col>
              <v-col cols="6" sm="3">
                <div class="text-caption text-medium-emphasis">Passages analysés</div>
                <div class="text-body-2">{{ rapport.nbMorceauxAnalyses }}</div>
              </v-col>
              <v-col cols="6" sm="3">
                <div class="text-caption text-medium-emphasis">Seuils appliqués</div>
                <div class="text-body-2">
                  {{ Math.round(rapport.seuilAttention * 100) }} % /
                  {{ Math.round(rapport.seuilSuspect * 100) }} %
                </div>
              </v-col>
              <v-col cols="6" sm="3">
                <div class="text-caption text-medium-emphasis">Analysé le</div>
                <div class="text-body-2">{{ formatDate(rapport.dateAnalyse) }}</div>
              </v-col>
            </v-row>

            <v-divider class="my-3" />

            <p v-if="rapport.correspondances.length === 0" class="text-caption text-medium-emphasis">
              Aucun rapprochement retenu.
            </p>

            <v-card
              v-for="(c, i) in rapport.correspondances"
              :key="i"
              variant="outlined"
              rounded="lg"
              class="mb-4"
            >
              <v-card-item>
                <div class="d-flex align-center justify-space-between flex-wrap ga-2">
                  <div>
                    <v-card-title class="text-subtitle-1">{{ c.documentArchiveNom }}</v-card-title>
                    <v-card-subtitle>
                      Passage {{ c.ordreMorceauAnalyse + 1 }} du rapport ·
                      passage {{ c.ordreMorceauArchive + 1 }} de l'archive
                    </v-card-subtitle>
                  </div>
                  <div class="d-flex align-center ga-2">
                    <v-chip :color="couleurNiveau(rapport.niveau)" size="small" variant="tonal" label>
                      {{ c.pourcentage }} %
                    </v-chip>
                    <v-btn
                      v-if="c.documentArchiveId"
                      icon="mdi-download"
                      variant="text"
                      size="small"
                      title="Télécharger l'archive"
                      @click="telecharger(c.documentArchiveId, c.documentArchiveNom)"
                    />
                  </div>
                </div>
              </v-card-item>

              <!-- Les deux passages en regard : c'est la lecture cote a cote qui permet de juger. -->
              <v-card-text>
                <v-row>
                  <v-col cols="12" md="6">
                    <div class="text-caption text-medium-emphasis mb-1">
                      Rapport analysé — {{ rapport.documentNom }}
                    </div>
                    <div class="extrait">{{ c.extraitAnalyse }}</div>
                  </v-col>
                  <v-col cols="12" md="6">
                    <div class="text-caption text-medium-emphasis mb-1">
                      Archive — {{ c.documentArchiveNom }}
                    </div>
                    <div class="extrait extrait--archive">{{ c.extraitArchive }}</div>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-card>
          </template>
        </v-card-text>

        <v-card-actions>
          <v-btn
            v-if="rapport"
            variant="text"
            size="small"
            prepend-icon="mdi-download"
            @click="telecharger(rapport.documentId, rapport.documentNom)"
          >
            Télécharger le rapport analysé
          </v-btn>
          <v-spacer />
          <v-btn variant="text" @click="detailOuvert = false">Fermer</v-btn>
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
  max-height: 260px;
  overflow-y: auto;
}

.extrait--archive {
  border-left-color: rgb(var(--v-theme-error));
}
</style>
