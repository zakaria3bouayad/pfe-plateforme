package com.pfe.pfe_backend.dto;

import com.pfe.pfe_backend.domain.enums.TypeNotification;

import java.time.LocalDateTime;

/**
 * Vue exposee au frontend (etape 7.2/7.5). Pas de destinataire ici : le
 * NotificationController ne renvoie jamais que les notifications de
 * l'utilisateur authentifie, l'information serait redondante.
 */
public record NotificationDto(
        Long id,
        TypeNotification type,
        String message,
        String lien,
        boolean lue,
        LocalDateTime dateCreation
) {
}
