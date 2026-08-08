package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByLibelle(String libelle);

    boolean existsByLibelle(String libelle);

    List<Promotion> findAllByOrderByAnneeDesc();
}
