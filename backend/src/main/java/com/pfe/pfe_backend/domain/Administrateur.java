package com.pfe.pfe_backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Administrateur de la plateforme. Gere le referentiel et valide les sujets.
 */
@Entity
@Table(name = "administrateur")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
public class Administrateur extends Utilisateur {

    @Size(max = 30)
    @Column(name = "niveau_acces", length = 30)
    private String niveauAcces;
}
