package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Client HTTP vers l'API d'embeddings Google Gemini (Lot 6, etape 6.3).
 *
 * Choix du modele : gemini-embedding-2, retenu pour deux raisons.
 * D'abord sa fenetre de 8192 tokens, quatre fois celle de
 * gemini-embedding-001, qui divise d'autant le nombre de morceaux et donc
 * d'appels. Ensuite sa renormalisation automatique des vecteurs tronques :
 * a 768 dimensions, gemini-embedding-001 renvoie des vecteurs non
 * normalises, et calculer un cosinus dessus sans les renormaliser fausse
 * silencieusement les scores.
 *
 * Contrepartie assumee : ce modele agrege plusieurs entrees en un seul
 * vecteur au lieu d'en renvoyer un par entree. Le traitement par lot est
 * donc impossible, chaque morceau demande son propre appel. D'ou la
 * temporisation entre appels, qui evite de heurter le quota de requetes par
 * minute du palier gratuit.
 *
 * La cle n'apparait jamais dans les traces : une erreur d'authentification
 * est journalisee par son code HTTP, jamais avec l'URL ou l'en-tete complet.
 */
@Service
@Slf4j
public class EmbeddingClient {

    /**
     * gemini-embedding-2 n'accepte pas le champ task_type : la tache se
     * declare en prefixe du texte. Le meme prefixe doit imperativement etre
     * applique au corpus et au rapport compare, sans quoi les vecteurs ne
     * vivent pas dans le meme espace et les scores n'ont aucun sens.
     */
    private static final String PREFIXE_TACHE = "task: sentence similarity | query: ";

    @Value("${gemini.api-key:}")
    private String cleApi;

    @Value("${gemini.embeddings.url}")
    private String urlBase;

    @Value("${gemini.embeddings.modele}")
    private String modele;

    @Value("${gemini.embeddings.dimension:768}")
    private int dimension;

    /** Pause entre deux appels, en millisecondes, pour rester sous le quota du palier gratuit. */
    @Value("${gemini.embeddings.pause-ms:250}")
    private long pauseMs;

    @Value("${gemini.embeddings.timeout-secondes:60}")
    private long timeoutSecondes;

    private RestClient restClient;

    @PostConstruct
    void initialiser() {
        this.restClient = RestClient.builder()
                .baseUrl(urlBase)
                .requestFactory(fabriqueAvecTimeouts())
                .build();

        if (!estConfigure()) {
            log.warn("Aucune cle API Gemini configuree : la vectorisation des documents echouera. "
                    + "Renseigner gemini.api-key dans application-local.properties "
                    + "ou la variable d'environnement GEMINI_API_KEY.");
        }
    }

    /** Permet aux appelants de renvoyer une erreur explicite plutot que de tenter un appel voue a l'echec. */
    public boolean estConfigure() {
        return cleApi != null && !cleApi.isBlank();
    }

    public int getDimension() {
        return dimension;
    }

    /**
     * Calcule le vecteur d'un texte.
     *
     * @throws BusinessException si la cle est absente, si l'API refuse
     *                           l'appel, ou si la dimension renvoyee ne
     *                           correspond pas a celle attendue
     */
    public float[] vectoriser(String texte) {
        if (!estConfigure()) {
            throw new BusinessException(
                    "Cle API Gemini absente : impossible de calculer les embeddings",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        Map<String, Object> corps = Map.of(
                "model", "models/" + modele,
                "content", Map.of("parts", List.of(Map.of("text", PREFIXE_TACHE + texte))),
                "outputDimensionality", dimension);

        try {
            ReponseEmbedding reponse = restClient.post()
                    .uri("/{modele}:embedContent", modele)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", cleApi)
                    .body(corps)
                    .retrieve()
                    .body(ReponseEmbedding.class);

            return extraireVecteur(reponse);

        } catch (RestClientResponseException e) {
            throw traduireErreurHttp(e);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Appel a l'API d'embeddings impossible : {}", e.toString());
            throw new BusinessException(
                    "L'API d'embeddings est injoignable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Vectorise une liste de morceaux, un appel par morceau, en respectant
     * la temporisation. L'ordre de la liste renvoyee suit celui de l'entree.
     */
    public List<float[]> vectoriserTout(List<String> morceaux) {
        return morceaux.stream().map(morceau -> {
            float[] vecteur = vectoriser(morceau);
            temporiser();
            return vecteur;
        }).toList();
    }

    // ------------------------------------------------------------ prive

    /**
     * Reponse de l'API, champs non utilises ignores. Publics : Jackson doit
     * pouvoir instancier ces records par reflexion.
     */
    public record ReponseEmbedding(Embedding embedding) {
        public record Embedding(List<Float> values) {}
    }

    private float[] extraireVecteur(ReponseEmbedding reponse) {
        if (reponse == null || reponse.embedding() == null || reponse.embedding().values() == null) {
            throw new BusinessException(
                    "Reponse inattendue de l'API d'embeddings", HttpStatus.BAD_GATEWAY);
        }
        List<Float> valeurs = reponse.embedding().values();

        // Une dimension inattendue signifie que la configuration a change
        // sans reindexation : mieux vaut echouer franchement que d'ecrire en
        // base des vecteurs incomparables entre eux.
        if (valeurs.size() != dimension) {
            throw new BusinessException(
                    "Dimension inattendue : " + valeurs.size() + " au lieu de " + dimension,
                    HttpStatus.BAD_GATEWAY);
        }

        float[] vecteur = new float[valeurs.size()];
        for (int i = 0; i < valeurs.size(); i++) {
            vecteur[i] = valeurs.get(i);
        }
        return vecteur;
    }

    /** Traduit les codes de l'API en messages exploitables, sans jamais divulguer la cle. */
    private BusinessException traduireErreurHttp(RestClientResponseException e) {
        int code = e.getStatusCode().value();
        log.error("L'API d'embeddings a repondu {}", code);

        return switch (code) {
            case 400 -> new BusinessException(
                    "Requete refusee par l'API d'embeddings (texte probablement trop long)",
                    HttpStatus.BAD_GATEWAY);
            case 401, 403 -> new BusinessException(
                    "Cle API Gemini invalide ou sans droit sur ce modele",
                    HttpStatus.SERVICE_UNAVAILABLE);
            case 429 -> new BusinessException(
                    "Quota de l'API d'embeddings depasse : reessayer plus tard",
                    HttpStatus.TOO_MANY_REQUESTS);
            default -> new BusinessException(
                    "L'API d'embeddings a repondu " + code, HttpStatus.BAD_GATEWAY);
        };
    }

    private void temporiser() {
        if (pauseMs <= 0) {
            return;
        }
        try {
            Thread.sleep(pauseMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(
                    "Vectorisation interrompue", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * Timeouts explicites : sans eux, un appel bloque immobiliserait le
     * thread de la requete HTTP entrante jusqu'a la fin des temps.
     */
    private ClientHttpRequestFactory fabriqueAvecTimeouts() {
        SimpleClientHttpRequestFactory fabrique = new SimpleClientHttpRequestFactory();
        fabrique.setConnectTimeout(Duration.ofSeconds(10));
        fabrique.setReadTimeout(Duration.ofSeconds(timeoutSecondes));
        return fabrique;
    }
}
