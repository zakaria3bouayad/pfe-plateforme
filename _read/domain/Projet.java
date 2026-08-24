package com.pfe.pfe_backend.domain;

import com.pfe.pfe_backend.domain.enums.StatutProjet;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Projet de PFE : affectation d'un sujet valide a une equipe, avec son
 * encadrant (EF-12). Un sujet et une equipe ne peuvent chacun etre lies
 * qu'a un seul projet a la fois.
 */
@Entity
@Table(
        name = "projet",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_projet_sujet", columnNames = "sujet_id"),
                @UniqueConstraint(name = "uk_projet_equipe", columnNames = "equipe_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Projet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "sujet_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_projet_sujet"))
    private Sujet sujet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "equipe_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_projet_equipe"))
    private Equipe equipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "encadrant_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_projet_encadrant"))
    private Superviseur encadrant;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutProjet statut;

    @Column(name = "date_affectation", nullable = false, updatable = false)
    private LocalDateTime dateAffectation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() {
        this.dateAffectation = LocalDateTime.now();
        this.dateModification = this.dateAffectation;
        if (this.statut == null) {
            this.statut = StatutProjet.EN_COURS;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Projet other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
