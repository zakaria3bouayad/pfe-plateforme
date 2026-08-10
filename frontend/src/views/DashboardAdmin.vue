<script setup>
import LayoutDashboard from '@/components/LayoutDashboard.vue'
import { useAuthStore } from '@/stores/authStore'

const auth = useAuthStore()

const cartes = [
  { titre: 'Utilisateurs', icone: 'mdi-account-multiple-outline', route: '/admin/utilisateurs' },
  { titre: 'Filières et promotions', icone: 'mdi-school-outline', route: '/admin/referentiel' },
  { titre: 'Validation des sujets', icone: 'mdi-check-circle-outline', route: '/admin/sujets' },
  { titre: 'Tous les projets', icone: 'mdi-folder-multiple-outline', route: '/admin/projets' },
  { titre: 'Statistiques', icone: 'mdi-chart-box-outline', route: '/admin/stats' },
  { titre: "Journal d'audit", icone: 'mdi-shield-search', texte: 'Lot 7' },
]
</script>

<template>
  <LayoutDashboard titre="Administration" icone="mdi-shield-crown-outline" couleur="deep-purple-darken-2">
    <v-alert type="success" variant="tonal" class="mb-6">
      Bienvenue {{ auth.nomComplet }} — vous êtes connecté en tant qu'administrateur.
    </v-alert>

    <v-row>
      <v-col v-for="c in cartes" :key="c.titre" cols="12" sm="6" md="4">
        <v-card variant="outlined" rounded="lg" class="h-100" :to="c.route" :link="!!c.route" :hover="!!c.route">
          <v-card-item>
            <v-icon :icon="c.icone" size="32" color="deep-purple-darken-2" />
            <v-card-title class="text-subtitle-1 mt-2">{{ c.titre }}</v-card-title>
            <v-card-subtitle>{{ c.route ? 'Ouvrir' : `Disponible au ${c.texte}` }}</v-card-subtitle>
          </v-card-item>
        </v-card>
      </v-col>
    </v-row>
  </LayoutDashboard>
</template>
