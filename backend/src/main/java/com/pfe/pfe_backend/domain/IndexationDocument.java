package com.pfe.pfe_backend.domain;

import com.pfe.pfe_backend.domain.enums.StatutIndexation;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Texte extrait d'un document, en entree du detecteur de similarite
 * (Lot 6, etape 6.2).
 *
 * Table separee de document, et non colonnes supplementaires : le texte d'un
 * rapport pese facilement plusieurs centaines de kilo-octets, et JPA charge
 * les colonnes basiques en eager. Les stocker sur Document ferait rapatrier
 * l'integralite des rapports a chaque GET /api/projets/{id}/documents. La
 * relation etant LAZY, le texte n'est lu que lorsqu'on le demande
 * explicitement. C'est aussi cette table qui portera le vecteur d'embedding
 * a l'etape 6.3.
 *
 * Une seule ligne par document (contrainte d'unicite sur document_id) :
 * une nouvelle extraction ecrase la precedente plutot que d'empiler un
 * historique, dont on n'a aucun usage ici.
 */
@Entity
@Table(
        name = "indexation_document",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_indexation_document", columnNames = "document_id"),
        indexes = @Index(name = "idx_indexation_statut", columnList = "statut")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "document_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_indexation_document"))
    private Document document;

    /**
     * Texte brut du PDF, espaces normalises. Type TEXT explicite plutot que
     * @Lob : sur PostgreSQL, @Lob sur un String bascule sur les large objects
     * (table pg_largeobject, acces par OID), inutilement complique ici.
     * Null si le statut n'est pas EXTRAIT.
     */
    @Column(name = "texte", columnDefinition = "TEXT")
    private String texte;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private StatutIndexation statut;

    /** Longueur du texte retenu, apres normalisation et troncature eventuelle. */
    @Min(0)
    @Column(name = "nb_caracteres", nullable = false)
    private int nbCaracteres;

    /** Nombre de pages du PDF, null si le fichier n'a pas pu etre ouvert. */
    @Column(name = "nb_pages")
    private Integer nbPages;

    /** Vrai si le texte a ete coupe a la limite de taille (le document reste exploitable, partiellement). */
    @Column(name = "tronque", nullable = false)
    private boolean tronque;

    /**
     * Nombre de morceaux vectorises (Lot 6, etape 6.3). Zero tant que la
     * vectorisation n'a pas abouti. Redondant avec un COUNT sur
     * morceau_document, mais evite cette requete a chaque affichage de la
     * liste des archives.
     */
    @Min(0)
    @Column(name = "nb_morceaux", nullable = false)
    private int nbMorceaux;

    /** Cause de l'echec, renseignee uniquement pour les statuts ECHEC et TYPE_NON_SUPPORTE. */
    @Size(max = 500)
    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "date_extraction", nullable = false)
    private LocalDateTime dateExtraction;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.dateExtraction = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IndexationDocument other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
