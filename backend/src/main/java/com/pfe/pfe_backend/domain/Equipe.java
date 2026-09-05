package com.pfe.pfe_backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Equipe d'etudiants constituee pour realiser un PFE (EF-11).
 *
 * Le rattachement des membres se fait via Etudiant.equipe (cote proprietaire
 * de la relation) : une equipe n'a pas de collection de membres ici, elle est
 * recalculee a la demande via EtudiantRepository.findByEquipeId, comme pour
 * Filiere/Promotion. L'affectation a un sujet est geree dans une etape
 * ulterieure du lot 2.
 */
@Entity
@Table(name = "equipe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Min(1)
    @Max(4)
    @Column(name = "taille_max", nullable = false)
    private int tailleMax;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "chef_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_equipe_chef"))
    private Etudiant chef;

    /**
     * Code d'invitation (hors plan, ajoute a la demande de Zakaria en cours
     * de lot 8) : remplace l'ancienne auto-inscription par liste ouverte -
     * seul un etudiant muni du code donne par le chef peut rejoindre.
     *
     * Volontairement PAS nullable = false ici : sous ddl-auto=update, une
     * colonne NOT NULL ajoutee par ALTER TABLE echoue des qu'une ligne
     * existe deja sans valeur (l'equipe de test creee avant ce changement,
     * par exemple). L'unicite et la non-nullite sont garanties par
     * EquipeService (generation a la creation, retro-generation au premier
     * chargement pour une equipe plus ancienne) ; la vraie contrainte NOT
     * NULL sera posee dans une migration Flyway a part.
     */
    @Column(name = "code_invitation", unique = true, length = 8)
    private String codeInvitation;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        this.dateModification = this.dateCreation;
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Equipe other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
