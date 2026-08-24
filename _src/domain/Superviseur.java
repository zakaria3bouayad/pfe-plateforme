package com.pfe.pfe_backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Encadrant pedagogique. Le quota limite le nombre de projets encadres (EF-10).
 */
@Entity
@Table(name = "superviseur")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
public class Superviseur extends Utilisateur {

    @Size(max = 100)
    @Column(name = "specialite", length = 100)
    private String specialite;

    @Size(max = 50)
    @Column(name = "grade", length = 50)
    private String grade;

    @Size(max = 100)
    @Column(name = "departement", length = 100)
    private String departement;

    @Min(1)
    @Column(name = "quota_projets", nullable = false)
    private int quotaProjets = 5;
}
