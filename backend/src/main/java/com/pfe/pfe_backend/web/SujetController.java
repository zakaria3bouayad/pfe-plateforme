package com.pfe.pfe_backend.web;

import com.pfe.pfe_backend.domain.enums.StatutSujet;
import com.pfe.pfe_backend.dto.DecisionSujetRequest;
import com.pfe.pfe_backend.dto.SujetDto;
import com.pfe.pfe_backend.dto.SujetRequest;
import com.pfe.pfe_backend.service.SujetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Proposition et consultation des sujets de PFE (EF-08).
 *
 * Consultation ouverte a tout compte authentifie (etudiants et encadrants
 * doivent pouvoir parcourir les sujets). Ecriture reservee a l'encadrant
 * proprietaire, sauf pour l'administrateur qui peut intervenir sur tout sujet.
 */
@RestController
@RequestMapping("/api/sujets")
@RequiredArgsConstructor
public class SujetController {

    private final SujetService sujetService;

    @GetMapping
    public ResponseEntity<List<SujetDto>> lister(
            @RequestParam(required = false) StatutSujet statut) {
        return ResponseEntity.ok(sujetService.lister(statut));
    }

    /** Sujets proposes par l'encadrant connecte, quel que soit leur statut. */
    @GetMapping("/moi")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<List<SujetDto>> mesSujets(Authentication authentication) {
        return ResponseEntity.ok(sujetService.listerParEncadrant(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SujetDto> trouver(@PathVariable Long id) {
        return ResponseEntity.ok(sujetService.trouver(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<SujetDto> proposer(
            Authentication authentication, @Valid @RequestBody SujetRequest requete) {
        SujetDto sujet = sujetService.proposer(authentication.getName(), requete);
        return ResponseEntity.status(HttpStatus.CREATED).body(sujet);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ENCADRANT', 'ADMINISTRATEUR')")
    public ResponseEntity<SujetDto> modifier(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody SujetRequest requete) {
        boolean estAdmin = estAdmin(authentication);
        return ResponseEntity.ok(
                sujetService.modifier(id, authentication.getName(), estAdmin, requete));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ENCADRANT', 'ADMINISTRATEUR')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id, Authentication authentication) {
        boolean estAdmin = estAdmin(authentication);
        sujetService.supprimer(id, authentication.getName(), estAdmin);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------ validation (EF-09)

    /** L'administrateur commence l'examen d'un sujet propose. */
    @PatchMapping("/{id}/demarrer-validation")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<SujetDto> demarrerValidation(@PathVariable Long id) {
        return ResponseEntity.ok(sujetService.demarrerValidation(id));
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<SujetDto> valider(@PathVariable Long id) {
        return ResponseEntity.ok(sujetService.valider(id));
    }

    @PatchMapping("/{id}/rejeter")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<SujetDto> rejeter(
            @PathVariable Long id, @RequestBody DecisionSujetRequest requete) {
        return ResponseEntity.ok(sujetService.rejeter(id, requete.commentaire()));
    }

    @PatchMapping("/{id}/demander-correction")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<SujetDto> demanderCorrection(
            @PathVariable Long id, @RequestBody DecisionSujetRequest requete) {
        return ResponseEntity.ok(sujetService.demanderCorrection(id, requete.commentaire()));
    }

    private boolean estAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATEUR"));
    }
}
