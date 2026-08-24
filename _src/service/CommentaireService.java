package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.Commentaire;
import com.pfe.pfe_backend.domain.Document;
import com.pfe.pfe_backend.domain.Etape;
import com.pfe.pfe_backend.domain.Projet;
import com.pfe.pfe_backend.domain.Utilisateur;
import com.pfe.pfe_backend.dto.CommentaireDto;
import com.pfe.pfe_backend.dto.CommentaireRequest;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.CommentaireRepository;
import com.pfe.pfe_backend.repository.DocumentRepository;
import com.pfe.pfe_backend.repository.EtapeRepository;
import com.pfe.pfe_backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Commentaires libres sur un document ou un jalon (Lot 4, bloc B).
 *
 * Ouvert en lecture a tout compte authentifie (meme principe que le reste
 * de l'API a ce stade). Depot reserve aux deux parties prenantes directes
 * du projet : le chef d'equipe et l'encadrant, meme granularite que les
 * autres verifications du domaine (cf. EtapeService).
 */
@Service
@RequiredArgsConstructor
public class CommentaireService {

    private final CommentaireRepository commentaireRepository;
    private final DocumentRepository documentRepository;
    private final EtapeRepository etapeRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional(readOnly = true)
    public List<CommentaireDto> listerParDocument(Long documentId) {
        return commentaireRepository.findByDocumentIdOrderByDateCreationAsc(documentId).stream()
                .map(CommentaireDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CommentaireDto> listerParEtape(Long etapeId) {
        return commentaireRepository.findByEtapeIdOrderByDateCreationAsc(etapeId).stream()
                .map(CommentaireDto::from).toList();
    }

    @Transactional
    public CommentaireDto commenterDocument(String email, Long documentId, CommentaireRequest requete) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> BusinessException.introuvable("Document introuvable"));
        if (document.isSupprime()) {
            throw BusinessException.introuvable("Document introuvable");
        }
        verifierParticipantDuProjet(document.getProjet(), email);

        Commentaire commentaire = Commentaire.builder()
                .contenu(requete.contenu().trim())
                .auteur(trouverUtilisateur(email))
                .document(document)
                .build();

        return CommentaireDto.from(commentaireRepository.save(commentaire));
    }

    @Transactional
    public CommentaireDto commenterEtape(String email, Long etapeId, CommentaireRequest requete) {
        Etape etape = etapeRepository.findById(etapeId)
                .orElseThrow(() -> BusinessException.introuvable("Jalon introuvable"));
        verifierParticipantDuProjet(etape.getProjet(), email);

        Commentaire commentaire = Commentaire.builder()
                .contenu(requete.contenu().trim())
                .auteur(trouverUtilisateur(email))
                .etape(etape)
                .build();

        return CommentaireDto.from(commentaireRepository.save(commentaire));
    }

    // ------------------------------------------------------------ prive

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
        throw BusinessException.interdit("Vous n'etes pas autorise a commenter ce projet");
    }
}
