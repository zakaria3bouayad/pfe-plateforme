package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.Equipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipeRepository extends JpaRepository<Equipe, Long> {

    List<Equipe> findAllByOrderByDateCreationDesc();

    Optional<Equipe> findByChefId(Long chefId);

    Optional<Equipe> findByCodeInvitation(String codeInvitation);

    boolean existsByCodeInvitation(String codeInvitation);
}
