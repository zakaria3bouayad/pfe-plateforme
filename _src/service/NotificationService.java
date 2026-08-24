package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.Notification;
import com.pfe.pfe_backend.domain.Utilisateur;
import com.pfe.pfe_backend.domain.enums.TypeNotification;
import com.pfe.pfe_backend.dto.NotificationDto;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.NotificationRepository;
import com.pfe.pfe_backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Notifications in-app (etape 7.2). Creation = persistance seule, synchrone :
 * le declenchement des mails restreints (etapes 7.3/7.4) reste a la charge
 * des services metier appelants, en plus de l'appel a creer() ici.
 *
 * Les methodes exposees au controleur (etape 7.5) prennent l'email de
 * l'utilisateur authentifie, comme dans le reste du codebase (SujetService,
 * MessageService...) - jamais un identifiant venu de l'URL, pour que chacun
 * n'accede jamais qu'a ses propres notifications.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;

    /** Appelee par les services metier lors des evenements branches a l'etape 7.4. */
    public Notification creer(Utilisateur destinataire, TypeNotification type, String message, String lien) {
        Notification notification = Notification.builder()
                .destinataire(destinataire)
                .type(type)
                .message(message)
                .lien(lien)
                .lue(false)
                .dateCreation(LocalDateTime.now())
                .build();
        return notificationRepository.save(notification);
    }

    /** Liste complete du destinataire authentifie, plus recentes en premier. */
    @Transactional(readOnly = true)
    public List<NotificationDto> lister(String email) {
        Long destinataireId = trouverUtilisateur(email).getId();
        return notificationRepository.findByDestinataireIdOrderByDateCreationDesc(destinataireId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /** Alimente le badge de la cloche (etape 7.10). */
    @Transactional(readOnly = true)
    public long compterNonLues(String email) {
        Long destinataireId = trouverUtilisateur(email).getId();
        return notificationRepository.countByDestinataireIdAndLueFalse(destinataireId);
    }

    /**
     * Marque une notification lue. L'email vient de l'utilisateur
     * authentifie (etape 7.5) : une notification qui n'est pas la sienne est
     * traitee comme introuvable, jamais comme interdite, pour ne rien
     * laisser deviner sur les notifications d'autrui.
     */
    public NotificationDto marquerLu(Long notificationId, String email) {
        Long destinataireId = trouverUtilisateur(email).getId();
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getDestinataire().getId().equals(destinataireId))
                .orElseThrow(() -> new BusinessException("Notification introuvable"));

        if (!notification.isLue()) {
            notification.setLue(true);
            notification.setDateLecture(LocalDateTime.now());
        }
        return toDto(notification);
    }

    /** Marque tout comme lu, pour le bouton correspondant de la cloche (etape 7.10). */
    public void marquerToutLu(String email) {
        Long destinataireId = trouverUtilisateur(email).getId();
        LocalDateTime maintenant = LocalDateTime.now();
        notificationRepository.findByDestinataireIdAndLueFalseOrderByDateCreationDesc(destinataireId)
                .forEach(n -> {
                    n.setLue(true);
                    n.setDateLecture(maintenant);
                });
    }

    private Utilisateur trouverUtilisateur(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.introuvable("Utilisateur introuvable"));
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getLien(),
                notification.isLue(),
                notification.getDateCreation()
        );
    }
}
