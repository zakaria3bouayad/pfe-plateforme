package com.pfe.pfe_backend.web;

import com.pfe.pfe_backend.dto.DecisionSujetRequest;
import com.pfe.pfe_backend.dto.EtapeDto;
import com.pfe.pfe_backend.dto.EtapeRequest;
import com.pfe.pfe_backend.dto.SoumissionRequest;
import com.pfe.pfe_backend.service.EtapeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion des jalons d'un projet (Lot 3, bloc A).
 *
 * Creation et gestion (CRUD) reservees a l'encadrant du projet. Soumission
 * reservee au chef d'equipe. Validation reservee a l'encadrant. Consultation
 * ouverte a tout compte authentifie.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EtapeController {

    private final EtapeService etapeService;

    @GetMapping("/projets/{projetId}/etapes")
    public ResponseEntity<List<EtapeDto>> lister(@PathVariable Long projetId) {
        return ResponseEntity.ok(etapeService.listerParProjet(projetId));
    }

    @PostMapping("/projets/{projetId}/etapes")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<EtapeDto> creer(
            @PathVariable Long projetId,
            Authentication authentication,
            @Valid @RequestBody EtapeRequest requete) {
        EtapeDto etape = etapeService.creer(authentication.getName(), projetId, requete);
        return ResponseEntity.status(HttpStatus.CREATED).body(etape);
    }

    @GetMapping("/etapes/{id}")
    public ResponseEntity<EtapeDto> trouver(@PathVariable Long id) {
        return ResponseEntity.ok(etapeService.trouver(id));
    }

    @PutMapping("/etapes/{id}")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<EtapeDto> modifier(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody EtapeRequest requete) {
        return ResponseEntity.ok(etapeService.modifier(id, authentication.getName(), requete));
    }

    @DeleteMapping("/etapes/{id}")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id, Authentication authentication) {
        etapeService.supprimer(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/etapes/{id}/soumettre")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<EtapeDto> soumettre(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody SoumissionRequest requete) {
        return ResponseEntity.ok(etapeService.soumettre(authentication.getName(), id, requete));
    }

    @PatchMapping("/etapes/{id}/valider")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<EtapeDto> valider(
            @PathVariable Long id,
            Authentication authentication,
            @RequestBody(required = false) DecisionSujetRequest requete) {
        String commentaire = requete != null ? requete.commentaire() : null;
        return ResponseEntity.ok(etapeService.valider(authentication.getName(), id, commentaire));
    }
}
