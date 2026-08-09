package com.pfe.pfe_backend.domain;

import com.pfe.pfe_backend.domain.enums.StatutEtape;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Jalon (etape) d'un projet de PFE (Lot 3, bloc A). Cree par l'encadrant,
 * soumis par le chef d'equipe, valide par l'encadrant. Le statut EN_RETARD
 * est recalcule automatiquement lorsque la date d'echeance est depassee
 * sans soumission (EF-26).
 */
@Entity
@Table(name = "etape")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Etape {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(name = "titre", nullable = false, length = 150)
    private String titre;

    @NotBlank
    @Size(max = 2000)
    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @NotNull
    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    /** Ordre d'affichage / de sequence des jalons au sein d'un projet. */
    @Min(1)
    @Column(name = "ordre", nullable = false)
    private int ordre;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutEtape statut;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "projet_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_etape_projet"))
    private Projet projet;

    /** Lien vers le livrable soumis par le chef d'equipe (pas de stockage MinIO avant le lot 4). */
    @Size(max = 500)
    @Column(name = "lien_livrable", length = 500)
    private String lienLivrable;

    @Size(max = 1000)
    @Column(name = "commentaire_soumission", length = 1000)
    private String commentaireSoumission;

    @Column(name = "date_soumission")
    private LocalDateTime dateSoumission;

    /** Retour de l'encadrant lors de la validation du jalon. */
    @Size(max = 1000)
    @Column(name = "commentaire_validation", length = 1000)
    private String commentaireValidation;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        this.dateModification = this.dateCreation;
        if (this.statut == null) {
            this.statut = StatutEtape.A_FAIRE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Etape other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
