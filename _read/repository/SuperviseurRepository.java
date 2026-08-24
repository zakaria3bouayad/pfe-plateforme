package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.Superviseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Acces specifique aux encadrants. Distinct de UtilisateurRepository :
 * ici toutes les methodes renvoient bien un Superviseur type, avec ses
 * champs propres (specialite, grade, quota).
 */
@Repository
public interface SuperviseurRepository extends JpaRepository<Superviseur, Long> {

    Optional<Superviseur> findByEmail(String email);
}
