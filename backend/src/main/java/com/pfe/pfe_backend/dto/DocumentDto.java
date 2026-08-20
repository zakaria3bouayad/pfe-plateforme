package com.pfe.pfe_backend.dto;

import com.pfe.pfe_backend.domain.Document;

import java.time.LocalDateTime;

/**
 * Representation d'un document renvoyee au frontend (Lot 4, bloc A).
 *
 * Enrichie au lot 6 (etape 6.1) des trois champs d'archivage : le frontend
 * peut ainsi afficher le marqueur "rapport de reference" partout ou une liste
 * de documents est deja rendue, sans DTO supplementaire.
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
        LocalDateTime dateUpload,
        boolean archive,
        LocalDateTime dateArchivage,
        String archiveParNom
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
                d.getDateUpload(),
                d.isArchive(),
                d.getDateArchivage(),
                d.getArchivePar() != null ? d.getArchivePar().getNomComplet() : null
        );
    }
}
