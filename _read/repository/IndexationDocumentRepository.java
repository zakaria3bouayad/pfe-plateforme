package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.IndexationDocument;
import com.pfe.pfe_backend.domain.enums.StatutIndexation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndexationDocumentRepository extends JpaRepository<IndexationDocument, Long> {

    Optional<IndexationDocument> findByDocumentId(Long documentId);

    boolean existsByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);

    /**
     * Corpus reellement exploitable : indexations portant sur un document
     * toujours archive et non supprime. Appeler avec StatutIndexation.VECTORISE
     * pour le calcul de similarite (6.4) : un document seulement EXTRAIT n'a
     * pas de vecteurs et ne peut pas etre compare.
     */
    @Query("""
            SELECT i FROM IndexationDocument i
            JOIN i.document d
            WHERE i.statut = :statut
              AND d.archive = TRUE
              AND d.supprime = FALSE
            """)
    List<IndexationDocument> findCorpusArchive(@Param("statut") StatutIndexation statut);

    long countByStatut(StatutIndexation statut);
}
