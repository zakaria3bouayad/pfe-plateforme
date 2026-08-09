package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.Sujet;
import com.pfe.pfe_backend.domain.enums.StatutSujet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SujetRepository extends JpaRepository<Sujet, Long> {

    List<Sujet> findAllByOrderByDatePropositionDesc();

    List<Sujet> findByStatutOrderByDatePropositionDesc(StatutSujet statut);

    List<Sujet> findByEncadrantIdOrderByDatePropositionDesc(Long encadrantId);
}
