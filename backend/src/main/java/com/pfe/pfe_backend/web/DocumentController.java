package com.pfe.pfe_backend.web;

import com.pfe.pfe_backend.dto.DocumentDto;
import com.pfe.pfe_backend.dto.DocumentTelechargement;
import com.pfe.pfe_backend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Gestion des documents verses sur un projet (Lot 4, bloc A).
 *
 * Upload reserve au chef d'equipe (l'appartenance exacte est verifiee dans
 * DocumentService). Suppression ouverte au chef d'equipe et a l'encadrant du
 * projet, arbitree par DocumentService. Consultation (liste, telechargement,
 * historique) ouverte a tout compte authentifie, comme le reste de l'API a ce
 * stade (meme principe que GET /api/projets, cf. dette technique lot 3).
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/projets/{projetId}/documents")
    public ResponseEntity<List<DocumentDto>> listerParProjet(@PathVariable Long projetId) {
        return ResponseEntity.ok(documentService.listerParProjet(projetId));
    }

    @GetMapping("/etapes/{etapeId}/documents")
    public ResponseEntity<List<DocumentDto>> listerParEtape(@PathVariable Long etapeId) {
        return ResponseEntity.ok(documentService.listerParEtape(etapeId));
    }

    @PostMapping(value = "/projets/{projetId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<DocumentDto> uploader(
            @PathVariable Long projetId,
            @RequestParam(required = false) Long etapeId,
            @RequestPart("fichier") MultipartFile fichier,
            Authentication authentication) {
        DocumentDto document = documentService.uploader(
                authentication.getName(), projetId, etapeId, fichier);
        return ResponseEntity.status(HttpStatus.CREATED).body(document);
    }

    @GetMapping("/documents/{id}/telecharger")
    public ResponseEntity<InputStreamResource> telecharger(@PathVariable Long id) {
        DocumentTelechargement fichier = documentService.telecharger(id);

        MediaType type;
        try {
            type = MediaType.parseMediaType(fichier.type());
        } catch (Exception e) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(fichier.nom(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(fichier.contenu()));
    }

    @GetMapping("/documents/{id}/versions")
    public ResponseEntity<List<DocumentDto>> historiqueVersions(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.historiqueVersions(id));
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENCADRANT')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id, Authentication authentication) {
        documentService.supprimer(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
