package com.pfe.pfe_backend.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Fragment de texte d'un document et son vecteur d'embedding
 * (Lot 6, etape 6.3).
 *
 * Pourquoi decouper plutot que vectoriser le rapport entier :
 *
 *  1. Contrainte technique - gemini-embedding-2 accepte au plus 8192 tokens
 *     par appel et tronque silencieusement au-dela. Un rapport de PFE en
 *     represente plusieurs dizaines de milliers.
 *  2. Contrainte metier - un vecteur unique moyenne sur cent pages ne
 *     rapproche que des rapports de meme theme. Le plagiat reel porte sur
 *     un chapitre ou une section : c'est a cette echelle qu'il faut
 *     comparer pour le detecter.
 *
 * La dimension 768 est figee ici et dans la configuration de l'appel a
 * l'API : changer l'une sans l'autre casse le calcul de similarite, et
 * changer la dimension impose de reindexer tout le corpus.
 */
@Entity
@Table(
        name = "morceau_document",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_morceau_indexation_ordre",
                columnNames = {"indexation_id", "ordre"}),
        indexes = @Index(name = "idx_morceau_indexation", columnList = "indexation_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MorceauDocument {

    /** Dimension du vecteur. Doit rester alignee sur gemini.embeddings.dimension. */
    public static final int DIMENSION = 768;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "indexation_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_morceau_indexation"))
    private IndexationDocument indexation;

    /** Position du morceau dans le document, a partir de 0. Sert a situer un passage suspect. */
    @Column(name = "ordre", nullable = false)
    private int ordre;

    /**
     * Texte du morceau, conserve pour pouvoir montrer a l'encadrant le
     * passage exact a l'origine d'un rapprochement (etape 6.7). Sans lui,
     * un score de similarite ne serait pas verifiable a la main.
     */
    @Column(name = "texte", columnDefinition = "TEXT", nullable = false)
    private String texte;

    /**
     * Vecteur d'embedding, type pgvector. gemini-embedding-2 renvoie des
     * vecteurs deja normalises, y compris aux dimensions tronquees : la
     * similarite cosinus peut donc etre calculee directement, sans
     * renormalisation prealable.
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = DIMENSION)
    @Column(name = "vecteur", columnDefinition = "vector(768)")
    private float[] vecteur;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MorceauDocument other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
