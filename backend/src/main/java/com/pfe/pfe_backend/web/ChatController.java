package com.pfe.pfe_backend.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.pfe_backend.dto.EscaladeRequest;
import com.pfe.pfe_backend.dto.EscaladeResponse;
import com.pfe.pfe_backend.dto.MessageAssistantRequest;
import com.pfe.pfe_backend.service.AuditService;
import com.pfe.pfe_backend.service.EscaladeService;
import com.pfe.pfe_backend.service.RagCallback;
import com.pfe.pfe_backend.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final RagService ragService;
    private final EscaladeService escaladeService;
    private final AuditService auditService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final long TIMEOUT_SSE_MS = 130_000L;

    @PostMapping(value = "/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ETUDIANT')")
    public SseEmitter poserQuestion(Authentication authentication, @Valid @RequestBody MessageAssistantRequest requete) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_SSE_MS);
        String acteur = authentication.getName();
        Thread.ofVirtual().name("assistant-rag-" + acteur).start(
                () -> ragService.repondre(requete.question(), new EmetteurSse(emitter, acteur)));
        return emitter;
    }

    /**
     * Mise en relation avec l'encadrant (etape 8.9, diagramme de sequence
     * 6). Endpoint REST classique, sans flux : contrairement a
     * /messages, il n'y a rien a generer, seulement une resolution
     * etudiant -> equipe -> projet -> encadrant (EscaladeService) suivie
     * d'une entree d'audit, sur le meme modele que la journalisation de
     * /messages a l'etape 8.8.
     */
    @PostMapping("/escalade")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<EscaladeResponse> escalader(Authentication authentication,
                                                        @Valid @RequestBody EscaladeRequest requete) {
        String acteur = authentication.getName();
        EscaladeResponse reponse = escaladeService.escalader(acteur, requete.question());
        auditService.enregistrer(acteur, "ASSISTANT_ESCALADE_DEMANDEE", null,
                "encadrant=" + reponse.encadrantEmail());
        return ResponseEntity.ok(reponse);
    }

    private class EmetteurSse implements RagCallback {
        private final SseEmitter emitter;
        private final String acteur;

        EmetteurSse(SseEmitter emitter, String acteur) {
            this.emitter = emitter;
            this.acteur = acteur;
        }

        @Override
        public void surToken(String token) {
            envoyer("token", token);
        }

        @Override
        public void surFin(String questionAnonymisee, List<String> sources) {
            envoyer("sources", ecrireJson(sources));
            journaliser(questionAnonymisee, true, sources.size());
            emitter.complete();
        }

        @Override
        public void surReponseImpossible(String questionAnonymisee, String message) {
            envoyer("impossible", message);
            journaliser(questionAnonymisee, false, 0);
            emitter.complete();
        }

        private void envoyer(String evenement, String donnee) {
            try {
                emitter.send(SseEmitter.event().name(evenement).data(donnee, MediaType.TEXT_PLAIN));
            } catch (IOException e) {
                log.warn("Assistant : envoi SSE interrompu pour {} : {}", acteur, e.toString());
                emitter.completeWithError(e);
            }
        }

        private void journaliser(String questionAnonymisee, boolean contexteTrouve, int nbSources) {
            String detail = "question=\"" + questionAnonymisee + "\"; contexte=" + (contexteTrouve ? "trouve" : "absent")
                    + "; sources=" + nbSources + "; generation=llm-service";
            auditService.enregistrer(acteur, "ASSISTANT_QUESTION_POSEE", null, detail);
        }

        private String ecrireJson(List<String> valeurs) {
            try {
                return OBJECT_MAPPER.writeValueAsString(valeurs);
            } catch (Exception e) {
                return "[]";
            }
        }
    }
}
