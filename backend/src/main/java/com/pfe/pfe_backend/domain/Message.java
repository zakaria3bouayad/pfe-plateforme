package com.pfe.pfe_backend.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Message de la messagerie d'un projet (Lot 5, bloc A). Fil de discussion
 * unique par projet, ouvert au chef d'equipe et a l'encadrant (meme
 * granularite que les commentaires du lot 4).
 */
@Entity
@Table(
        name = "message",
        indexes = {
                @Index(name = "idx_message_projet", columnList = "projet_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 2000)
    @Column(name = "contenu", nullable = false, length = 2000)
    private String contenu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "auteur_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_message_auteur"))
    private Utilisateur auteur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "projet_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_message_projet"))
    private Projet projet;

    @Column(name = "date_envoi", nullable = false, updatable = false)
    private LocalDateTime dateEnvoi;

    @PrePersist
    protected void onCreate() {
        this.dateEnvoi = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
