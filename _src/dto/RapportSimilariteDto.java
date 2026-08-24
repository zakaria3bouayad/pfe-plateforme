package com.pfe.pfe_backend.dto;

import com.pfe.pfe_backend.domain.RapportSimilarite;
import com.pfe.pfe_backend.domain.enums.NiveauSimilarite;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Rapport de similarite complet renvoye au frontend (Lot 6, etape 6.4).
 *
 * Porte deliberement le champ "avertissement" : ce rapport est un outil
 * d'aide a la lecture, pas un verdict. L'interface doit afficher cette
 * reserve, sans quoi un score eleve serait pris pour une preuve de plagiat
 * alors qu'il peut resulter d'une source commune legitimement citee par les
 * deux rapports.
 */
public record RapportSimilariteDto(
        Long id,
        Long documentId,
        String documentNom,
        Long projetId,
        double scoreMax,
        int pourcentageMax,
        NiveauSimilarite niveau,
        int nbDocumentsCompares,
        int nbMorceauxAnalyses,
        double seuilSuspect,
        double seuilAttention,
        String demandeParNom,
        LocalDateTime dateAnalyse,
        String avertissement,
        List<CorrespondanceSimilariteDto> correspondances
) {

    private static final String AVERTISSEMENT =
            "Ce rapport signale des proximites de sens entre passages, il ne constitue "
                    + "pas une preuve de plagiat. Une forte similarite peut resulter d'une "
                    + "source commune correctement citee, d'un vocabulaire technique partage "
                    + "ou d'une methode standard du domaine. Chaque passage signale doit etre "
                    + "verifie manuellement.";

    private static final String AVERTISSEMENT_CORPUS_VIDE =
            "Aucun document archive n'etait disponible pour la comparaison : ce rapport "
                    + "n'est pas concluant. Marquer des rapports de reference comme archives "
                    + "avant de relancer l'analyse.";

    public static RapportSimilariteDto from(RapportSimilarite r) {
        return new RapportSimilariteDto(
                r.getId(),
                r.getDocument().getId(),
                r.getDocument().getNom(),
                r.getDocument().getProjet().getId(),
                Math.round(r.getScoreMax() * 10000d) / 10000d,
                (int) Math.round(r.getScoreMax() * 100),
                r.getNiveau(),
                r.getNbDocumentsCompares(),
                r.getNbMorceauxAnalyses(),
                r.getSeuilSuspect(),
                r.getSeuilAttention(),
                r.getDemandePar() != null ? r.getDemandePar().getNomComplet() : null,
                r.getDateAnalyse(),
                r.getNbDocumentsCompares() == 0 ? AVERTISSEMENT_CORPUS_VIDE : AVERTISSEMENT,
                r.getCorrespondances().stream().map(CorrespondanceSimilariteDto::from).toList()
        );
    }

    /** Variante allegee pour les listes : sans les correspondances ni leurs extraits. */
    public static RapportSimilariteDto resume(RapportSimilarite r) {
        return new RapportSimilariteDto(
                r.getId(),
                r.getDocument().getId(),
                r.getDocument().getNom(),
                r.getDocument().getProjet().getId(),
                Math.round(r.getScoreMax() * 10000d) / 10000d,
                (int) Math.round(r.getScoreMax() * 100),
                r.getNiveau(),
                r.getNbDocumentsCompares(),
                r.getNbMorceauxAnalyses(),
                r.getSeuilSuspect(),
                r.getSeuilAttention(),
                r.getDemandePar() != null ? r.getDemandePar().getNomComplet() : null,
                r.getDateAnalyse(),
                r.getNbDocumentsCompares() == 0 ? AVERTISSEMENT_CORPUS_VIDE : AVERTISSEMENT,
                List.of()
        );
    }
}
