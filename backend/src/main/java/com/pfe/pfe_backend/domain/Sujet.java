package com.pfe.pfe_backend.domain;

import com.pfe.pfe_backend.domain.enums.StatutSujet;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Sujet de PFE propose par un encadrant (EF-08). Cycle de vie decrit par
 * StatutSujet, valide par l'administrateur (EF-09).
 */
@Entity
@Table(name = "sujet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sujet {

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

    @Size(max = 200)
    @Column(name = "mots_cles", length = 200)
    private String motsCles;

    @Min(1)
    @Column(name = "capacite_max", nullable = false)
    private int capaciteMax;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutSujet statut;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "encadrant_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sujet_encadrant"))
    private Superviseur encadrant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "filiere_id",
            foreignKey = @ForeignKey(name = "fk_sujet_filiere"))
    private Filiere filiere;

    /** Retour de l'administrateur lors d'un rejet ou d'une demande de correction (EF-09). */
    @Size(max = 1000)
    @Column(name = "commentaire_validation", length = 1000)
    private String commentaireValidation;

    @Column(name = "date_proposition", nullable = false, updatable = false)
    private LocalDateTime dateProposition;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() {
        this.dateProposition = LocalDateTime.now();
        this.dateModification = this.dateProposition;
        if (this.statut == null) {
            this.statut = StatutSujet.PROPOSE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sujet other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
