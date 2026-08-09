package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.Etape;
import com.pfe.pfe_backend.domain.enums.StatutEtape;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EtapeRepository extends JpaRepository<Etape, Long> {

    List<Etape> findByProjetIdOrderByOrdreAsc(Long projetId);

    List<Etape> findByProjetIdAndStatutOrderByOrdreAsc(Long projetId, StatutEtape statut);

    /** Jalons non soldes (ni soumis, ni valides) dont l'echeance est depassee : pour le calcul EN_RETARD (EF-26). */
    List<Etape> findByStatutNotInAndDateEcheanceBefore(Collection<StatutEtape> statutsExclus, LocalDate date);

    long countByProjetIdAndStatut(Long projetId, StatutEtape statut);

    // ------------------------------------------------------------ stats (Lot 3, bloc B)

    long countByProjetId(Long projetId);

    /** Prochain jalon non encore valide, pour le tableau de bord etudiant. */
    Optional<Etape> findFirstByProjetIdAndStatutNotOrderByDateEcheanceAsc(Long projetId, StatutEtape statutExclu);

    /** Tous jalons des projets encadres par un encadrant donne. */
    long countByProjetEncadrantIdAndStatut(Long encadrantId, StatutEtape statut);

    long countByStatut(StatutEtape statut);
}
