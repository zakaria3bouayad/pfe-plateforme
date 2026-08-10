<script setup>
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import { useAuthStore } from '@/stores/authStore'

const auth = useAuthStore()

const cartes = [
  { titre: 'Mes sujets proposés', icone: 'mdi-lightbulb-on-outline', route: '/encadrant/sujets' },
  { titre: 'Projets encadrés', icone: 'mdi-folder-multiple-outline', texte: 'Lot 3' },
  { titre: 'Jalons', icone: 'mdi-flag-checkered', route: '/encadrant/jalons' },
  { titre: 'Livrables à commenter', icone: 'mdi-comment-text-outline', texte: 'Lot 4' },
  { titre: 'Discussions', icone: 'mdi-forum-outline', texte: 'Lot 5' },
  { titre: 'Rapports de similarité', icone: 'mdi-file-compare', texte: 'Lot 6' },
]
</script>

<template>
  <LayoutDashboard titre="Espace encadrant" icone="mdi-account-tie-outline" couleur="teal-darken-2">
    <v-alert type="success" variant="tonal" class="mb-6">
      Bienvenue {{ auth.nomComplet }} — vous êtes connecté en tant qu'encadrant.
    </v-alert>

    <v-row>
      <v-col v-for="c in cartes" :key="c.titre" cols="12" sm="6" md="4">
        <v-card variant="outlined" rounded="lg" class="h-100" :to="c.route" :link="!!c.route" :hover="!!c.route">
          <v-card-item>
            <v-icon :icon="c.icone" size="32" color="teal-darken-2" />
            <v-card-title class="text-subtitle-1 mt-2">{{ c.titre }}</v-card-title>
            <v-card-subtitle>{{ c.route ? 'Ouvrir' : `Disponible au ${c.texte}` }}</v-card-subtitle>
          </v-card-item>
        </v-card>
      </v-col>
    </v-row>
  </LayoutDashboard>
</template>
