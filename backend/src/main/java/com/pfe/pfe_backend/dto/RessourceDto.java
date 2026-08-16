package com.pfe.pfe_backend.dto;

import com.pfe.pfe_backend.domain.Ressource;

import java.time.LocalDateTime;

/**
 * Representation d'une ressource renvoyee au frontend (Lot 5, bloc B).
 */
public record RessourceDto(
        Long id,
        String titre,
        String description,
        String categorie,
        String lien,
        String fichierNom,
        String fichierType,
        Long fichierTaille,
        Long auteurId,
        String auteurNom,
        LocalDateTime dateCreation
) {
    public static RessourceDto from(Ressource r) {
        return new RessourceDto(
                r.getId(),
                r.getTitre(),
                r.getDescription(),
                r.getCategorie(),
                r.getLien(),
                r.getFichierNom(),
                r.getFichierType(),
                r.getFichierTaille(),
                r.getAuteur().getId(),
                r.getAuteur().getNomComplet(),
                r.getDateCreation()
        );
    }
}
