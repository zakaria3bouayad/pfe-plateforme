<script setup>
import { ref, onMounted } from 'vue'
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import api, { messageErreur } from '@/services/api'

const onglet = ref('filieres')

const filieres = ref([])
const promotions = ref([])
const chargement = ref(true)
const erreur = ref(null)
const succes = ref(null)

const obligatoire = [(v) => !!v || 'Champ obligatoire']

async function charger() {
  chargement.value = true
  erreur.value = null
  try {
    const [rf, rp] = await Promise.all([api.get('/filieres'), api.get('/promotions')])
    filieres.value = rf.data
    promotions.value = rp.data
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargement.value = false
  }
}

onMounted(charger)

// ------------------------------------------------------------ filieres

const dialogueFiliere = ref(false)
const modeEditionFiliere = ref(false)
const filiereEnEdition = ref(null)
const formulaireFiliereValide = ref(false)
const fFiliere = ref({ code: '', libelle: '', departement: '' })

function ouvrirCreationFiliere() {
  modeEditionFiliere.value = false
  filiereEnEdition.value = null
  fFiliere.value = { code: '', libelle: '', departement: '' }
  dialogueFiliere.value = true
}

function ouvrirEditionFiliere(f) {
  modeEditionFiliere.value = true
  filiereEnEdition.value = f
  fFiliere.value = { code: f.code, libelle: f.libelle, departement: f.departement || '' }
  dialogueFiliere.value = true
}

