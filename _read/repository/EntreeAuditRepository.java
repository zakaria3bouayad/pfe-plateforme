package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.EntreeAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/** Acces au journal d'audit (etape 7.6). */
public interface EntreeAuditRepository extends JpaRepository<EntreeAudit, Long> {

    /**
     * Recherche paginee pour AuditController (etape 7.9). Chaque filtre est
     * facultatif (NULL = ignore) : un admin peut consulter le journal brut,
     * ou le restreindre par acteur (recherche partielle), action (code
     * exact) et/ou plage de dates.
     */
    @Query("""
            SELECT e FROM EntreeAudit e
            WHERE (:acteur IS NULL OR LOWER(e.acteur) LIKE LOWER(CONCAT('%', :acteur, '%')))
              AND (:action IS NULL OR e.action = :action)
              AND (:depuis IS NULL OR e.horodatage >= :depuis)
              AND (:jusqua IS NULL OR e.horodatage <= :jusqua)
            ORDER BY e.horodatage DESC
            """)
    Page<EntreeAudit> rechercher(
            @Param("acteur") String acteur,
            @Param("action") String action,
            @Param("depuis") LocalDateTime depuis,
            @Param("jusqua") LocalDateTime jusqua,
            Pageable pageable);
}
