package com.pfe.pfe_backend.web;

import com.pfe.pfe_backend.dto.EntreeAuditDto;
import com.pfe.pfe_backend.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Consultation du journal d'audit (Lot 7, etape 7.9).
 *
 * Reserve a l'administrateur : le journal peut contenir des tentatives
 * echouees (ex. connexion sur un email inconnu) qui n'ont pas a etre
 * visibles d'un etudiant ou d'un encadrant. Le prefixe /api/admin est deja
 * verrouille sur le role ADMINISTRATEUR par SecurityConfig ; le
 * @PreAuthorize present ici est une seconde barriere, alignee sur le style
 * de AdminDocumentController.
 *
 * Pagination obligatoire : jamais de liste brute renvoyee, le journal
 * pouvant croitre indefiniment. Chaque filtre est facultatif et se combine
 * avec les autres (voir EntreeAuditRepository.rechercher).
 */
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Page<EntreeAuditDto>> rechercher(
            @RequestParam(required = false) String acteur,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime depuis,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime jusqua,
            @PageableDefault(size = 20, sort = "horodatage", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditService.rechercher(acteur, action, depuis, jusqua, pageable));
    }
}
