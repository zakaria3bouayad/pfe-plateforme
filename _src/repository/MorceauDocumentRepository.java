package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.MorceauDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MorceauDocumentRepository extends JpaRepository<MorceauDocument, Long> {

    List<MorceauDocument> findByIndexationIdOrderByOrdreAsc(Long indexationId);

    /**
     * Suppression en masse avant reindexation. Requete de modification
     * directe plutot que deleteBy derive : evite de charger en memoire des
     * dizaines de morceaux, vecteurs compris, pour les supprimer un a un.
     */
    @Modifying
    @Query("DELETE FROM MorceauDocument m WHERE m.indexation.id = :indexationId")
    void supprimerParIndexation(@Param("indexationId") Long indexationId);

    long countByIndexationId(Long indexationId);

    /**
     * Recherche les morceaux du corpus archive les plus proches d'un vecteur
     * donne (Lot 6, etape 6.4).
     *
     * Requete native, et non JPQL : l'operateur de distance cosinus <=> de
     * pgvector n'a pas d'equivalent en JPQL. C'est aussi le seul moyen de
     * laisser PostgreSQL faire le tri et la limitation, plutot que de
     * rapatrier tout le corpus en memoire pour le trier en Java.
     *
     * Le score renvoye est 1 - distance : pgvector fournit une distance
     * (0 = identique), on veut une similarite (1 = identique). Cette
     * conversion n'est exacte que sur des vecteurs normalises, ce que
     * gemini-embedding-2 garantit, y compris aux dimensions tronquees.
     *
     * Le vecteur est passe en chaine au format pgvector "[0.1,0.2,...]" puis
     * converti : JPA ne sait pas lier un float[] a un parametre de type
     * vector.
     *
     * Filtres : uniquement les documents marques archives et non supprimes,
     * et jamais le document analyse lui-meme, qui se rapprocherait
     * evidemment de lui-meme avec un score de 1.
     *
     * Les autres documents du meme projet ne sont volontairement PAS exclus.
     * Consequence assumee : si une version anterieure du meme rapport a ete
     * archivee, elle ressortira avec un score tres eleve. C'est un faux
     * positif previsible, que l'encadrant identifie d'un coup d'oeil au nom
     * du document ; l'exclure creerait a l'inverse un angle mort permanent,
     * dans lequel une reprise entre deux projets d'une meme equipe passerait
     * inapercue.
     */
    @Query(value = """
            SELECT d.id            AS "documentId",
                   d.nom           AS "documentNom",
                   m.ordre         AS "ordre",
                   m.texte         AS "texte",
                   1 - (m.vecteur <=> CAST(:vecteur AS vector)) AS "score"
            FROM morceau_document m
            JOIN indexation_document i ON i.id = m.indexation_id
            JOIN document d           ON d.id = i.document_id
            WHERE d.archive = TRUE
              AND d.supprime = FALSE
              AND d.id <> :documentExclu
            ORDER BY m.vecteur <=> CAST(:vecteur AS vector)
            LIMIT :limite
            """, nativeQuery = true)
    List<MorceauProche> chercherPlusProches(
            @Param("vecteur") String vecteur,
            @Param("documentExclu") Long documentExclu,
            @Param("limite") int limite);

    /**
     * Projection du resultat de la recherche vectorielle.
     *
     * Les alias de la requete sont entre guillemets doubles : sans cela
     * PostgreSQL les replierait en minuscules et le mappage sur les getters
     * en casse mixte deviendrait incertain.
     */
    interface MorceauProche {
        Long getDocumentId();
        String getDocumentNom();
        Integer getOrdre();
        String getTexte();
        Double getScore();
    }
}
