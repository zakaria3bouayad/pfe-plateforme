package com.pfe.pfe_backend.dto;

import com.pfe.pfe_backend.domain.Document;

import java.time.LocalDateTime;

/**
 * Representation d'un document renvoyee au frontend (Lot 4, bloc A).
 */
public record DocumentDto(
        Long id,
        String nom,
        String type,
        Long taille,
        int version,
        Long projetId,
        Long etapeId,
        Long uploadeurId,
        String uploadeurNom,
        LocalDateTime dateUpload
) {
    public static DocumentDto from(Document d) {
        return new DocumentDto(
                d.getId(),
                d.getNom(),
                d.getType(),
                d.getTaille(),
                d.getVersion(),
                d.getProjet().getId(),
                d.getEtape() != null ? d.getEtape().getId() : null,
                d.getUploadeur().getId(),
                d.getUploadeur().getNomComplet(),
                d.getDateUpload()
        );
    }
}
