package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.RapportSimilarite;
import com.pfe.pfe_backend.domain.enums.NiveauSimilarite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RapportSimilariteRepository extends JpaRepository<RapportSimilarite, Long> {

    /** Historique complet des analyses d'un document, de la plus recente a la plus ancienne. */
    List<RapportSimilarite> findByDocumentIdOrderByDateAnalyseDesc(Long documentId);

    /** Derniere analyse en date d'un document : celle qu'on affiche par defaut. */
    Optional<RapportSimilarite> findFirstByDocumentIdOrderByDateAnalyseDesc(Long documentId);

    /**
     * Derniers rapports de tous les documents d'un projet donne, pour la vue
     * encadrant (etape 6.7).
     */
    @Query("""
            SELECT r FROM RapportSimilarite r
            JOIN r.document d
            WHERE d.projet.id = :projetId
            ORDER BY r.dateAnalyse DESC
            """)
    List<RapportSimilarite> findParProjet(@Param("projetId") Long projetId);

    /**
     * Rapports des documents encadres par un superviseur donne, filtres sur
     * un niveau. Sert a lister les cas a examiner sans parcourir projet par
     * projet.
     */
    @Query("""
            SELECT r FROM RapportSimilarite r
            JOIN r.document d
            WHERE d.projet.encadrant.id = :encadrantId
              AND r.niveau IN :niveaux
            ORDER BY r.scoreMax DESC
            """)
    List<RapportSimilarite> findParEncadrantEtNiveaux(
            @Param("encadrantId") Long encadrantId,
            @Param("niveaux") List<NiveauSimilarite> niveaux);

    long countByNiveau(NiveauSimilarite niveau);
}
