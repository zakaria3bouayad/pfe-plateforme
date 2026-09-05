package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.ArticleAide;
import com.pfe.pfe_backend.domain.enums.StatutIndexationArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleAideRepository extends JpaRepository<ArticleAide, Long> {

    /** Idempotence du seed de demarrage (DataInitializer) : ne pas recreer un article deja charge. */
    boolean existsByTitre(String titre);

    /** Articles pas encore exploitables par la recherche (EN_ATTENTE ou ECHEC), a (re)vectoriser. */
    List<ArticleAide> findByStatutIndexationNot(StatutIndexationArticle statut);

    /**
     * Recherche vectorielle des k articles les plus proches d'une question
     * (Lot 8, etape 8.6 - meme pattern que
     * MorceauDocumentRepository.chercherPlusProches au lot 6).
     *
     * Requete native : l'operateur de distance cosinus <=> de pgvector n'a
     * pas d'equivalent JPQL, et c'est le seul moyen de laisser PostgreSQL
     * trier et limiter plutot que de rapatrier tout le corpus en memoire.
     *
     * Le score renvoye est 1 - distance : pgvector fournit une distance
     * (0 = identique), on veut une similarite (1 = identique). Exact
     * seulement sur des vecteurs normalises, ce que gemini-embedding-2
     * garantit (meme raisonnement qu'au lot 6).
     *
     * Seuls les articles VECTORISE participent : un article EN_ATTENTE ou en
     * ECHEC n'a pas de vecteur exploitable. Pas d'exclusion de "soi-meme"
     * ici, contrairement au lot 6 : la question posee n'est pas elle-meme un
     * article du corpus.
     */
    @Query(value = """
            SELECT a.id        AS "id",
                   a.titre     AS "titre",
                   a.contenu   AS "contenu",
                   a.categorie AS "categorie",
                   1 - (a.vecteur <=> CAST(:vecteur AS vector)) AS "score"
            FROM article_aide a
            WHERE a.statut_indexation = 'VECTORISE'
            ORDER BY a.vecteur <=> CAST(:vecteur AS vector)
            LIMIT :limite
            """, nativeQuery = true)
    List<ArticleProche> chercherPlusProches(
            @Param("vecteur") String vecteur,
            @Param("limite") int limite);

    /**
     * Projection du resultat de la recherche vectorielle. Alias entre
     * guillemets doubles dans la requete : sans cela PostgreSQL les
     * replierait en minuscules et le mappage sur ces getters en casse mixte
     * deviendrait incertain (meme precaution que MorceauProche au lot 6).
     */
    interface ArticleProche {
        Long getId();
        String getTitre();
        String getContenu();
        String getCategorie();
        Double getScore();
    }
}
