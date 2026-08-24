package com.pfe.pfe_backend.domain;

import com.pfe.pfe_backend.domain.enums.TypeNotification;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Notification in-app adressee a un utilisateur (etudiant, encadrant ou
 * administrateur). Creee par {@code NotificationService} en reaction a un
 * evenement metier (etape 7.4) ; {@code lien} porte la route frontend a
 * ouvrir au clic, sans FK polymorphe vers la ressource d'origine (sujet,
 * projet, document...) pour rester simple, dans le meme esprit que l'absence
 * de table de preferences a ce stade.
 */
@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Destinataire unique. Chacun n'accede qu'aux siennes (etape 7.5). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinataire_id", nullable = false)
    private Utilisateur destinataire;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 30, nullable = false)
    private TypeNotification type;

    @Column(name = "message", length = 500, nullable = false)
    private String message;

    /** Route frontend a ouvrir au clic (ex. "/etudiant/sujets/12"), optionnelle. */
    @Column(name = "lien", length = 255)
    private String lien;

    @Column(name = "lue", nullable = false)
    @Builder.Default
    private boolean lue = false;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    /** Renseignee au marquage lu (etape 7.2), null tant que non lue. */
    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;
}
