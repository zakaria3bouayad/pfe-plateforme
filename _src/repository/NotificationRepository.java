package com.pfe.pfe_backend.repository;

import com.pfe.pfe_backend.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Liste complete d'un destinataire, plus recentes en premier (etape 7.2). */
    List<Notification> findByDestinataireIdOrderByDateCreationDesc(Long destinataireId);

    /** Notifications non lues d'un destinataire, pour le marquage groupe (etape 7.2). */
    List<Notification> findByDestinataireIdAndLueFalseOrderByDateCreationDesc(Long destinataireId);

    /** Alimente le compteur de la cloche (etape 7.2/7.10). */
    long countByDestinataireIdAndLueFalse(Long destinataireId);
}
