package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.repository.ArticleAideRepository.ArticleProche;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestration de l'assistant conversationnel RAG (Lot 8, etape 8.7 ;
 * diagramme de sequence 6). Suit le meme enchainement que le diagramme :
 * anonymiser la question, rechercher les k=5 passages les plus proches,
 * decider si le contexte est suffisant, construire un prompt contraint
 * (garde-fous) et lancer la generation en streaming - ou repondre "je ne
 * sais pas" sans jamais appeler le modele si le contexte est insuffisant.
 *
 * Ne gere ni le transport SSE (RagCallback abstrait la sortie) ni
 * l'escalade elle-meme (etape 8.9) : ce service se limite a la decision et
 * a la generation. La journalisation (etape 8.8) est faite par l'appelant,
 * a partir de la question anonymisee que ce service lui repasse - jamais
 * ici, pour eviter de coupler RagService a la couche audit.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagService {

    private final ArticleAideService articleAideService;
    private final AnonymisationService anonymisationService;
    private final LlmGatewayClient llmGatewayClient;

    /** k de la recherche des plus proches voisins (diagramme de sequence 6 : k = 5). */
    @Value("${assistant.k-voisins:5}")
    private int kVoisins;

    /**
     * Score minimal (similarite cosinus, meilleur passage) pour considerer
     * qu'un contexte est exploitable. Valeur provisoire, non calibree faute
     * de jeu de questions de reference (contrairement aux seuils de
     * similarite.* du lot 6, calibres sur un jeu d'essai a l'etape 6.6) : a
     * ajuster lors des tests reels de l'assistant (etape 8.13).
     */
    @Value("${assistant.seuil-pertinence:0.75}")
    private double seuilPertinence;

    private static final String MESSAGE_SERVICE_INDISPONIBLE =
            "L'assistant est temporairement indisponible. N'hesitez pas a contacter "
                    + "directement votre encadrant en attendant.";

    private static final String MESSAGE_AUCUN_CONTEXTE =
            "Je ne sais pas repondre a cette question a partir de la documentation "
                    + "disponible. Je vous invite a contacter directement votre encadrant.";

    /**
     * Repond a une question deja recue par le controleur (etape 8.8), en
     * cedant chaque fragment de reponse a {@code callback} au fur et a
     * mesure de sa generation.
     *
     * La question est anonymisee en tout premier, avant meme la
     * verification de disponibilite : ainsi callback.surReponseImpossible
     * recoit toujours une question anonymisee exploitable pour la
     * journalisation (etape 8.8), quelle que soit la raison de l'echec.
     */
    public void repondre(String question, RagCallback callback) {
        if (question == null || question.isBlank()) {
            callback.surReponseImpossible("", MESSAGE_AUCUN_CONTEXTE);
            return;
        }

        // RGPD (ENF-15) : la question anonymisee est ce qui sera vectorise
        // (donc transmis a l'API d'embeddings), inclus dans le prompt envoye
        // au modele, ET journalise par l'appelant - jamais la question brute.
        String questionAnonymisee = anonymisationService.anonymiser(question);

        if (!llmGatewayClient.estDisponible()) {
            log.warn("Assistant : passerelle LLM injoignable, reponse impossible.");
            callback.surReponseImpossible(questionAnonymisee, MESSAGE_SERVICE_INDISPONIBLE);
            return;
        }

        List<ArticleProche> passages = articleAideService
                .rechercherPassagesPertinents(questionAnonymisee, kVoisins);

        // Journalise systematiquement le meilleur score, pertinent ou non :
        // seule donnee necessaire a la calibration du seuil a l'etape 8.13
        // (contrairement au cas "aucun contexte", qui ne loggait avant que
        // les echecs, sans jamais montrer les scores des questions qui
        // passaient le seuil - impossible de juger si celui-ci est bien
        // place sans voir les deux cotes de la coupure).
        Double meilleurScore = passages.isEmpty() ? null : passages.get(0).getScore();
        boolean pertinent = contextePertinent(passages);
        log.info("Assistant : question=\"{}\" ; meilleur score = {} ; seuil = {} ; retenu = {}",
                questionAnonymisee, meilleurScore, seuilPertinence, pertinent);

        if (!pertinent) {
            callback.surReponseImpossible(questionAnonymisee, MESSAGE_AUCUN_CONTEXTE);
            return;
        }

        String prompt = construirePrompt(questionAnonymisee, passages);
        List<String> sources = passages.stream()
                .map(ArticleProche::getTitre)
                .distinct()
                .toList();

        try {
            llmGatewayClient.completer(prompt, callback::surToken);
            callback.surFin(questionAnonymisee, sources);
        } catch (Exception e) {
            log.error("Assistant : echec de generation : {}", e.toString());
            callback.surReponseImpossible(questionAnonymisee, MESSAGE_SERVICE_INDISPONIBLE);
        }
    }

    /**
     * Porte alt/sinon du diagramme de sequence 6 : un contexte "pertinent"
     * exige qu'au moins un article soit vectorise ET que le meilleur score
     * franchisse le seuil. En dessous, la generation n'est meme pas
     * tentee - premiere ligne de defense contre les reponses inventees
     * (cible d'evaluation du cahier des charges : 0 reponse inventee).
     */
    private boolean contextePertinent(List<ArticleProche> passages) {
        if (passages.isEmpty()) {
            return false;
        }
        Double meilleurScore = passages.get(0).getScore();
        return meilleurScore != null && meilleurScore >= seuilPertinence;
    }

    /**
     * Prompt contraint (contexte + role + garde-fous, diagramme de sequence
     * 6). Deuxieme ligne de defense apres le seuil de pertinence ci-dessus :
     * le seuil peut laisser passer un contexte techniquement proche mais
     * insuffisant pour repondre precisement - le modele doit alors pouvoir
     * refuser plutot qu'inventer.
     */
    private String construirePrompt(String question, List<ArticleProche> passages) {
        StringBuilder contexte = new StringBuilder();
        for (ArticleProche passage : passages) {
            contexte.append("### ").append(passage.getTitre())
                    .append(" (").append(passage.getCategorie()).append(")\n")
                    .append(passage.getContenu())
                    .append("\n\n");
        }

        return """
                Tu es l'assistant de la plateforme de gestion des projets de fin d'etudes \
                (PFE). Tu aides les etudiants a utiliser la plateforme.

                Regles strictes :
                - Reponds UNIQUEMENT a partir des articles fournis ci-dessous, jamais a \
                partir de connaissances generales.
                - Si les articles fournis ne permettent pas de repondre precisement, dis \
                explicitement que tu ne sais pas et invite l'utilisateur a contacter son \
                encadrant. N'invente jamais de fonctionnalite ni de procedure.
                - Cite le ou les titres des articles sur lesquels ta reponse s'appuie.
                - Ne demande et ne repete jamais de donnee personnelle (email, numero \
                etudiant, telephone...).
                - Reponds en francais, de maniere concise et directe.

                Articles disponibles :
                %s
                Question de l'utilisateur : %s
                """.formatted(contexte, question);
    }
}
