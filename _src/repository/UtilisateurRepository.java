package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.Utilisateur;
import com.pfe.pfe_backend.domain.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Acces aux comptes. Polymorphe : renvoie indifferemment un Etudiant,
 * un Superviseur ou un Administrateur grace a l'heritage JOINED.
 */
@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    /** Utilise par l'authentification (EF-01). */
    Optional<Utilisateur> findByEmail(String email);

    Optional<Utilisateur> findByEmailAndActifTrue(String email);

    /** Controle d'unicite avant creation de compte. */
    boolean existsByEmail(String email);

    Page<Utilisateur> findByRole(Role role, Pageable pageable);

    Page<Utilisateur> findByActifTrue(Pageable pageable);

    long countByRole(Role role);

    // ------------------------------------------------------------ gestion admin (EF-03)

    List<Utilisateur> findAllByOrderByNomAscPrenomAsc();

    List<Utilisateur> findByRoleOrderByNomAscPrenomAsc(Role role);
}
