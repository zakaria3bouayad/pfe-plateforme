<script setup>
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import AssistantChat from '@/components/AssistantChat.vue'

/**
 * Page de l'assistant conversationnel (Lot 8, etape 8.12). Contrairement a
 * la messagerie (reservee au chef d'equipe), l'assistant est ouvert a tout
 * etudiant (EF-48) : pas de chargement d'equipe/projet ici, le widget
 * (AssistantChat, etapes 8.10-8.11) est autonome.
 */
</script>

<template>
  <LayoutDashboard titre="Assistant" icone="mdi-robot-outline" couleur="primary">
    <v-btn variant="text" to="/etudiant" prepend-icon="mdi-arrow-left" class="mb-4">Retour</v-btn>

    <AssistantChat />
  </LayoutDashboard>
</template>
