package com.pfe.pfe_backend.web;

import com.pfe.pfe_backend.dto.PromotionDto;
import com.pfe.pfe_backend.dto.PromotionRequest;
import com.pfe.pfe_backend.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Referentiel des promotions (EF-07).
 *
 * La lecture est publique : le formulaire d'inscription en a besoin avant
 * meme qu'un compte existe. L'ecriture est reservee a l'administrateur.
 */
@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public ResponseEntity<List<PromotionDto>> lister() {
        return ResponseEntity.ok(promotionService.lister());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<PromotionDto> creer(@Valid @RequestBody PromotionRequest requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.creer(requete));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<PromotionDto> modifier(
            @PathVariable Long id, @Valid @RequestBody PromotionRequest requete) {
        return ResponseEntity.ok(promotionService.modifier(id, requete));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        promotionService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
