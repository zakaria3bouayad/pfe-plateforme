package com.pfe.pfe_backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Client vers le service Python de generation (passerelle LLM, lot 8,
 * etapes 8.1-8.2, module llm-service/). Distinct d'EmbeddingClient : celui-ci
 * reste en Java et appelle directement Gemini pour les embeddings (lot 6,
 * dette assumee), tandis que la generation de texte passe par ce service
 * isole (cahier des charges, section 8.4).
 *
 * Etape 8.3 : verification de disponibilite seule (degradation controlee,
 * ENF-20). Etape 8.7 : ajout de l'appel de generation en streaming
 * (POST /completion), desormais utilise par RagService.
 */
@Service
@Slf4j
public class LlmGatewayClient {

    /**
     * ObjectMapper prive, non injecte : ce module Spring Boot 4
     * (spring-boot-starter-webmvc) n'expose pas de bean ObjectMapper
     * pret a l'emploi comme le faisait spring-boot-starter-web en Spring
     * Boot 3 (constate au demarrage - UnsatisfiedDependencyException).
     * Un usage aussi restreint (lire deux champs JSON d'un evenement SSE)
     * n'a de toute facon pas besoin de la configuration Jackson globale de
     * l'application.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${llm.gateway.url}")
    private String urlBase;

    @Value("${llm.gateway.timeout-ms:2000}")
    private int timeoutMs;

    /**
     * Read-timeout dedie a l'appel de generation, bien plus long que celui
     * de la verification de disponibilite : une reponse en streaming peut
     * prendre plusieurs dizaines de secondes, contrairement a un simple
     * GET /health.
     */
    @Value("${llm.gateway.timeout-completion-secondes:120}")
    private long timeoutCompletionSecondes;

    private RestClient restClientDisponibilite;
    private RestClient restClientCompletion;

    @PostConstruct
    void initialiser() {
        this.restClientDisponibilite = RestClient.builder()
                .baseUrl(urlBase)
                .requestFactory(fabriqueAvecTimeouts(
                        Duration.ofMillis(timeoutMs), Duration.ofMillis(timeoutMs)))
                .build();

        this.restClientCompletion = RestClient.builder()
                .baseUrl(urlBase)
                .requestFactory(fabriqueAvecTimeouts(
                        Duration.ofMillis(timeoutMs), Duration.ofSeconds(timeoutCompletionSecondes)))
                .build();
    }

    /**
     * Verifie que le service Python repond, sans jamais lever d'exception :
     * une indisponibilite (service arrete, timeout, erreur reseau) est un
     * cas attendu, pas une erreur de programmation. Seuls les logs portent
     * le detail de la cause, pour eviter de faire fuiter des informations
     * d'infrastructure interne dans une reponse API.
     */
    public boolean estDisponible() {
        try {
            restClientDisponibilite.get()
                    .uri("/health")
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("Passerelle LLM injoignable ({}) : {}", urlBase, e.toString());
            return false;
        }
    }

    /**
     * Genere une reponse en streaming a partir d'un prompt deja construit
     * (RagService, etape 8.7) et cede chaque fragment de texte a
     * {@code surToken} au fur et a mesure de sa reception.
     *
     * Lit la reponse SSE du service Python ligne par ligne plutot que de
     * charger la reponse entiere en memoire : c'est la seule facon de
     * relayer les tokens au fil de l'eau plutot que d'attendre la fin
     * complete de la generation.
     *
     * Appel bloquant (Spring MVC, pas WebFlux) : a executer depuis un thread
     * dedie a la requete SSE en cours (ChatController, etape 8.8), jamais
     * depuis le thread de traitement HTTP principal.
     *
     * @throws BusinessException si la passerelle repond une erreur HTTP, si
     *                           le service Python signale un echec de
     *                           generation en cours de flux, ou si la
     *                           connexion est perdue
     */
    public void completer(String prompt, Consumer<String> surToken) {
        Map<String, Object> corps = Map.of("prompt", prompt);

        try {
            restClientCompletion.post()
                    .uri("/completion")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corps)
                    .exchange((requete, reponse) -> {
                        if (!reponse.getStatusCode().is2xxSuccessful()) {
                            throw new BusinessException(
                                    "La passerelle LLM a repondu " + reponse.getStatusCode().value(),
                                    HttpStatus.BAD_GATEWAY);
                        }
                        try (BufferedReader lecteur = new BufferedReader(
                                new InputStreamReader(reponse.getBody(), StandardCharsets.UTF_8))) {
                            String ligne;
                            while ((ligne = lecteur.readLine()) != null) {
                                traiterLigneSse(ligne, surToken);
                            }
                        }
                        return null;
                    });
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Appel de generation a la passerelle LLM impossible : {}", e.toString());
            throw new BusinessException(
                    "La passerelle LLM est injoignable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /** Une ligne SSE hors "data: ..." (commentaire, ligne vide) est ignoree, comme le veut le format. */
    private void traiterLigneSse(String ligne, Consumer<String> surToken) {
        if (!ligne.startsWith("data:")) {
            return;
        }
        String donnee = ligne.substring("data:".length()).trim();
        if (donnee.isEmpty() || donnee.equals("[DONE]")) {
            return;
        }

        JsonNode noeud;
        try {
            noeud = OBJECT_MAPPER.readTree(donnee);
        } catch (JsonProcessingException e) {
            log.warn("Fragment SSE illisible ignore : {}", donnee);
            return;
        }

        if (noeud.has("error")) {
            throw new BusinessException(
                    "La generation a echoue cote passerelle LLM : " + noeud.get("error").asText(),
                    HttpStatus.BAD_GATEWAY);
        }
        if (noeud.has("token")) {
            surToken.accept(noeud.get("token").asText());
        }
    }

    private ClientHttpRequestFactory fabriqueAvecTimeouts(Duration connexion, Duration lecture) {
        SimpleClientHttpRequestFactory fabrique = new SimpleClientHttpRequestFactory();
        fabrique.setConnectTimeout(connexion);
        fabrique.setReadTimeout(lecture);
        return fabrique;
    }
}
