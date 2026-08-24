package com.pfe.pfe_backend.domain.enums;

/**
 * Resultat de l'extraction du texte d'un document (Lot 6, etape 6.2).
 *
 * Un statut different de EXTRAIT signifie que le document ne pourra pas
 * entrer dans le calcul de similarite : il faut pouvoir le distinguer d'une
 * simple absence d'indexation, sans quoi une archive sortirait du corpus
 * sans que personne ne sache pourquoi.
 *
 * ATTENTION avant d'ajouter une valeur : Hibernate genere une contrainte
 * CHECK sur la colonne statut au moment ou il CREE la table, en y figeant la
 * liste des valeurs de l'enum. Avec ddl-auto=update, cette contrainte n'est
 * jamais mise a jour ensuite : une nouvelle valeur est rejetee par PostgreSQL
 * au commit, alors que le code Java compile sans rien signaler. C'est
 * precisement ce qui s'est produit en ajoutant VECTORISE et ECHEC_EMBEDDING a
 * l'etape 6.3, la table ayant ete creee a l'etape 6.2. Correctif applique en
 * base de developpement :
 *
 *   ALTER TABLE indexation_document DROP CONSTRAINT indexation_document_statut_check;
 *   ALTER TABLE indexation_document ADD CONSTRAINT indexation_document_statut_check
 *       CHECK (statut IN ('EXTRAIT','VECTORISE','ECHEC_EMBEDDING','VIDE',
 *                         'TYPE_NON_SUPPORTE','ECHEC'));
 *
 * La migration V5 (etape 6.10) doit declarer la contrainte avec les six
 * valeurs, pour qu'une base recreee de zero soit d'emblee coherente.
 */
public enum StatutIndexation {

    /**
     * Texte recupere avec succes, mais vecteurs pas encore calcules.
     * Etat intermediaire : le document ne participe pas encore au corpus.
     */
    EXTRAIT,

    /**
     * Texte extrait ET vectorise (Lot 6, etape 6.3). Seul statut permettant
     * a un document d'entrer dans le corpus de comparaison.
     */
    VECTORISE,

    /**
     * Texte correctement extrait, mais l'appel a l'API d'embeddings a echoue
     * (quota depasse, cle invalide, panne reseau). Distinct de ECHEC : le
     * texte est en base, seule la vectorisation est a rejouer.
     */
    ECHEC_EMBEDDING,

    /**
     * PDF lisible mais sans couche texte (document scanne, ou uniquement des
     * images). Aucun OCR n'est prevu : le cas est trace, pas rattrape.
     */
    VIDE,

    /** Le document n'est pas un PDF : aucune extraction tentee. */
    TYPE_NON_SUPPORTE,

    /** PDF illisible : fichier corrompu, chiffre, ou erreur d'acces au stockage. */
    ECHEC
}
