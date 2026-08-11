<script setup>
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import { useAuthStore } from '@/stores/authStore'

const auth = useAuthStore()

const cartes = [
  { titre: 'Mon projet', icone: 'mdi-folder-outline', route: '/etudiant/projet' },
  { titre: 'Checkpoints', icone: 'mdi-flag-checkered', route: '/etudiant/jalons' },
  { titre: 'Sujets disponibles', icone: 'mdi-lightbulb-outline', route: '/etudiant/sujets' },
  { titre: 'Mon équipe', icone: 'mdi-account-group-outline', route: '/etudiant/equipe' },
  { titre: 'Documents', icone: 'mdi-file-document-outline', route: '/etudiant/documents' },
  { titre: 'Messagerie', icone: 'mdi-forum-outline', texte: 'Lot 5' },
  { titre: 'Bibliothèque', icone: 'mdi-library-outline', texte: 'Lot 5' },
]
</script>

<template>
  <LayoutDashboard titre="Espace étudiant" icone="mdi-school-outline" couleur="primary">
    <v-alert type="success" variant="tonal" class="mb-6">
      Bienvenue {{ auth.nomComplet }} — vous êtes connecté en tant qu'étudiant.
    </v-alert>

    <v-row>
      <v-col v-for="c in cartes" :key="c.titre" cols="12" sm="6" md="4">
        <v-card variant="outlined" rounded="lg" class="h-100" :to="c.route" :link="!!c.route" :hover="!!c.route">
          <v-card-item>
            <v-icon :icon="c.icone" size="32" color="primary" />
            <v-card-title class="text-subtitle-1 mt-2">{{ c.titre }}</v-card-title>
            <v-card-subtitle>{{ c.route ? 'Ouvrir' : `Disponible au ${c.texte}` }}</v-card-subtitle>
          </v-card-item>
        </v-card>
      </v-col>
    </v-row>
  </LayoutDashboard>
</template>
