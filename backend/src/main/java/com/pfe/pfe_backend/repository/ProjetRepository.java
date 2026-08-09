package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.Projet;
import com.pfe.pfe_backend.domain.enums.StatutProjet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjetRepository extends JpaRepository<Projet, Long> {

    Optional<Projet> findBySujetId(Long sujetId);

    Optional<Projet> findByEquipeId(Long equipeId);

    List<Projet> findByEncadrantIdOrderByDateAffectationDesc(Long encadrantId);

    List<Projet> findAllByOrderByDateAffectationDesc();

    long countByEncadrantIdAndStatut(Long encadrantId, StatutProjet statut);
}
