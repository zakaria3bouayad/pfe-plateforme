package com.pfe.pfe_backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Promotion academique (EF-07). Exemple : annee = 2026, libelle = "2025-2026".
 */
@Entity
@Table(
        name = "promotion",
        uniqueConstraints = @UniqueConstraint(name = "uk_promotion_libelle", columnNames = "libelle")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(2000)
    @Column(name = "annee", nullable = false)
    private int annee;

    @NotBlank
    @Size(max = 60)
    @Column(name = "libelle", nullable = false, length = 60)
    private String libelle;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Promotion other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
