package com.pfe.pfe_backend.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Rapprochement entre un passage du document analyse et un passage d'un
 * document archive (Lot 6, etape 6.4).
 *
 * Une ligne par document archive : seule la meilleure correspondance est
 * conservee pour chaque archive. Empiler toutes les paires de morceaux
 * noierait l'encadrant sous des dizaines de lignes redondantes, alors qu'un
 * seul passage suffit a decider s'il faut regarder de plus pres.
 *
 * Les deux extraits sont stockes en dur plutot que reconstruits a la
 * demande. C'est une duplication assumee : elle garantit que le rapport
 * reste consultable et verifiable meme si le document archive est plus tard
 * demarque, reindexe avec un decoupage different, ou supprime. Un rapport
 * dont on ne pourrait plus afficher les passages incrimines n'aurait aucune
 * valeur probante.
 */
@Entity
@Table(
        name = "correspondance_similarite",
        indexes = {
                @Index(name = "idx_correspondance_rapport", columnList = "rapport_id"),
                @Index(name = "idx_correspondance_archive", columnList = "document_archive_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorrespondanceSimilarite {

    /** Longueur maximale d'un extrait conserve, de quoi juger sans dupliquer le rapport entier. */
    public static final int LONGUEUR_EXTRAIT = 1500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "rapport_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_correspondance_rapport"))
    private RapportSimilarite rapport;

    /**
     * Document archive rapproche. Sans contrainte de non-suppression : le
     * rapport doit rester lisible meme si l'archive disparait ensuite, d'ou
     * la conservation du nom en clair ci-dessous.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "document_archive_id",
            foreignKey = @ForeignKey(name = "fk_correspondance_archive"))
    private Document documentArchive;

    /** Nom du document archive au moment de l'analyse, conserve independamment de la relation. */
    @Column(name = "document_archive_nom", length = 255)
    private String documentArchiveNom;

    /** Similarite cosinus dans [0,1] : 1 signifie des vecteurs identiques. */
    @Column(name = "score", nullable = false)
    private double score;

    /** Position du passage concerne dans le document analyse. */
    @Column(name = "ordre_morceau_analyse", nullable = false)
    private int ordreMorceauAnalyse;

    /** Position du passage concerne dans le document archive. */
    @Column(name = "ordre_morceau_archive", nullable = false)
    private int ordreMorceauArchive;

    @Column(name = "extrait_analyse", columnDefinition = "TEXT")
    private String extraitAnalyse;

    @Column(name = "extrait_archive", columnDefinition = "TEXT")
    private String extraitArchive;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CorrespondanceSimilarite other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
