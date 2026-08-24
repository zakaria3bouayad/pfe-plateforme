package com.pfe.pfe_backend.web;

import com.pfe.pfe_backend.dto.RapportSimilariteDto;
import com.pfe.pfe_backend.service.SimilariteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Declenchement et consultation des rapports de similarite
 * (Lot 6, etape 6.5).
 *
 * Toutes les routes sont reservees aux roles ENCADRANT et ADMINISTRATEUR.
 * L'etudiant en est exclu, y compris pour ses propres documents, et ce
 * choix est assume : donner a l'auteur la possibilite de relancer
 * l'analyse et de lire son score lui permettrait de retoucher son texte
 * jusqu'a passer sous le seuil, retournant l'outil contre son objet meme.
 *
 * Le controle de role assure par @PreAuthorize ne suffit pas : il laisserait
 * un encadrant consulter les rapports des projets d'un collegue.
 * SimilariteService verifie donc en plus que le demandeur encadre bien le
 * projet concerne, l'administrateur seul echappant a cette restriction.
 *
 * Le declenchement est une operation lourde : il peut impliquer l'extraction
 * du PDF puis un appel a l'API d'embeddings par morceau. Compter jusqu'a
 * une a deux minutes sur un rapport volumineux jamais indexe.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SimilariteController {

    private final SimilariteService similariteService;

    // ------------------------------------------------------- declenchement

    /**
     * Lance une analyse du document contre le corpus archive et renvoie le
     * rapport produit.
     *
     * Renvoie 201 et non 200 : chaque appel cree un nouveau rapport plutot
     * que de mettre a jour le precedent. Relancer une analyse apres
     * enrichissement du corpus doit laisser une trace distincte, sans quoi
     * l'historique des verifications serait perdu.
     */
    @PostMapping("/documents/{documentId}/similarite")
    @PreAuthorize("hasAnyRole('ENCADRANT', 'ADMINISTRATEUR')")
    public ResponseEntity<RapportSimilariteDto> analyser(
            @PathVariable Long documentId,
            Authentication authentication) {
        RapportSimilariteDto rapport =
                similariteService.analyser(authentication.getName(), documentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(rapport);
    }

    // -------------------------------------------------------- consultation

    /** Dernier rapport en date d'un document, correspondances comprises. */
    @GetMapping("/documents/{documentId}/similarite")
    @PreAuthorize("hasAnyRole('ENCADRANT', 'ADMINISTRATEUR')")
    public ResponseEntity<RapportSimilariteDto> dernierRapport(
            @PathVariable Long documentId,
            Authentication authentication) {
        return ResponseEntity.ok(
                similariteService.dernierRapport(authentication.getName(), documentId));
    }

    /** Historique des analyses d'un document, en resume (sans les extraits). */
    @GetMapping("/documents/{documentId}/similarite/historique")
    @PreAuthorize("hasAnyRole('ENCADRANT', 'ADMINISTRATEUR')")
    public ResponseEntity<List<RapportSimilariteDto>> historique(
            @PathVariable Long documentId,
            Authentication authentication) {
        return ResponseEntity.ok(
                similariteService.historique(authentication.getName(), documentId));
    }

    /** Rapport precis par son identifiant, correspondances comprises. */
    @GetMapping("/similarites/{rapportId}")
    @PreAuthorize("hasAnyRole('ENCADRANT', 'ADMINISTRATEUR')")
    public ResponseEntity<RapportSimilariteDto> consulter(
            @PathVariable Long rapportId,
            Authentication authentication) {
        return ResponseEntity.ok(
                similariteService.consulter(authentication.getName(), rapportId));
    }

    /** Tous les rapports des documents d'un projet, en resume. */
    @GetMapping("/projets/{projetId}/similarites")
    @PreAuthorize("hasAnyRole('ENCADRANT', 'ADMINISTRATEUR')")
    public ResponseEntity<List<RapportSimilariteDto>> parProjet(
            @PathVariable Long projetId,
            Authentication authentication) {
        return ResponseEntity.ok(
                similariteService.parProjet(authentication.getName(), projetId));
    }

    /**
     * Cas a examiner par l'encadrant connecte : niveaux ATTENTION et
     * SUSPECT sur l'ensemble de ses projets, du score le plus fort au plus
     * faible. C'est le point d'entree de la vue encadrant (etape 6.7).
     */
    @GetMapping("/similarites/a-examiner")
    @PreAuthorize("hasRole('ENCADRANT')")
    public ResponseEntity<List<RapportSimilariteDto>> aExaminer(Authentication authentication) {
        return ResponseEntity.ok(
                similariteService.aExaminerPourEncadrant(authentication.getName()));
    }
}
