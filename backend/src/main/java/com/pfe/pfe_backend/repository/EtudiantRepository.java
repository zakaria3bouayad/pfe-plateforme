package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.Etudiant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Acces specifique aux etudiants. Distinct de UtilisateurRepository :
 * ici toutes les methodes renvoient bien un Etudiant type, avec ses
 * champs propres (numero, filiere, promotion).
 */
@Repository
public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {

    Optional<Etudiant> findByNumeroEtudiant(String numeroEtudiant);

    boolean existsByNumeroEtudiant(String numeroEtudiant);

    Page<Etudiant> findByFiliereId(Long filiereId, Pageable pageable);

    Page<Etudiant> findByPromotionId(Long promotionId, Pageable pageable);
}
