package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.audit.Audite;
import com.pfe.pfe_backend.domain.Filiere;
import com.pfe.pfe_backend.domain.Sujet;
import com.pfe.pfe_backend.domain.Superviseur;
import com.pfe.pfe_backend.domain.enums.StatutSujet;
import com.pfe.pfe_backend.domain.enums.TypeNotification;
import com.pfe.pfe_backend.dto.SujetDto;
import com.pfe.pfe_backend.dto.SujetRequest;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.FiliereRepository;
import com.pfe.pfe_backend.repository.SujetRepository;
import com.pfe.pfe_backend.repository.SuperviseurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Proposition, gestion et validation des sujets de PFE (EF-08, EF-09).
 *
 * Machine a etats : PROPOSE -> EN_VALIDATION -> VALIDE / REJETE / A_CORRIGER.
 * L'affectation (AFFECTE) et la cloture (CLOTURE) relevent d'une etape
 * ulterieure du lot 2 (equipes / projets).
 */
@Service
@RequiredArgsConstructor
public class SujetService {

    private static final Set<StatutSujet> STATUTS_MODIFIABLES =
            EnumSet.of(StatutSujet.PROPOSE, StatutSujet.A_CORRIGER);

    /** Statuts qui ne comptent plus dans le quota d'un encadrant (EF-10). */
    private static final Set<StatutSujet> STATUTS_INACTIFS =
            EnumSet.of(StatutSujet.REJETE, StatutSujet.CLOTURE);

    private final SujetRepository sujetRepository;
    private final SuperviseurRepository superviseurRepository;
    private final FiliereRepository filiereRepository;
    private final NotificationService notificationService;
    private final MailService mailService;

