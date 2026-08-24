package com.pfe.pfe_backend.dto;

import com.pfe.pfe_backend.domain.Commentaire;

import java.time.LocalDateTime;

/**
 * Representation d'un commentaire renvoyee au frontend (Lot 4, bloc B).
 */
public record CommentaireDto(
        Long id,
        String contenu,
        Long auteurId,
        String auteurNom,
        Long documentId,
        Long etapeId,
        LocalDateTime dateCreation
) {
    public static CommentaireDto from(Commentaire c) {
        return new CommentaireDto(
                c.getId(),
                c.getContenu(),
                c.getAuteur().getId(),
                c.getAuteur().getNomComplet(),
                c.getDocument() != null ? c.getDocument().getId() : null,
                c.getEtape() != null ? c.getEtape().getId() : null,
                c.getDateCreation()
        );
    }
}
