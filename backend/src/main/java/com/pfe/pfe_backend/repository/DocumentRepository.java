package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /** Documents actifs (non supprimes) d'un projet, toutes versions et tous jalons confondus. */
    List<Document> findByProjetIdAndSupprimeFalseOrderByDateUploadDesc(Long projetId);

    /** Documents actifs rattaches a un jalon precis. */
    List<Document> findByEtapeIdAndSupprimeFalseOrderByDateUploadDesc(Long etapeId);

    /** Historique des versions (actives ou non) d'un document general de projet (sans jalon), du plus recent au plus ancien. */
    List<Document> findByProjetIdAndNomAndEtapeIsNullOrderByVersionDesc(Long projetId, String nom);

    /** Historique des versions (actives ou non) d'un document rattache a un jalon donne, du plus recent au plus ancien. */
    List<Document> findByProjetIdAndNomAndEtapeIdOrderByVersionDesc(Long projetId, String nom, Long etapeId);

    /** Derniere version active d'un document general de projet (sans jalon). */
    Optional<Document> findFirstByProjetIdAndNomAndEtapeIsNullAndSupprimeFalseOrderByVersionDesc(Long projetId, String nom);

    /** Derniere version active d'un document rattache a un jalon donne. */
    Optional<Document> findFirstByProjetIdAndNomAndEtapeIdAndSupprimeFalseOrderByVersionDesc(Long projetId, String nom, Long etapeId);

    Optional<Document> findByCheminMinio(String cheminMinio);

    long countByProjetIdAndSupprimeFalse(Long projetId);
}
