package com.pfe.pfe_backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Document verse sur un projet (Lot 4, bloc A). Peut etre rattache
 * directement au projet (document general) ou a un jalon precis
 * (livrable soumis pour une etape).
 *
 * Versionnement automatique : un nouvel upload portant le meme nom pour le
 * meme couple (projet, jalon) cree une nouvelle ligne avec un numero de
 * version incremente, la ligne precedente etant conservee telle quelle
 * (historique complet). La suppression est toujours logique (supprime=true) :
 * aucune ligne n'est jamais effacee, et l'objet MinIO correspondant n'est
 * retire qu'au moment de cette suppression logique.
 */
@Entity
@Table(
        name = "document",
        uniqueConstraints = @UniqueConstraint(name = "uk_document_chemin_minio", columnNames = "chemin_minio"),
        indexes = {
                @Index(name = "idx_document_projet", columnList = "projet_id"),
                @Index(name = "idx_document_etape", columnList = "etape_id"),
                @Index(name = "idx_document_projet_nom_etape", columnList = "projet_id,nom,etape_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom original du fichier tel que fourni par l'uploadeur (sert aussi de cle de versionnement). */
    @NotBlank
    @Size(max = 255)
    @Column(name = "nom", nullable = false, length = 255)
    private String nom;

    /** Type MIME du fichier (ex. application/pdf). */
    @NotBlank
    @Size(max = 150)
    @Column(name = "type", nullable = false, length = 150)
    private String type;

    /** Taille du fichier en octets. */
    @NotNull
    @Min(0)
    @Column(name = "taille", nullable = false)
    private Long taille;

    /** Chemin (cle objet) du fichier dans le bucket MinIO. Unique : chaque version a son propre objet. */
    @NotBlank
    @Size(max = 500)
    @Column(name = "chemin_minio", nullable = false, length = 500)
    private String cheminMinio;

    /** Numero de version, incremente automatiquement par nom au sein d'un meme (projet, jalon). */
    @Min(1)
    @Column(name = "version", nullable = false)
    private int version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "projet_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_document_projet"))
    private Projet projet;

    /** Jalon associe, facultatif : absent pour un document general du projet. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "etape_id",
            foreignKey = @ForeignKey(name = "fk_document_etape"))
    private Etape etape;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "uploadeur_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_document_uploadeur"))
    private Utilisateur uploadeur;

    @Column(name = "date_upload", nullable = false, updatable = false)
    private LocalDateTime dateUpload;

    /** Suppression logique : aucune ligne n'est jamais effacee physiquement (coherent avec EF-03). */
    @Column(name = "supprime", nullable = false)
    private boolean supprime;

    @Column(name = "date_suppression")
    private LocalDateTime dateSuppression;

    @PrePersist
    protected void onCreate() {
        this.dateUpload = LocalDateTime.now();
        if (this.version <= 0) {
            this.version = 1;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Document other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
