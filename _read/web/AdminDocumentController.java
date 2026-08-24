package com.pfe.pfe_backend.web;

import com.pfe.pfe_backend.dto.ArchiveDocumentRequest;
import com.pfe.pfe_backend.dto.DocumentDto;
import com.pfe.pfe_backend.dto.IndexationDocumentDto;
import com.pfe.pfe_backend.service.DocumentService;
import com.pfe.pfe_backend.service.IndexationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Administration du corpus de rapports de reference (Lot 6, etape 6.1).
 *
 * Separe du DocumentController, qui porte les actions etudiant et encadrant
 * sur les documents d'un projet. Le prefixe /api/admin est deja verrouille
 * sur le role ADMINISTRATEUR par SecurityConfig ; le @PreAuthorize present
 * sur chaque methode est une seconde barriere, alignee sur le style de
 * UtilisateurController.
 */
@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
public class AdminDocumentController {

    private final DocumentService documentService;
    private final IndexationService indexationService;

    /** Corpus de comparaison du detecteur de similarite : uniquement les documents marques. */
    @GetMapping("/archives")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<List<DocumentDto>> listerArchives() {
        return ResponseEntity.ok(documentService.listerArchives());
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<DocumentDto> changerArchive(
            @PathVariable Long id,
            @Valid @RequestBody ArchiveDocumentRequest requete,
            Authentication authentication) {
        return ResponseEntity.ok(
                documentService.changerArchive(authentication.getName(), id, requete.archive()));
    }

    // ------------------------------------------- indexation (Lot 6, etape 6.2)

    /**
     * Relance l'extraction du texte d'un document. Le marquage comme archive
     * l'a deja declenchee ; cet endpoint sert a rattraper un echec sans avoir
     * a demarquer puis remarquer le document.
     */
    @PostMapping("/{id}/indexer")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<IndexationDocumentDto> indexer(@PathVariable Long id) {
        return ResponseEntity.ok(indexationService.indexer(id));
    }

    /** Etat de l'indexation : statut, volume de texte, cause d'un eventuel echec. */
    @GetMapping("/{id}/indexation")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<IndexationDocumentDto> consulterIndexation(@PathVariable Long id) {
        return ResponseEntity.ok(indexationService.consulter(id));
    }
}
