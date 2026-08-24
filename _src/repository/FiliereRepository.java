package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.Filiere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FiliereRepository extends JpaRepository<Filiere, Long> {

    Optional<Filiere> findByCode(String code);

    boolean existsByCode(String code);

    List<Filiere> findAllByOrderByLibelleAsc();
}
