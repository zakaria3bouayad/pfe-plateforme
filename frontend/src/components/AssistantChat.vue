<script setup>
import { ref, reactive, nextTick } from 'vue'
import { poserQuestionAssistant } from '@/services/assistant'
import api, { messageErreur } from '@/services/api'

/**
 * Widget de chat de l'assistant conversationnel RAG (Lot 8, etapes 8.10 a
 * 8.12 ; EF-48/EF-49, diagramme de sequence 6).
 *
 *  - le badge "genere par IA" (EF-49) - uniquement sur une vraie reponse
 *    generee, jamais sur les messages "impossible" qui sont des textes fixes
 *    de RagService, pas une production du modele ;
 *  - le bouton d'escalade (EF-48 "oriente vers un interlocuteur humain"),
 *    affiche seulement quand la reponse est impossible, qui appelle
 *    POST /api/assistant/escalade (etape 8.9) avec la question d'origine.
 *
 * Pas d'affichage separe des sources citees (retire suite aux tests de
 * 8.12) : la citation reste satisfaite par le prompt de RagService, qui
 * demande au modele de citer les titres directement dans sa reponse - les
 * k=5 passages retrouves ne sont pas tous forcement utilises par le modele,
 * les afficher tous a part etait plus trompeur qu'utile.
 *
 * Chaque message assistant est un objet reactive() (pas un objet brut
 * pousse dans un ref([])) : sans ca, muter messageAssistant.contenu au fil
 * du flux SSE ne declenche aucun re-rendu (on ecrit sur l'objet source, pas
 * sur le proxy reactif renvoye par le tableau) - la reponse ne s'affichait
 * qu'au prochain changement d'etat qui forcait un re-rendu.
 */

const messages = ref([])
const question = ref('')
const enCours = ref(false)
const erreur = ref(null)
const zoneMessages = ref(null)

async function poser() {
  const texte = question.value.trim()
  if (!texte || enCours.value) return

  erreur.value = null
  messages.value.push({ role: 'utilisateur', contenu: texte })
  question.value = ''

  const messageAssistant = reactive({
    role: 'assistant',
    contenu: '',
    enAttente: true,
    question: texte,
    impossible: false,
    escaladeEnCours: false,
    escaladeReponse: null,
    escaladeErreur: null,
  })
  messages.value.push(messageAssistant)

  enCours.value = true
  await defilerVersLeBas()

  try {
    await poserQuestionAssistant(texte, {
      surToken: (fragment) => {
        messageAssistant.enAttente = false
        messageAssistant.contenu += fragment
        defilerVersLeBas()
      },
      surImpossible: (message) => {
        messageAssistant.enAttente = false
        messageAssistant.impossible = true
        messageAssistant.contenu = message
      },
    })
  } catch {
    messageAssistant.enAttente = false
    messageAssistant.impossible = true
    messageAssistant.contenu = "L'assistant est temporairement indisponible."
    erreur.value = "La connexion a l'assistant a echoue."
  } finally {
    enCours.value = false
    await defilerVersLeBas()
  }
}

/** Mise en relation avec l'encadrant (etape 8.9), depuis un message "impossible". */
async function demanderEscalade(m) {
  if (m.escaladeEnCours || m.escaladeReponse) return

  m.escaladeEnCours = true
  m.escaladeErreur = null
  try {
    const { data } = await api.post('/assistant/escalade', { question: m.question })
    m.escaladeReponse = data
  } catch (e) {
    m.escaladeErreur = messageErreur(e)
  } finally {
    m.escaladeEnCours = false
  }
}

function surEntree(e) {
  if (e.shiftKey) return
  e.preventDefault()
  poser()
}

async function defilerVersLeBas() {
  await nextTick()
  if (zoneMessages.value) {
    zoneMessages.value.scrollTop = zoneMessages.value.scrollHeight
  }
}
</script>

<template>
  <v-card variant="outlined" rounded="lg">
    <v-card-item>
      <v-card-title>
        <v-icon icon="mdi-robot-outline" class="mr-2" />
        Assistant
      </v-card-title>
      <v-card-subtitle>Questions sur l'utilisation de la plateforme</v-card-subtitle>
    </v-card-item>

    <v-divider />

    <div ref="zoneMessages" class="assistant-zone pa-4">
      <p v-if="messages.length === 0" class="text-caption text-medium-emphasis text-center">
        Posez une question sur l'utilisation de la plateforme.
      </p>

      <div
        v-for="(m, i) in messages"
        :key="i"
        class="d-flex flex-column mb-3"
        :class="m.role === 'utilisateur' ? 'align-end' : 'align-start'"
      >
        <div
          v-if="m.role === 'assistant' && !m.enAttente && !m.impossible"
          class="text-caption text-medium-emphasis mb-1 d-flex align-center ga-1"
        >
          <v-icon icon="mdi-creation" size="12" />
          Réponse générée par IA
        </div>

        <div class="bulle" :class="m.role === 'utilisateur' ? 'bulle-moi' : 'bulle-autre'">
          <v-progress-circular v-if="m.enAttente" indeterminate size="16" width="2" />
          <span v-else class="text-body-2 text-wrap">{{ m.contenu }}</span>
        </div>

        <div v-if="m.impossible" class="mt-2" style="max-width: 80%">
          <v-btn
            v-if="!m.escaladeReponse"
            size="small"
            variant="tonal"
            color="secondary"
            prepend-icon="mdi-account-question-outline"
            :loading="m.escaladeEnCours"
            @click="demanderEscalade(m)"
          >
            Contacter mon encadrant
          </v-btn>
          <v-alert v-else type="success" variant="tonal" density="compact" class="text-caption">
            {{ m.escaladeReponse.message }}
          </v-alert>
          <div v-if="m.escaladeErreur" class="text-caption text-error mt-1">{{ m.escaladeErreur }}</div>
        </div>
      </div>
    </div>

    <v-alert v-if="erreur" type="error" variant="tonal" density="compact" class="mx-4 mb-2" :text="erreur" />

    <v-divider />

    <v-card-text class="d-flex align-end ga-2">
      <v-textarea
        v-model="question"
        label="Votre question"
        variant="outlined"
        density="comfortable"
        rows="1"
        auto-grow
        max-rows="4"
        hide-details
        :disabled="enCours"
        @keydown.enter="surEntree"
      />
      <v-btn icon="mdi-send" color="primary" :loading="enCours" :disabled="!question.trim()" @click="poser" />
    </v-card-text>
  </v-card>
</template>

<style scoped>
.assistant-zone {
  max-height: 420px;
  min-height: 160px;
  overflow-y: auto;
}

.bulle {
  max-width: 80%;
  padding: 8px 12px;
  border-radius: 12px;
}

.bulle-moi {
  background-color: rgb(var(--v-theme-primary));
  color: rgb(var(--v-theme-on-primary));
}

.bulle-autre {
  background-color: rgba(var(--v-theme-on-surface), 0.06);
}
</style>
