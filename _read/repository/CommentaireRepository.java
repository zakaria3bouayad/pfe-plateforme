package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.Commentaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {

    List<Commentaire> findByDocumentIdOrderByDateCreationAsc(Long documentId);

    List<Commentaire> findByEtapeIdOrderByDateCreationAsc(Long etapeId);
}
