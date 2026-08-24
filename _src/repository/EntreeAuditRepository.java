package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.EntreeAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/** Acces au journal d'audit (etape 7.6). */
public interface EntreeAuditRepository extends JpaRepository<EntreeAudit, Long> {

    /**
     * Recherche paginee pour AuditController (etape 7.9). Chaque filtre est
     * facultatif (NULL = ignore) : un admin peut consulter le journal brut,
     * ou le restreindre par acteur (recherche partielle), action (code
     * exact) et/ou plage de dates.
     *
     * Requete NATIVE (et non JPQL/HQL) - historique du choix : la version
     * JPQL de cette requete, avec le filtre "facultatif" idiomatique
     * "(:param IS NULL OR ...)", s'est heurtee a trois echecs successifs
     * cote type-inference de Hibernate/PostgreSQL, tous causes par le meme
     * mecanisme (Hibernate doit deduire, pour chaque parametre nomme, UN
     * type JDBC valable pour la totalite de ses occurrences dans l'arbre
     * SQM, avant meme d'executer la requete) :
     *   1) :acteur, utilise seulement via "IS NULL" et un CONCAT/LOWER sans
     *      contexte fortement type, se voyait deduit en bytea -> "function
     *      lower(bytea) does not exist".
     *   2) Un CAST(:acteur AS string) corrige ce premier cas (le type se
     *      propage a toutes les occurrences), mais :depuis souffrait du
     *      meme probleme sans avoir de CAST : "could not determine data
     *      type of parameter $5" (PostgreSQL, faute d'indice, refuse meme
     *      de demarrer la requete).
     *   3) Un CAST(:depuis AS timestamp) equivalent, attendu pour reunir le
     *      meme correctif, a plutot fait resoudre :depuis en bytea de facon
     *      EXPLICITE (Hibernate 7.4.1 area) : "cannot cast type bytea to
     *      timestamp without time zone" au lieu de l'erreur precedente -
     *      preuve que le mecanisme de deduction JPQL, pour un parametre
     *      LocalDateTime seulement contraint par un CAST HQL, ne converge
     *      pas correctement dans cette version.
     * La requete est donc passee en SQL natif - mais premiere version
     * (sans CAST du tout, comptant sur le fait qu'un parametre natif est lie
     * d'apres le type Java de l'argument fourni) a REPRODUIT la meme erreur
     * "could not determine data type of parameter $5", cette fois sur
     * l'occurrence nue ":depuis IS NULL". Raison : en JPQL, Hibernate fixe
     * UN type par parametre NOMME et le reutilise partout (d'ou le CAST
     * unique qui suffisait pour :acteur) ; en SQL natif, c'est PostgreSQL
     * lui-meme qui type chaque "?" GENERE - un par occurrence textuelle,
     * meme si plusieurs proviennent du meme :param nomme - d'apres le seul
     * contexte local de CETTE occurrence. "e.horodatage >= ?" a un contexte
     * (comparaison a une colonne timestamp) qui suffit a Postgres pour
     * deduire le type localement ; "? IS NULL" n'en a aucun. D'ou la regle
     * retenue : CAST explicitement CHAQUE occurrence de chaque parametre
     * facultatif, y compris son "IS NULL" nu, sans compter sur une
     * propagation globale qui n'existe pas en natif.
     *
     * countQuery est fourni explicitement : Spring Data ne sait pas deriver
     * automatiquement un COUNT(*) a partir d'un SELECT natif.
     *
     * Le tri n'est pas fige ici : il vient entierement du Pageable, deja
     * par defaut sur horodatage decroissant (@PageableDefault,
     * AuditController) - Spring Data l'ajoute lui-meme a une requete
     * native sans ORDER BY existant.
     */
    @Query(value = """
            SELECT * FROM entree_audit e
            WHERE (CAST(:acteur AS varchar) IS NULL OR LOWER(e.acteur) LIKE LOWER(CONCAT('%', CAST(:acteur AS varchar), '%')))
              AND (CAST(:action AS varchar) IS NULL OR e.action = CAST(:action AS varchar))
              AND (CAST(:depuis AS timestamp) IS NULL OR e.horodatage >= CAST(:depuis AS timestamp))
              AND (CAST(:jusqua AS timestamp) IS NULL OR e.horodatage <= CAST(:jusqua AS timestamp))
            """,
            countQuery = """
            SELECT count(*) FROM entree_audit e
            WHERE (CAST(:acteur AS varchar) IS NULL OR LOWER(e.acteur) LIKE LOWER(CONCAT('%', CAST(:acteur AS varchar), '%')))
              AND (CAST(:action AS varchar) IS NULL OR e.action = CAST(:action AS varchar))
              AND (CAST(:depuis AS timestamp) IS NULL OR e.horodatage >= CAST(:depuis AS timestamp))
              AND (CAST(:jusqua AS timestamp) IS NULL OR e.horodatage <= CAST(:jusqua AS timestamp))
            """,
            nativeQuery = true)
    Page<EntreeAudit> rechercher(
            @Param("acteur") String acteur,
            @Param("action") String action,
            @Param("depuis") LocalDateTime depuis,
            @Param("jusqua") LocalDateTime jusqua,
            Pageable pageable);
}
