package com.pfe.pfe_backend.web;

import com.pfe.pfe_backend.dto.RessourceDto;
import com.pfe.pfe_backend.dto.RessourceTelechargement;
import com.pfe.pfe_backend.service.RessourceService;
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
 * Bibliotheque de ressources partagees (Lot 5, bloc B).
 *
 * Consultation ouverte a tout compte authentifie. Creation reservee a
 * l'encadrant ou a l'administrateur (l'etudiant ne peut pas publier de
 * ressource). Edition et suppression reservees a l'auteur de la ressource
 * ou a un administrateur (verifie dans RessourceService).
 */
@RestController
@RequestMapping("/api/ressources")
@RequiredArgsConstructor
public class RessourceController {

    private final RessourceService ressourceService;

    @GetMapping
    public ResponseEntity<List<RessourceDto>> lister(
            @RequestParam(required = false) String categorie) {
        List<RessourceDto> ressources = categorie != null && !categorie.isBlank()
                ? ressourceService.listerParCategorie(categorie)
                : ressourceService.lister();
        return ResponseEntity.ok(ressources);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ENCADRANT', 'ADMINISTRATEUR')")
    public ResponseEntity<RessourceDto> creer(
            @RequestParam String titre,
            @RequestParam(required = false) String description,
            @RequestParam String categorie,
            @RequestParam(required = false) String lien,
            @RequestPart(required = false) MultipartFile fichier,
            Authentication authentication) {
        RessourceDto ressource = ressourceService.creer(
                authentication.getName(), titre, description, categorie, lien, fichier);
        return ResponseEntity.status(HttpStatus.CREATED).body(ressource);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ENCADRANT', 'ADMINISTRATEUR')")
    public ResponseEntity<RessourceDto> modifier(
            @PathVariable Long id,
            @RequestParam String titre,
            @RequestParam(required = false) String description,
            @RequestParam String categorie,
            @RequestParam(required = false) String lien,
            @RequestPart(required = false) MultipartFile fichier,
            Authentication authentication) {
        RessourceDto ressource = ressourceService.modifier(
                authentication.getName(), id, titre, description, categorie, lien, fichier);
        return ResponseEntity.ok(ressource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ENCADRANT', 'ADMINISTRATEUR')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id, Authentication authentication) {
        ressourceService.supprimer(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/telecharger")
    public ResponseEntity<InputStreamResource> telecharger(@PathVariable Long id) {
        RessourceTelechargement fichier = ressourceService.telecharger(id);

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
}
