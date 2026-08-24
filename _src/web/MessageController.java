package com.pfe.pfe_backend.web;

import com.pfe.pfe_backend.dto.MessageDto;
import com.pfe.pfe_backend.dto.MessageRequest;
import com.pfe.pfe_backend.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Messagerie d'un projet (Lot 5, bloc A).
 *
 * Acces (lecture et envoi) reserve au chef d'equipe et a l'encadrant du
 * projet concerne (verifie dans le service).
 */
@RestController
@RequestMapping("/api/projets/{projetId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENCADRANT')")
    public ResponseEntity<List<MessageDto>> lister(
            @PathVariable Long projetId, Authentication authentication) {
        return ResponseEntity.ok(messageService.listerParProjet(authentication.getName(), projetId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENCADRANT')")
    public ResponseEntity<MessageDto> envoyer(
            @PathVariable Long projetId,
            Authentication authentication,
            @Valid @RequestBody MessageRequest requete) {
        MessageDto message = messageService.envoyer(authentication.getName(), projetId, requete);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
}
