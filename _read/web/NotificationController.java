package com.pfe.pfe_backend.web;

import com.pfe.pfe_backend.dto.NotificationDto;
import com.pfe.pfe_backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Notifications in-app (etape 7.5).
 *
 * Ouvert a tout compte authentifie, quel que soit son role (etudiant,
 * encadrant, administrateur - etape 7.11) : le destinataire vient toujours
 * du jeton (Authentication), jamais d'un identifiant d'URL, pour que chacun
 * n'accede jamais qu'aux siennes.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> lister(Authentication authentication) {
        return ResponseEntity.ok(notificationService.lister(authentication.getName()));
    }

    /** Alimente le badge de la cloche (etape 7.10). */
    @GetMapping("/compteur")
    public ResponseEntity<Long> compterNonLues(Authentication authentication) {
        return ResponseEntity.ok(notificationService.compterNonLues(authentication.getName()));
    }

    @PatchMapping("/{id}/lu")
    public ResponseEntity<NotificationDto> marquerLu(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(notificationService.marquerLu(id, authentication.getName()));
    }

    /** Marque tout comme lu, pour le bouton correspondant de la cloche (etape 7.10). */
    @PatchMapping("/lu")
    public ResponseEntity<Void> marquerToutLu(Authentication authentication) {
        notificationService.marquerToutLu(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
