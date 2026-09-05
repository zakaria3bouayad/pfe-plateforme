package com.pfe.pfe_backend.domain.enums;

/**
 * Etat d'indexation d'un ArticleAide dans le corpus de l'assistant RAG
 * (Lot 8, etape 8.4).
 *
 * Plus simple que StatutIndexation (Lot 6) : un article est redige a la
 * main (etape 8.5), pas extrait d'un PDF, donc aucun des cas d'echec
 * d'extraction (VIDE, TYPE_NON_SUPPORTE, ECHEC d'extraction...) ne
 * s'applique ici - seul l'appel a l'API d'embeddings peut echouer.
 *
 * Meme precaution qu'au lot 6 (cf. StatutIndexation) : Hibernate fige la
 * contrainte CHECK sur cette colonne au moment ou il cree la table.
 * V7__assistant.sql (etape 8.14) devra declarer explicitement les trois
 * valeurs ci-dessous.
 */
public enum StatutIndexationArticle {

    /** Valeur par defaut a la creation : pas encore vectorise. */
    EN_ATTENTE,

    /** Vecteur calcule : l'article participe a la recherche du RAG (etape 8.6). */
    VECTORISE,

    /** L'appel a l'API d'embeddings a echoue (quota, cle invalide, panne reseau) : a rejouer. */
    ECHEC
}
