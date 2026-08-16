package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.Message;
import com.pfe.pfe_backend.domain.Projet;
import com.pfe.pfe_backend.domain.Utilisateur;
import com.pfe.pfe_backend.dto.MessageDto;
import com.pfe.pfe_backend.dto.MessageRequest;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.MessageRepository;
import com.pfe.pfe_backend.repository.ProjetRepository;
import com.pfe.pfe_backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Messagerie d'un projet (Lot 5, bloc A). Fil de discussion unique par
 * projet, en REST simple (pas de WebSocket malgre la dependance presente
 * dans le pom.xml).
 *
 * Meme granularite d'acces que les commentaires du lot 4 : reserve au chef
 * d'equipe et a l'encadrant du projet concerne.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ProjetRepository projetRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional(readOnly = true)
    public List<MessageDto> listerParProjet(String email, Long projetId) {
        Projet projet = trouverProjet(projetId);
        verifierParticipantDuProjet(projet, email);
        return messageRepository.findByProjetIdOrderByDateEnvoiAsc(projetId).stream()
                .map(MessageDto::from).toList();
    }

    @Transactional
    public MessageDto envoyer(String email, Long projetId, MessageRequest requete) {
        Projet projet = trouverProjet(projetId);
        verifierParticipantDuProjet(projet, email);

        Message message = Message.builder()
                .contenu(requete.contenu().trim())
                .auteur(trouverUtilisateur(email))
                .projet(projet)
                .build();

        return MessageDto.from(messageRepository.save(message));
    }

    // ------------------------------------------------------------ prive

    private Projet trouverProjet(Long projetId) {
        return projetRepository.findById(projetId)
                .orElseThrow(() -> BusinessException.introuvable("Projet introuvable"));
    }

    private Utilisateur trouverUtilisateur(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.introuvable("Utilisateur introuvable"));
    }

    private void verifierParticipantDuProjet(Projet projet, String email) {
        if (projet.getEncadrant().getEmail().equalsIgnoreCase(email)) {
            return;
        }
        if (projet.getEquipe().getChef().getEmail().equalsIgnoreCase(email)) {
            return;
        }
        throw BusinessException.interdit("Vous n'etes pas autorise a acceder a la messagerie de ce projet");
    }
}
