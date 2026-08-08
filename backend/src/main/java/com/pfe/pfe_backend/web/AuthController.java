package com.pfe.pfe_backend.web;

import com.pfe.pfe_backend.dto.*;
import com.pfe.pfe_backend.repository.UtilisateurRepository;
import com.pfe.pfe_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Points d'entree d'authentification (EF-01).
 *
 * Toutes ces routes sont publiques sauf /me, qui exige un jeton valide.
 * Le controleur ne contient aucune logique metier : il recoit, delegue, renvoie.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UtilisateurRepository utilisateurRepository;

    /** Creation d'un compte. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> inscrire(@Valid @RequestBody RegisterRequest requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.inscrire(requete));
    }

    /** Connexion : renvoie le couple de jetons. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> connecter(@Valid @RequestBody LoginRequest requete) {
        return ResponseEntity.ok(authService.connecter(requete));
    }

    /** Renouvellement de l'access token a partir du refresh token. */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> rafraichir(@Valid @RequestBody RefreshRequest requete) {
        return ResponseEntity.ok(authService.rafraichir(requete));
    }

    /**
     * Profil de l'utilisateur connecte.
     * Route protegee : sert a verifier qu'un jeton est bien pris en compte.
     */
    @GetMapping("/me")
    public ResponseEntity<UtilisateurDto> profil(Authentication authentication) {
        return utilisateurRepository.findByEmail(authentication.getName())
                .map(UtilisateurDto::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
