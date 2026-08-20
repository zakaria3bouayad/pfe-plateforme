package com.pfe.pfe_backend.dto;

import com.pfe.pfe_backend.domain.IndexationDocument;
import com.pfe.pfe_backend.domain.enums.StatutIndexation;

import java.time.LocalDateTime;

/**
 * Etat de l'indexation d'un document (Lot 6, etape 6.2).
 *
 * Ne transporte jamais le texte complet, seulement un apercu : renvoyer un
 * rapport entier en JSON n'a aucun usage cote interface et exposerait le
 * contenu integral d'un document a qui interroge l'API.
 */
public record IndexationDocumentDto(
        Long documentId,
        String documentNom,
        StatutIndexation statut,
        int nbCaracteres,
        Integer nbPages,
        boolean tronque,
        int nbMorceaux,
        String message,
        String apercu,
        LocalDateTime dateExtraction
) {

    /** Longueur de l'extrait renvoye, suffisante pour verifier a l'oeil que l'extraction a fonctionne. */
    private static final int LONGUEUR_APERCU = 500;

    public static IndexationDocumentDto from(IndexationDocument i) {
        String texte = i.getTexte();
        String apercu = texte == null ? null
                : texte.length() > LONGUEUR_APERCU ? texte.substring(0, LONGUEUR_APERCU) + "..." : texte;

        return new IndexationDocumentDto(
                i.getDocument().getId(),
                i.getDocument().getNom(),
                i.getStatut(),
                i.getNbCaracteres(),
                i.getNbPages(),
                i.isTronque(),
                i.getNbMorceaux(),
                i.getMessage(),
                apercu,
                i.getDateExtraction()
        );
    }
}
