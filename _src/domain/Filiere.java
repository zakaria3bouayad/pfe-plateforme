package com.pfe.pfe_backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Filiere academique (EF-07). Referentiel gere par l'administrateur.
 */
@Entity
@Table(
        name = "filiere",
        uniqueConstraints = @UniqueConstraint(name = "uk_filiere_code", columnNames = "code")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Filiere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 10)
    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @NotBlank
    @Size(max = 100)
    @Column(name = "libelle", nullable = false, length = 100)
    private String libelle;

    @Size(max = 100)
    @Column(name = "departement", length = 100)
    private String departement;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Filiere other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
