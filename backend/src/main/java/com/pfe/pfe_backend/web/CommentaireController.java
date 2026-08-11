package com.pfe.pfe_backend.web;

import com.pfe.pfe_backend.dto.CommentaireDto;
import com.pfe.pfe_backend.dto.CommentaireRequest;
import com.pfe.pfe_backend.service.CommentaireService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Commentaires sur un document ou un jalon (Lot 4, bloc B).
 *
 * Consultation ouverte a tout compte authentifie. Depot reserve au chef
 * d'equipe et a l'encadrant du projet concerne (verifie dans le service).
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentaireController {

    private final CommentaireService commentaireService;

    @GetMapping("/documents/{documentId}/commentaires")
    public ResponseEntity<List<CommentaireDto>> listerParDocument(@PathVariable Long documentId) {
        return ResponseEntity.ok(commentaireService.listerParDocument(documentId));
    }

    @PostMapping("/documents/{documentId}/commentaires")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENCADRANT')")
    public ResponseEntity<CommentaireDto> commenterDocument(
            @PathVariable Long documentId,
            Authentication authentication,
            @Valid @RequestBody CommentaireRequest requete) {
        CommentaireDto commentaire =
                commentaireService.commenterDocument(authentication.getName(), documentId, requete);
        return ResponseEntity.status(HttpStatus.CREATED).body(commentaire);
    }

    @GetMapping("/etapes/{etapeId}/commentaires")
    public ResponseEntity<List<CommentaireDto>> listerParEtape(@PathVariable Long etapeId) {
        return ResponseEntity.ok(commentaireService.listerParEtape(etapeId));
    }

    @PostMapping("/etapes/{etapeId}/commentaires")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENCADRANT')")
    public ResponseEntity<CommentaireDto> commenterEtape(
            @PathVariable Long etapeId,
            Authentication authentication,
            @Valid @RequestBody CommentaireRequest requete) {
        CommentaireDto commentaire =
                commentaireService.commenterEtape(authentication.getName(), etapeId, requete);
        return ResponseEntity.status(HttpStatus.CREATED).body(commentaire);
    }
}
