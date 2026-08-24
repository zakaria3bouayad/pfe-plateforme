package com.pfe.pfe_backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Ressource de la bibliotheque partagee (Lot 5, bloc B). Bibliotheque
 * globale, non rattachee a un projet : titre, description, categorie, et
 * son contenu qui peut etre un lien externe et/ou un fichier uploade
 * (MinIO, meme mecanisme que Document). Au moins l'un des deux (lien ou
 * fichier) doit etre fourni ; verifie au niveau du service, pas ici.
 */
@Entity
@Table(
        name = "ressource",
        indexes = {
                @Index(name = "idx_ressource_categorie", columnList = "categorie")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ressource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "titre", nullable = false, length = 255)
    private String titre;

    @Size(max = 2000)
    @Column(name = "description", length = 2000)
    private String description;

    @NotBlank
    @Size(max = 100)
    @Column(name = "categorie", nullable = false, length = 100)
    private String categorie;

    /** Lien externe vers la ressource, facultatif si un fichier est fourni. */
    @Size(max = 500)
    @Column(name = "lien", length = 500)
    private String lien;

    /** Nom original du fichier uploade, facultatif si un lien est fourni. */
    @Size(max = 255)
    @Column(name = "fichier_nom", length = 255)
    private String fichierNom;

    /** Type MIME du fichier uploade. */
    @Size(max = 150)
    @Column(name = "fichier_type", length = 150)
    private String fichierType;

    /** Taille du fichier uploade, en octets. */
    @Min(0)
    @Column(name = "fichier_taille")
    private Long fichierTaille;

    /** Chemin (cle objet) du fichier dans le bucket MinIO. */
    @Size(max = 500)
    @Column(name = "fichier_chemin_minio", length = 500)
    private String fichierCheminMinio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "auteur_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ressource_auteur"))
    private Utilisateur auteur;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ressource other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
