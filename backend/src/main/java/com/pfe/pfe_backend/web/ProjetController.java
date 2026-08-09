package com.pfe.pfe_backend.web;

import com.pfe.pfe_backend.dto.ProjetDto;
import com.pfe.pfe_backend.dto.ProjetRequest;
import com.pfe.pfe_backend.service.ProjetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Affectation et consultation des projets de PFE (EF-12).
 */
@RestController
@RequestMapping("/api/projets")
@RequiredArgsConstructor
public class ProjetController {

    private final ProjetService projetService;

    @GetMapping
    public ResponseEntity<List<ProjetDto>> lister() {
        return ResponseEntity.ok(projetService.lister());
    }

    /** Projet de l'equipe de l'etudiant connecte. */
    @GetMapping("/moi")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<ProjetDto> monProjet(Authentication authentication) {
        return ResponseEntity.ok(projetService.trouverParEtudiant(authentication.getName()));
    }

    /** Projets encadres par l'encadrant connecte. */
    @GetMapping("/mes-projets")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<List<ProjetDto>> mesProjets(Authentication authentication) {
        return ResponseEntity.ok(projetService.listerParEncadrant(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjetDto> trouver(@PathVariable Long id) {
        return ResponseEntity.ok(projetService.trouver(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<ProjetDto> affecter(
            Authentication authentication, @Valid @RequestBody ProjetRequest requete) {
        ProjetDto projet = projetService.affecter(authentication.getName(), requete);
        return ResponseEntity.status(HttpStatus.CREATED).body(projet);
    }
}
