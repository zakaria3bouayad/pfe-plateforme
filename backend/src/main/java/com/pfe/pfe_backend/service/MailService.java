package com.pfe.pfe_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Envoi de mail (etape 7.3), sur spring-boot-starter-mail (dependance
 * presente depuis le lot 0, jamais utilisee jusqu'ici). SMTP de
 * developpement : MailHog (docker-compose, service "mailhog"), sans
 * authentification ni cle - interface web sur http://localhost:8025 pour la
 * demo, les mails n'en sortent jamais reellement.
 *
 * Ne decide pas quels evenements declenchent un mail : c'est aux services
 * metier appelants (etape 7.4) de restreindre les appels a la liste courte
 * decidee (sujet valide/rejete, jalon valide, similarite SUSPECT). Ce service
 * se contente d'envoyer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${mail.expediteur}")
    private String expediteur;

    /**
     * Asynchrone (etape 7.3) : ne bloque jamais la requete HTTP ni la
     * transaction appelante sur le SMTP. Un echec est journalise et avale -
     * la notification in-app (etape 7.2), deja creee de son cote, reste la
     * source de verite pour l'utilisateur.
     */
    @Async
    public void envoyer(String destinataire, String sujet, String contenu) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(expediteur);
        message.setTo(destinataire);
        message.setSubject(sujet);
        message.setText(contenu);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("Echec d'envoi de mail a {} (sujet : {}) : {}", destinataire, sujet, e.getMessage());
        }
    }
}
