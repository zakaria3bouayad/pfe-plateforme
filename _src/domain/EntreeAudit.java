package com.pfe.pfe_backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Ligne du journal d'audit (Lot 7, bloc B). Ecrite par AuditService via
 * l'annotation @Audite interceptee par un aspect AOP (etape 7.7), en
 * propagation REQUIRES_NEW : une entree doit survivre au rollback de
 * l'action auditee (ex. une validation de sujet qui echoue en base doit tout
 * de meme laisser une trace de la tentative), donc jamais dans la meme
 * transaction que cette action.
 *
 * acteur est un email en texte libre, pas une FK vers Utilisateur : les
 * echecs de connexion (etape 7.8) couvrent aussi une saisie d'email inconnu
 * du systeme, qui ne correspond a aucune ligne "utilisateur" et rendrait une
 * FK inutilisable pour ce cas precisement le plus interessant a tracer.
 * cible suit le meme principe que Notification.lien (etape 7.1) : une
 * reference textuelle libre (ex. "Sujet#42"), jamais une FK polymorphe.
 *
 * Ecrite une fois, jamais modifiee : pas de date de modification, pas de
 * suppression logique - un journal d'audit qui peut etre corrige perd sa
 * valeur probante.
 */
@Entity
@Table(
        name = "entree_audit",
        indexes = {
                @Index(name = "idx_entree_audit_acteur", columnList = "acteur"),
                @Index(name = "idx_entree_audit_action", columnList = "action"),
                @Index(name = "idx_entree_audit_horodatage", columnList = "horodatage")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntreeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Email de l'auteur de l'action, ou de la tentative si elle a echoue (ex. connexion echouee sur un email inconnu). */
    @NotBlank
    @Size(max = 120)
    @Column(name = "acteur", nullable = false, length = 120)
    private String acteur;

    /** Code de l'action auditee (ex. "CONNEXION_ECHOUEE", "SUJET_VALIDE"), porte par l'annotation @Audite au point d'appel. */
    @NotBlank
    @Size(max = 50)
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    /** Reference textuelle libre de l'objet touche (ex. "Sujet#42"), absente pour une action sans cible unique (ex. connexion). */
    @Size(max = 255)
    @Column(name = "cible", length = 255)
    private String cible;

    /** Contexte additionnel en texte libre (raison d'un echec, ancien/nouvel etat...), facultatif. */
    @Size(max = 1000)
    @Column(name = "detail", length = 1000)
    private String detail;

    @Column(name = "horodatage", nullable = false, updatable = false)
    private LocalDateTime horodatage;

    /** Adresse IP du client HTTP a l'origine de l'action. IPv6 compris (longueur max 45). */
    @Size(max = 45)
    @Column(name = "ip", length = 45)
    private String ip;

    @PrePersist
    protected void onCreate() {
        this.horodatage = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntreeAudit other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