    @Transactional(readOnly = true)
    public List<SujetDto> lister(StatutSujet statut) {
        List<Sujet> sujets = statut != null
                ? sujetRepository.findByStatutOrderByDatePropositionDesc(statut)
                : sujetRepository.findAllByOrderByDatePropositionDesc();
        return sujets.stream().map(SujetDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SujetDto> listerParEncadrant(String emailEncadrant) {
        Superviseur encadrant = trouverEncadrant(emailEncadrant);
        return sujetRepository.findByEncadrantIdOrderByDatePropositionDesc(encadrant.getId())
                .stream().map(SujetDto::from).toList();
    }

    @Transactional(readOnly = true)
    public SujetDto trouver(Long id) {
        return SujetDto.from(trouverEntite(id));
    }

    @Transactional
    public SujetDto proposer(String emailEncadrant, SujetRequest requete) {
        Superviseur encadrant = trouverEncadrant(emailEncadrant);
        String titre = requete.titre().trim();

        if (sujetRepository.existsByEncadrantIdAndTitreIgnoreCaseAndStatutNot(
                encadrant.getId(), titre, StatutSujet.REJETE)) {
            throw BusinessException.conflit("Vous avez deja un sujet actif avec ce titre");
        }

        long sujetsActifs = sujetRepository.countByEncadrantIdAndStatutNotIn(
                encadrant.getId(), STATUTS_INACTIFS);
        if (sujetsActifs >= encadrant.getQuotaProjets()) {
            throw BusinessException.conflit(
                    "Vous avez atteint votre quota de sujets actifs (" + encadrant.getQuotaProjets() + ")");
        }

        Filiere filiere = resoudreFiliere(requete.filiereId());

        Sujet sujet = Sujet.builder()
                .titre(titre)
                .description(requete.description().trim())
                .motsCles(requete.motsCles())
                .capaciteMax(requete.capaciteMax())
                .statut(StatutSujet.PROPOSE)
                .encadrant(encadrant)
                .filiere(filiere)
                .build();

        return SujetDto.from(sujetRepository.save(sujet));
    }

    @Transactional
    public SujetDto modifier(Long id, String emailEncadrant, boolean estAdmin, SujetRequest requete) {
        Sujet sujet = trouverEntite(id);
        verifierProprietaireOuAdmin(sujet, emailEncadrant, estAdmin);
        verifierModifiable(sujet);

        sujet.setTitre(requete.titre().trim());
        sujet.setDescription(requete.description().trim());
        sujet.setMotsCles(requete.motsCles());
        sujet.setCapaciteMax(requete.capaciteMax());
        sujet.setFiliere(resoudreFiliere(requete.filiereId()));

        // Une correction remet le sujet dans le circuit de validation.
        if (sujet.getStatut() == StatutSujet.A_CORRIGER) {
            sujet.setStatut(StatutSujet.PROPOSE);
            sujet.setCommentaireValidation(null);
        }

        return SujetDto.from(sujet);
    }

    // ------------------------------------------------------------ validation (EF-09)

    /** L'administrateur commence l'examen d'un sujet propose. */
    @Transactional
    public SujetDto demarrerValidation(Long id) {
        Sujet sujet = trouverEntite(id);
        exigerStatut(sujet, StatutSujet.PROPOSE);
        sujet.setStatut(StatutSujet.EN_VALIDATION);
        return SujetDto.from(sujet);
    }

    /** Couverture du journal d'audit (etape 7.8) : validation de sujet. */
    @Audite(action = "SUJET_VALIDE", cible = "'Sujet#' + #id")
    @Transactional
    public SujetDto valider(Long id) {
        Sujet sujet = trouverEntite(id);
        exigerStatut(sujet, StatutSujet.EN_VALIDATION);
        sujet.setStatut(StatutSujet.VALIDE);
        sujet.setCommentaireValidation(null);

        notifierDecision(sujet, TypeNotification.SUJET_VALIDE, "PFE - Sujet valide",
                "Votre sujet \"" + sujet.getTitre() + "\" a ete valide.");

        return SujetDto.from(sujet);
    }

    /** Couverture du journal d'audit (etape 7.8) : rejet de sujet. */
    @Audite(action = "SUJET_REJETE", cible = "'Sujet#' + #id", detail = "#commentaire")
    @Transactional
    public SujetDto rejeter(Long id, String commentaire) {
        Sujet sujet = trouverEntite(id);
        exigerStatut(sujet, StatutSujet.EN_VALIDATION);
        exigerCommentaire(commentaire);
        sujet.setStatut(StatutSujet.REJETE);
        sujet.setCommentaireValidation(commentaire.trim());

        notifierDecision(sujet, TypeNotification.SUJET_REJETE, "PFE - Sujet rejete",
                "Votre sujet \"" + sujet.getTitre() + "\" a ete rejete : " + commentaire.trim());

        return SujetDto.from(sujet);
    }

    @Transactional
    public SujetDto demanderCorrection(Long id, String commentaire) {
        Sujet sujet = trouverEntite(id);
        exigerStatut(sujet, StatutSujet.EN_VALIDATION);
        exigerCommentaire(commentaire);
        sujet.setStatut(StatutSujet.A_CORRIGER);
        sujet.setCommentaireValidation(commentaire.trim());
        return SujetDto.from(sujet);
    }

    @Transactional
    public void supprimer(Long id, String emailEncadrant, boolean estAdmin) {
        Sujet sujet = trouverEntite(id);
        verifierProprietaireOuAdmin(sujet, emailEncadrant, estAdmin);
        verifierModifiable(sujet);

        sujetRepository.delete(sujet);
    }

    // ------------------------------------------------------------ prive

    private Sujet trouverEntite(Long id) {
        return sujetRepository.findById(id)
                .orElseThrow(() -> BusinessException.introuvable("Sujet introuvable"));
    }

    private Superviseur trouverEncadrant(String email) {
        return superviseurRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.introuvable("Encadrant introuvable"));
    }

    private Filiere resoudreFiliere(Long filiereId) {
        if (filiereId == null) {
            return null;
        }
        return filiereRepository.findById(filiereId)
                .orElseThrow(() -> BusinessException.introuvable("Filiere introuvable"));
    }

    private void verifierProprietaireOuAdmin(Sujet sujet, String emailEncadrant, boolean estAdmin) {
        if (estAdmin) {
            return;
        }
        if (!sujet.getEncadrant().getEmail().equalsIgnoreCase(emailEncadrant)) {
            throw BusinessException.interdit("Ce sujet ne vous appartient pas");
        }
    }

    private void verifierModifiable(Sujet sujet) {
        if (!STATUTS_MODIFIABLES.contains(sujet.getStatut())) {
            throw BusinessException.conflit(
                    "Ce sujet est deja entre dans le circuit de validation et ne peut plus etre modifie");
        }
    }

    private void exigerStatut(Sujet sujet, StatutSujet attendu) {
        if (sujet.getStatut() != attendu) {
            throw BusinessException.conflit(
                    "Ce sujet est au statut " + sujet.getStatut()
                            + ", cette transition n'est possible que depuis " + attendu);
        }
    }

    private void exigerCommentaire(String commentaire) {
        if (commentaire == null || commentaire.isBlank()) {
            throw new BusinessException("Un commentaire est obligatoire pour cette decision");
        }
    }

    /**
     * Notification + mail (etape 7.4) : sujet valide et sujet rejete sont
     * tous deux dans la liste courte des mails (decision assumee), a
     * l'inverse de la demande de correction, qui n'est pas branchee ici.
     */
    private void notifierDecision(Sujet sujet, TypeNotification type, String objetMail, String message) {
        Superviseur encadrant = sujet.getEncadrant();
        notificationService.creer(encadrant, type, message, "/sujets/" + sujet.getId());
        mailService.envoyer(encadrant.getEmail(), objetMail, message);
    }
}
