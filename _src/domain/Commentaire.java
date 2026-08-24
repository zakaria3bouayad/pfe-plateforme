package com.pfe.pfe_backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Commentaire libre depose sur un document ou un jalon (Lot 4, bloc B).
 * Rattache a l'un OU l'autre, jamais les deux ni aucun des deux : la regle
 * est verifiee par CommentaireService (4.7), pas par une contrainte SQL, pour
 * rester coherent avec le reste du schema (deja pilote par Hibernate/Flyway).
 */
@Entity
@Table(
        name = "commentaire",
        indexes = {
                @Index(name = "idx_commentaire_document", columnList = "document_id"),
                @Index(name = "idx_commentaire_etape", columnList = "etape_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commentaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 2000)
    @Column(name = "contenu", nullable = false, length = 2000)
    private String contenu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "auteur_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_commentaire_auteur"))
    private Utilisateur auteur;

    /** Document commente, si le commentaire porte sur un document. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "document_id",
            foreignKey = @ForeignKey(name = "fk_commentaire_document"))
    private Document document;

    /** Jalon commente, si le commentaire porte sur un jalon. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "etape_id",
            foreignKey = @ForeignKey(name = "fk_commentaire_etape"))
    private Etape etape;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Commentaire other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
