package com.pfe.pfe_backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Etudiant realisant un PFE. Rattache a exactement une filiere et une promotion (EF-07).
 */
@Entity
@Table(
        name = "etudiant",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_etudiant_numero", columnNames = "numero_etudiant")
)
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
public class Etudiant extends Utilisateur {

    @NotBlank
    @Size(max = 20)
    @Column(name = "numero_etudiant", nullable = false, length = 20)
    private String numeroEtudiant;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "filiere_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_etudiant_filiere"))
    private Filiere filiere;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "promotion_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_etudiant_promotion"))
    private Promotion promotion;
}
