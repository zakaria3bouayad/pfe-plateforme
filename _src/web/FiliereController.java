package com.pfe.pfe_backend.web;

import com.pfe.pfe_backend.dto.FiliereDto;
import com.pfe.pfe_backend.dto.FiliereRequest;
import com.pfe.pfe_backend.service.FiliereService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Referentiel des filieres (EF-07).
 *
 * La lecture est publique : le formulaire d'inscription en a besoin avant
 * meme qu'un compte existe. L'ecriture est reservee a l'administrateur.
 */
@RestController
@RequestMapping("/api/filieres")
@RequiredArgsConstructor
public class FiliereController {

    private final FiliereService filiereService;

    @GetMapping
    public ResponseEntity<List<FiliereDto>> lister() {
        return ResponseEntity.ok(filiereService.lister());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<FiliereDto> creer(@Valid @RequestBody FiliereRequest requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(filiereService.creer(requete));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<FiliereDto> modifier(
            @PathVariable Long id, @Valid @RequestBody FiliereRequest requete) {
        return ResponseEntity.ok(filiereService.modifier(id, requete));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        filiereService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
