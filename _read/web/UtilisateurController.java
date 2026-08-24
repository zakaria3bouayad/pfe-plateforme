package com.pfe.pfe_backend.web;

import com.pfe.pfe_backend.domain.enums.Role;
import com.pfe.pfe_backend.dto.StatutActifRequest;
import com.pfe.pfe_backend.dto.UtilisateurDto;
import com.pfe.pfe_backend.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion des comptes utilisateurs par l'administrateur (EF-03).
 *
 * Pas de creation ni de suppression ici : la creation passe par
 * /api/auth/register, et un compte n'est jamais supprime (desactivation
 * logique uniquement).
 */
@RestController
@RequestMapping("/api/admin/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<List<UtilisateurDto>> lister(@RequestParam(required = false) Role role) {
        return ResponseEntity.ok(utilisateurService.lister(role));
    }

    @PatchMapping("/{id}/actif")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<UtilisateurDto> changerStatutActif(
            @PathVariable Long id,
            @RequestBody StatutActifRequest requete,
            Authentication authentication) {
        return ResponseEntity.ok(
                utilisateurService.changerStatutActif(id, requete.actif(), authentication.getName()));
    }
}
