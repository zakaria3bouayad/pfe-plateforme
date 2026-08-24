package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.Ressource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RessourceRepository extends JpaRepository<Ressource, Long> {

    List<Ressource> findAllByOrderByDateCreationDesc();

    List<Ressource> findByCategorieIgnoreCaseOrderByDateCreationDesc(String categorie);
}
