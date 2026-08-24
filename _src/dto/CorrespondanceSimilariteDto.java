package com.pfe.pfe_backend.dto;

import com.pfe.pfe_backend.domain.CorrespondanceSimilarite;

/**
 * Un rapprochement entre le document analyse et un document archive
 * (Lot 6, etape 6.4).
 *
 * Le score est arrondi a quatre decimales : au-dela, on afficherait un bruit
 * de calcul en virgule flottante qui donnerait une fausse impression de
 * precision.
 */
public record CorrespondanceSimilariteDto(
        Long documentArchiveId,
        String documentArchiveNom,
        double score,
        int pourcentage,
        int ordreMorceauAnalyse,
        int ordreMorceauArchive,
        String extraitAnalyse,
        String extraitArchive
) {
    public static CorrespondanceSimilariteDto from(CorrespondanceSimilarite c) {
        return new CorrespondanceSimilariteDto(
                c.getDocumentArchive() != null ? c.getDocumentArchive().getId() : null,
                c.getDocumentArchiveNom(),
                Math.round(c.getScore() * 10000d) / 10000d,
                (int) Math.round(c.getScore() * 100),
                c.getOrdreMorceauAnalyse(),
                c.getOrdreMorceauArchive(),
                c.getExtraitAnalyse(),
                c.getExtraitArchive()
        );
    }
}