async function soumettreFiliere() {
  if (!formulaireFiliereValide.value) return
  erreur.value = null
  try {
    if (modeEditionFiliere.value) {
      await api.put(`/filieres/${filiereEnEdition.value.id}`, fFiliere.value)
    } else {
      await api.post('/filieres', fFiliere.value)
    }
    dialogueFiliere.value = false
    succes.value = modeEditionFiliere.value ? 'Filière modifiée.' : 'Filière créée.'
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

async function supprimerFiliere(f) {
  if (!confirm(`Supprimer la filière "${f.libelle}" ?`)) return
  erreur.value = null
  try {
    await api.delete(`/filieres/${f.id}`)
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

// ------------------------------------------------------------ promotions

const dialoguePromotion = ref(false)
const modeEditionPromotion = ref(false)
const promotionEnEdition = ref(null)
const formulairePromotionValide = ref(false)
const fPromotion = ref({ annee: new Date().getFullYear(), libelle: '' })

function ouvrirCreationPromotion() {
  modeEditionPromotion.value = false
  promotionEnEdition.value = null
  fPromotion.value = { annee: new Date().getFullYear(), libelle: '' }
  dialoguePromotion.value = true
}

function ouvrirEditionPromotion(p) {
  modeEditionPromotion.value = true
  promotionEnEdition.value = p
  fPromotion.value = { annee: p.annee, libelle: p.libelle }
  dialoguePromotion.value = true
}

async function soumettrePromotion() {
  if (!formulairePromotionValide.value) return
  erreur.value = null
  try {
    if (modeEditionPromotion.value) {
      await api.put(`/promotions/${promotionEnEdition.value.id}`, fPromotion.value)
    } else {
      await api.post('/promotions', fPromotion.value)
    }
    dialoguePromotion.value = false
    succes.value = modeEditionPromotion.value ? 'Promotion modifiée.' : 'Promotion créée.'
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}

async function supprimerPromotion(p) {
  if (!confirm(`Supprimer la promotion "${p.libelle}" ?`)) return
  erreur.value = null
  try {
    await api.delete(`/promotions/${p.id}`)
    await charger()
  } catch (e) {
    erreur.value = messageErreur(e)
  }
}
</script>

<template>
  <LayoutDashboard titre="Filières et promotions" icone="mdi-school-outline" couleur="deep-purple-darken-2">
    <v-btn variant="text" to="/admin" prepend-icon="mdi-arrow-left" class="mb-4">Retour</v-btn>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mb-4" :text="erreur" />
    <v-alert v-if="succes" type="success" variant="tonal" density="compact" class="mb-4" :text="succes" />

    <v-progress-linear v-if="chargement" indeterminate color="deep-purple-darken-2" class="mb-4" />

    <v-tabs v-model="onglet" color="deep-purple-darken-2" class="mb-4">
      <v-tab value="filieres">Filières</v-tab>
      <v-tab value="promotions">Promotions</v-tab>
    </v-tabs>

    <v-window v-model="onglet">
      <v-window-item value="filieres">
        <div class="d-flex justify-end mb-3">
          <v-btn color="deep-purple-darken-2" prepend-icon="mdi-plus" @click="ouvrirCreationFiliere">
            Ajouter une filière
          </v-btn>
        </div>

        <v-alert v-if="!chargement && filieres.length === 0" type="info" variant="tonal">
          Aucune filière pour l'instant.
        </v-alert>

        <v-table v-else density="comfortable">
          <thead>
            <tr>
              <th>Code</th>
              <th>Libellé</th>
              <th>Département</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="f in filieres" :key="f.id">
              <td>{{ f.code }}</td>
              <td>{{ f.libelle }}</td>
              <td>{{ f.departement || '—' }}</td>
              <td class="text-right">
                <v-btn icon="mdi-pencil-outline" variant="text" size="small" @click="ouvrirEditionFiliere(f)" />
                <v-btn
                  icon="mdi-delete-outline"
                  variant="text"
                  size="small"
                  color="error"
                  @click="supprimerFiliere(f)"
                />
              </td>
            </tr>
          </tbody>
        </v-table>
      </v-window-item>

      <v-window-item value="promotions">
        <div class="d-flex justify-end mb-3">
          <v-btn color="deep-purple-darken-2" prepend-icon="mdi-plus" @click="ouvrirCreationPromotion">
            Ajouter une promotion
          </v-btn>
        </div>

        <v-alert v-if="!chargement && promotions.length === 0" type="info" variant="tonal">
          Aucune promotion pour l'instant.
        </v-alert>

        <v-table v-else density="comfortable">
          <thead>
            <tr>
              <th>Année</th>
              <th>Libellé</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in promotions" :key="p.id">
              <td>{{ p.annee }}</td>
              <td>{{ p.libelle }}</td>
              <td class="text-right">
                <v-btn icon="mdi-pencil-outline" variant="text" size="small" @click="ouvrirEditionPromotion(p)" />
                <v-btn
                  icon="mdi-delete-outline"
                  variant="text"
                  size="small"
                  color="error"
                  @click="supprimerPromotion(p)"
                />
              </td>
            </tr>
          </tbody>
        </v-table>
      </v-window-item>
    </v-window>

    <!-- Creation / edition filiere -->
    <v-dialog v-model="dialogueFiliere" max-width="480">
      <v-card rounded="lg">
        <v-card-title class="pt-4">{{ modeEditionFiliere ? 'Modifier la filière' : 'Ajouter une filière' }}</v-card-title>
        <v-card-text>
          <v-form v-model="formulaireFiliereValide" @submit.prevent="soumettreFiliere">
            <v-text-field
              v-model="fFiliere.code"
              label="Code"
              variant="outlined"
              density="comfortable"
              :rules="obligatoire"
              class="mb-2"
            />
            <v-text-field
              v-model="fFiliere.libelle"
              label="Libellé"
              variant="outlined"
              density="comfortable"
              :rules="obligatoire"
              class="mb-2"
            />
            <v-text-field
              v-model="fFiliere.departement"
              label="Département (facultatif)"
              variant="outlined"
              density="comfortable"
            />
            <v-card-actions class="px-0">
              <v-spacer />
              <v-btn variant="text" @click="dialogueFiliere = false">Annuler</v-btn>
              <v-btn color="deep-purple-darken-2" type="submit" :disabled="!formulaireFiliereValide">
                {{ modeEditionFiliere ? 'Enregistrer' : 'Ajouter' }}
              </v-btn>
            </v-card-actions>
          </v-form>
        </v-card-text>
      </v-card>
    </v-dialog>

    <!-- Creation / edition promotion -->
    <v-dialog v-model="dialoguePromotion" max-width="480">
      <v-card rounded="lg">
        <v-card-title class="pt-4">
          {{ modeEditionPromotion ? 'Modifier la promotion' : 'Ajouter une promotion' }}
        </v-card-title>
        <v-card-text>
          <v-form v-model="formulairePromotionValide" @submit.prevent="soumettrePromotion">
            <v-text-field
              v-model.number="fPromotion.annee"
              type="number"
              min="2000"
              label="Année"
              variant="outlined"
              density="comfortable"
              :rules="obligatoire"
              class="mb-2"
            />
            <v-text-field
              v-model="fPromotion.libelle"
              label="Libellé"
              variant="outlined"
              density="comfortable"
              :rules="obligatoire"
            />
            <v-card-actions class="px-0">
              <v-spacer />
              <v-btn variant="text" @click="dialoguePromotion = false">Annuler</v-btn>
              <v-btn color="deep-purple-darken-2" type="submit" :disabled="!formulairePromotionValide">
                {{ modeEditionPromotion ? 'Enregistrer' : 'Ajouter' }}
              </v-btn>
            </v-card-actions>
          </v-form>
        </v-card-text>
      </v-card>
    </v-dialog>
  </LayoutDashboard>
</template>
