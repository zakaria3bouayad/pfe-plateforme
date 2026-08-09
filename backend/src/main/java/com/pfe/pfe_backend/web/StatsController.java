package com.pfe.pfe_backend.web;

import com.pfe.pfe_backend.dto.StatsAdminDto;
import com.pfe.pfe_backend.dto.StatsEncadrantDto;
import com.pfe.pfe_backend.dto.StatsEtudiantDto;
import com.pfe.pfe_backend.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tableaux de bord agreges par role (Lot 3, bloc B).
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/etudiant")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<StatsEtudiantDto> statsEtudiant(Authentication authentication) {
        return ResponseEntity.ok(statsService.statsEtudiant(authentication.getName()));
    }

    @GetMapping("/encadrant")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<StatsEncadrantDto> statsEncadrant(Authentication authentication) {
        return ResponseEntity.ok(statsService.statsEncadrant(authentication.getName()));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<StatsAdminDto> statsAdmin() {
        return ResponseEntity.ok(statsService.statsAdmin());
    }
}
