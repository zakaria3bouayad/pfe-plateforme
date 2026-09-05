package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.ArticleAide;
import com.pfe.pfe_backend.domain.enums.StatutIndexationArticle;
import com.pfe.pfe_backend.repository.ArticleAideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.StringJoiner;

/**
 * Indexation et recherche vectorielle du corpus documentation/FAQ de
 * l'assistant conversationnel RAG (Lot 8, etape 8.6).
 *
 * Meme principe que SimilariteService (lot 6) pour la recherche : requete
 * SQL native pgvector (ArticleAideRepository.chercherPlusProches), qui
 * laisse PostgreSQL trier et limiter plutot que de rapatrier tout le corpus
 * en memoire.
 *
 * Contrairement au lot 6, pas de service d'indexation separe
 * (IndexationService) : un ArticleAide est deja du texte pret a vectoriser
 * (redige a la main, etape 8.5), il n'y a ni extraction PDF ni decoupage en
 * morceaux a orchestrer. Indexation et recherche tiennent donc dans ce seul
 * service.
 *
 * Reutilise EmbeddingClient tel quel (dette assumee, decision de cadrage du
 * lot 8) : les embeddings restent en Java, seule la generation de reponse
 * passe par le service Python (llm-service/).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ArticleAideService {

    private final ArticleAideRepository articleAideRepository;
    private final EmbeddingClient embeddingClient;

    /**
     * Vectorise tous les articles pas encore VECTORISE (EN_ATTENTE ou
     * ECHEC). Appelee au demarrage par DataInitializer, juste apres le seed
     * du corpus. Un echec sur un article (quota depasse, cle invalide,
     * panne reseau) n'interrompt pas les suivants : il est marque ECHEC et
     * sera rejoue au prochain demarrage, plutot que de faire echouer tout
     * le seed pour un seul article.
     */
    @Transactional
    public void indexerEnAttente() {
        List<ArticleAide> aTraiter = articleAideRepository
                .findByStatutIndexationNot(StatutIndexationArticle.VECTORISE);

        if (aTraiter.isEmpty()) {
            return;
        }
        if (!embeddingClient.estConfigure()) {
            log.warn("Cle API Gemini absente : {} article(s) d'aide restent non vectorises.",
                    aTraiter.size());
            return;
        }

        int succes = 0;
        for (ArticleAide article : aTraiter) {
            try {
                article.setVecteur(embeddingClient.vectoriser(article.getContenu()));
                article.setStatutIndexation(StatutIndexationArticle.VECTORISE);
                succes++;
            } catch (Exception e) {
                log.error("Echec de vectorisation de l'article d'aide {} (\"{}\") : {}",
                        article.getId(), article.getTitre(), e.toString());
                article.setStatutIndexation(StatutIndexationArticle.ECHEC);
            }
        }
        articleAideRepository.saveAll(aTraiter);
        log.info("Corpus assistant : {}/{} article(s) d'aide vectorises.", succes, aTraiter.size());
    }

    /**
     * Recherche vectorielle des k passages les plus pertinents pour une
     * question (etape 8.6). Appelee par RagService a l'etape 8.7, qui
     * construira le prompt (contexte + role + garde-fous) a partir du
     * resultat.
     *
     * La question est vectorisee avec le meme EmbeddingClient que le
     * corpus : les deux doivent vivre dans le meme espace vectoriel (meme
     * prefixe de tache), condition deja garantie par EmbeddingClient
     * lui-meme (lot 6).
     */
    @Transactional(readOnly = true)
    public List<ArticleAideRepository.ArticleProche> rechercherPassagesPertinents(
            String question, int k) {

        float[] vecteurQuestion = embeddingClient.vectoriser(question);
        return articleAideRepository.chercherPlusProches(formaterVecteur(vecteurQuestion), k);
    }

    /** Serialise un float[] au format litteral attendu par pgvector : [0.1,0.2,...]. */
    private String formaterVecteur(float[] vecteur) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (float valeur : vecteur) {
            joiner.add(Float.toString(valeur));
        }
        return joiner.toString();
    }
}
