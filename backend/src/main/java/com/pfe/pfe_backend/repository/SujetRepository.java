package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.Sujet;
import com.pfe.pfe_backend.domain.enums.StatutSujet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SujetRepository extends JpaRepository<Sujet, Long> {

    List<Sujet> findAllByOrderByDatePropositionDesc();

    List<Sujet> findByStatutOrderByDatePropositionDesc(StatutSujet statut);

    List<Sujet> findByEncadrantIdOrderByDatePropositionDesc(Long encadrantId);

    /** Doublon : meme encadrant, meme titre, sujet pas deja rejete. */
    boolean existsByEncadrantIdAndTitreIgnoreCaseAndStatutNot(
            Long encadrantId, String titre, StatutSujet statutExclu);

    /** Quota : nombre de sujets actifs (ni rejetes, ni clotures) d'un encadrant. */
    long countByEncadrantIdAndStatutNotIn(Long encadrantId, Collection<StatutSujet> statutsExclus);

    // ------------------------------------------------------------ stats (Lot 3, bloc B)

    long countByEncadrantIdAndStatutIn(Long encadrantId, Collection<StatutSujet> statuts);

    long countByStatutIn(Collection<StatutSujet> statuts);
}
