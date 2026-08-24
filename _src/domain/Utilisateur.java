package com.pfe.pfe_backend.domain;

import com.pfe.pfe_backend.domain.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Classe mere abstraite de tous les comptes (figure 2 de la conception UML).
 *
 * Strategie d'heritage JOINED : une table "utilisateur" pour les champs communs,
 * plus une table par sous-classe reliee par la meme cle primaire. C'est la
 * strategie la plus normalisee : aucune colonne nulle inutile.
 */
@Entity
@Table(
        name = "utilisateur",
        uniqueConstraints = @UniqueConstraint(name = "uk_utilisateur_email", columnNames = "email"),
        indexes = {
                @Index(name = "idx_utilisateur_role", columnList = "role"),
                @Index(name = "idx_utilisateur_actif", columnList = "actif")
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
public abstract class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 60)
    @Column(name = "nom", nullable = false, length = 60)
    private String nom;

    @NotBlank
    @Size(max = 60)
    @Column(name = "prenom", nullable = false, length = 60)
    private String prenom;

    @NotBlank
    @Email
    @Size(max = 120)
    @Column(name = "email", nullable = false, length = 120)
    private String email;

    /** Hash BCrypt (60 caracteres). Jamais le mot de passe en clair (EF-02). */
    @NotBlank
    @Column(name = "mot_de_passe", nullable = false, length = 72)
    private String motDePasse;

    @Size(max = 20)
    @Column(name = "telephone", length = 20)
    private String telephone;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    /** Desactivation logique : on ne supprime jamais un compte (EF-03). */
    @Column(name = "actif", nullable = false)
    private boolean actif = true;

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

    @Transient
    public String getNomComplet() {
        return prenom + " " + nom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Utilisateur other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
