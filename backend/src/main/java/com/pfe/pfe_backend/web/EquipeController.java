package com.pfe.pfe_backend.web;

import com.pfe.pfe_backend.dto.AjoutMembreRequest;
import com.pfe.pfe_backend.dto.EquipeDto;
import com.pfe.pfe_backend.dto.EquipeRequest;
import com.pfe.pfe_backend.dto.RejoindreEquipeRequest;
import com.pfe.pfe_backend.service.EquipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Constitution et consultation des equipes d'etudiants (EF-11).
 *
 * Consultation ouverte a tout compte authentifie. Creation reservee a
 * l'etudiant chef d'equipe. Ajout d'un membre : par le chef (numero
 * etudiant) ou par le candidat lui-meme muni du code d'invitation
 * (rejoindreParCode, hors plan initial, remplace en cours de lot 8 l'ancienne
 * auto-inscription par liste ouverte) ; retrait reserve au chef. La
 * dissolution peut aussi etre faite par un administrateur.
 */
@RestController
@RequestMapping("/api/equipes")
@RequiredArgsConstructor
public class EquipeController {

    private final EquipeService equipeService;

    @GetMapping
    public ResponseEntity<List<EquipeDto>> lister() {
        return ResponseEntity.ok(equipeService.lister());
    }

    /** Equipe de l'etudiant connecte. */
    @GetMapping("/moi")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<EquipeDto> monEquipe(Authentication authentication) {
        return ResponseEntity.ok(equipeService.trouverParEtudiant(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipeDto> trouver(@PathVariable Long id) {
        return ResponseEntity.ok(equipeService.trouver(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<EquipeDto> creer(
            Authentication authentication, @Valid @RequestBody EquipeRequest requete) {
        EquipeDto equipe = equipeService.creer(authentication.getName(), requete);
        return ResponseEntity.status(HttpStatus.CREATED).body(equipe);
    }

    @PostMapping("/{id}/membres")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<EquipeDto> ajouterMembre(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody AjoutMembreRequest requete) {
        return ResponseEntity.ok(equipeService.ajouterMembre(id, authentication.getName(), requete));
    }

    /**
     * Adhesion sur code d'invitation (hors plan, remplace en cours de lot 8
     * l'ancienne auto-inscription par liste ouverte de toutes les equipes
     * rejoignables). Pas de {id} dans l'URL : le code identifie l'equipe.
     */
    @PostMapping("/rejoindre")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<EquipeDto> rejoindre(
            Authentication authentication, @Valid @RequestBody RejoindreEquipeRequest requete) {
        return ResponseEntity.ok(equipeService.rejoindreParCode(requete.code(), authentication.getName()));
    }

    /** Le membre connecte quitte l'equipe (le chef doit dissoudre plutot que quitter). */
    @DeleteMapping("/{id}/membres/moi")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<Void> quitter(@PathVariable Long id, Authentication authentication) {
        equipeService.quitter(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/membres/{etudiantId}")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<Void> retirerMembre(
            @PathVariable Long id, @PathVariable Long etudiantId, Authentication authentication) {
        equipeService.retirerMembre(id, authentication.getName(), etudiantId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ADMINISTRATEUR')")
    public ResponseEntity<Void> dissoudre(@PathVariable Long id, Authentication authentication) {
        boolean estAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATEUR"));
        equipeService.dissoudre(id, authentication.getName(), estAdmin);
        return ResponseEntity.noContent().build();
    }
}
