<script setup>
/**
 * Consultation du journal d'audit (Lot 7, etape 7.12), cote administrateur.
 *
 * Seule vue de l'application a consommer une reponse paginee (Page<...>) du
 * backend plutot qu'une simple liste : le journal grossit indefiniment
 * (AuditController, etape 7.9), une pagination cote serveur est donc
 * necessaire. Filtres facultatifs et combinables, comme la requete
 * EntreeAuditRepository.rechercher sous-jacente.
 */
import { ref, watch, onMounted } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'

const COULEUR = 'blue-grey-darken-2'
const TAILLE_PAGE = 20

const entrees = ref([])
const pageCourante = ref(1) // 1-indexe cote UI (v-pagination), 0-indexe cote API
const nbPages = ref(0)
const nbEntrees = ref(0)

const chargement = ref(true)
const erreur = ref(null)

const filtreActeur = ref('')
const filtreAction = ref('')
const filtreDepuis = ref('')
const filtreJusqua = ref('')

function parametresFiltres() {
  const params = {}
  if (filtreActeur.value.trim()) params.acteur = filtreActeur.value.trim()
  if (filtreAction.value.trim()) params.action = filtreAction.value.trim()
  if (filtreDepuis.value) params.depuis = `${filtreDepuis.value}T00:00:00`
  if (filtreJusqua.value) params.jusqua = `${filtreJusqua.value}T23:59:59`
  return params
}

async function charger() {
  chargement.value = true
  erreur.value = null
  try {
    const { data } = await api.get('/admin/audit', {
      params: { ...parametresFiltres(), page: pageCourante.value - 1, size: TAILLE_PAGE },
    })
    entrees.value = data.content
    nbPages.value = data.totalPages
    nbEntrees.value = data.totalElements
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargement.value = false
  }
}

/** Toute recherche repart de la premiere page : les filtres precedents ne s'y appliquaient pas. */
function rechercher() {
  if (pageCourante.value === 1) {
    charger()
  } else {
    pageCourante.value = 1
  }
}

function reinitialiser() {
  filtreActeur.value = ''
  filtreAction.value = ''
  filtreDepuis.value = ''
  filtreJusqua.value = ''
  rechercher()
}

watch(pageCourante, charger)
onMounted(charger)
</script>

<template>
  <LayoutDashboard titre="Journal d'audit" icone="mdi-book-clock-outline" :couleur="COULEUR">
    <v-btn variant="text" to="/admin" prepend-icon="mdi-arrow-left" class="mb-4">Retour</v-btn>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />

    <!-- ============================================ filtres -->
    <v-card variant="outlined" rounded="lg" class="mb-4">
      <v-card-text>
        <v-form @submit.prevent="rechercher">
          <v-row dense>
            <v-col cols="12" sm="6" md="3">
              <v-text-field
                v-model="filtreActeur"
                label="Acteur"
                placeholder="email (partiel)"
                variant="outlined"
                density="compact"
                clearable
                hide-details
              />
            </v-col>
            <v-col cols="12" sm="6" md="3">
              <v-text-field
                v-model="filtreAction"
                label="Action"
                placeholder="ex. SUJET_VALIDE"
                variant="outlined"
                density="compact"
                clearable
                hide-details
              />
            </v-col>
            <v-col cols="6" md="2">
              <v-text-field
                v-model="filtreDepuis"
                label="Depuis"
                type="date"
                variant="outlined"
                density="compact"
                hide-details
              />
            </v-col>
            <v-col cols="6" md="2">
              <v-text-field
                v-model="filtreJusqua"
                label="Jusqu'au"
                type="date"
                variant="outlined"
                density="compact"
                hide-details
              />
            </v-col>
            <v-col cols="12" md="2" class="d-flex align-center ga-2">
              <v-btn type="submit" color="primary" variant="tonal">Filtrer</v-btn>
              <v-btn variant="text" @click="reinitialiser">Réinitialiser</v-btn>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
    </v-card>

    <v-progress-linear v-if="chargement" indeterminate :color="COULEUR" class="mb-4" />

    <v-alert v-if="!chargement && entrees.length === 0" type="info" variant="tonal">
      Aucune entrée ne correspond à ces critères.
    </v-alert>

    <!-- ============================================ resultats -->
    <v-table v-if="!chargement && entrees.length" density="comfortable" class="mb-4">
      <thead>
        <tr>
          <th>Horodatage</th>
          <th>Acteur</th>
          <th>Action</th>
          <th>Cible</th>
          <th>Détail</th>
          <th>IP</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="e in entrees" :key="e.id">
          <td class="text-no-wrap">{{ new Date(e.horodatage).toLocaleString('fr-FR') }}</td>
          <td>{{ e.acteur }}</td>
          <td>
            <v-chip size="small" variant="tonal" :color="COULEUR" label>{{ e.action }}</v-chip>
          </td>
          <td>{{ e.cible ?? '—' }}</td>
          <td class="text-medium-emphasis">{{ e.detail ?? '—' }}</td>
          <td class="text-no-wrap">{{ e.ip ?? '—' }}</td>
        </tr>
      </tbody>
    </v-table>

    <div v-if="!chargement && nbEntrees > 0" class="d-flex flex-column align-center ga-2">
      <span class="text-caption text-medium-emphasis">{{ nbEntrees }} entrée(s) au total</span>
      <v-pagination v-model="pageCourante" :length="nbPages" :total-visible="7" density="comfortable" />
    </div>
  </LayoutDashboard>
</template>
