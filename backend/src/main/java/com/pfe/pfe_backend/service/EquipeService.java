package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.Equipe;
import com.pfe.pfe_backend.domain.Etudiant;
import com.pfe.pfe_backend.dto.AjoutMembreRequest;
import com.pfe.pfe_backend.dto.EquipeDto;
import com.pfe.pfe_backend.dto.EquipeRequest;
import com.pfe.pfe_backend.dto.MembreEquipeDto;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.EquipeRepository;
import com.pfe.pfe_backend.repository.EtudiantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * Constitution et gestion des equipes d'etudiants (EF-11).
 *
 * Un etudiant appartient a au plus une equipe a la fois (Etudiant.equipe).
 * Le chef est celui qui a cree l'equipe ; il est seul habilite a en retirer
 * des membres, et ne peut la quitter que via dissoudre(). L'ajout d'un
 * membre peut venir soit du chef (ajouterMembre), soit du candidat
 * lui-meme muni du code d'invitation (rejoindreParCode, hors plan initial).
 * L'affectation d'un sujet a une equipe fait l'objet d'une etape ulterieure
 * du lot 2.
 */
@Service
@RequiredArgsConstructor
public class EquipeService {

    private final EquipeRepository equipeRepository;
    private final EtudiantRepository etudiantRepository;

    /** Alphabet sans caracteres ambigus (pas de 0/O ni 1/I) pour un code lisible a l'oral. */
    private static final String ALPHABET_CODE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LONGUEUR_CODE = 6;
    private static final SecureRandom ALEA = new SecureRandom();

    @Transactional(readOnly = true)
    public List<EquipeDto> lister() {
        return equipeRepository.findAllByOrderByDateCreationDesc().stream()
                .map(this::versDtoSansCode).toList();
    }

    @Transactional(readOnly = true)
    public EquipeDto trouver(Long id) {
        return versDtoSansCode(trouverEntite(id));
    }

    /**
     * Equipe de l'etudiant connecte, code d'invitation inclus (etape
     * consultee par le chef pour le transmettre). Pas readOnly : une equipe
     * plus ancienne que ce changement peut encore avoir codeInvitation nul,
     * et doit pouvoir etre corrigee ici (dirty-checking JPA) sans migration.
     */
    @Transactional
    public EquipeDto trouverParEtudiant(String email) {
        Etudiant etudiant = trouverEtudiant(email);
        if (etudiant.getEquipe() == null) {
            throw BusinessException.introuvable("Vous n'appartenez a aucune equipe");
        }
        Equipe equipe = etudiant.getEquipe();
        garantirCodeInvitation(equipe);
        return versDto(equipe);
    }

    @Transactional
    public EquipeDto creer(String emailChef, EquipeRequest requete) {
        Etudiant chef = trouverEtudiant(emailChef);

        if (chef.getEquipe() != null) {
            throw BusinessException.conflit("Vous appartenez deja a une equipe");
        }

        Equipe equipe = Equipe.builder()
                .nom(requete.nom().trim())
                .tailleMax(requete.tailleMax())
                .chef(chef)
                .codeInvitation(genererCodeUnique())
                .build();
        equipe = equipeRepository.save(equipe);

        chef.setEquipe(equipe);

        return versDto(equipe);
    }

    @Transactional
    public EquipeDto ajouterMembre(Long equipeId, String emailChef, AjoutMembreRequest requete) {
        Equipe equipe = trouverEntite(equipeId);
        verifierChef(equipe, emailChef);

        Etudiant candidat = etudiantRepository.findByNumeroEtudiant(requete.numeroEtudiant())
                .orElseThrow(() -> BusinessException.introuvable("Aucun etudiant avec ce numero"));

        if (candidat.getEquipe() != null) {
            throw BusinessException.conflit("Cet etudiant appartient deja a une equipe");
        }

        Etudiant chef = equipe.getChef();
        if (!candidat.getFiliere().getId().equals(chef.getFiliere().getId())
                || !candidat.getPromotion().getId().equals(chef.getPromotion().getId())) {
            throw new BusinessException(
                    "L'etudiant doit etre de la meme filiere et de la meme promotion que l'equipe");
        }

        if (etudiantRepository.countByEquipeId(equipeId) >= equipe.getTailleMax()) {
            throw BusinessException.conflit("L'equipe a atteint sa taille maximale");
        }

        candidat.setEquipe(equipe);
        return versDto(equipe);
    }

    /**
     * Adhesion sur code d'invitation (hors plan, remplace en cours de lot 8
     * l'ancienne auto-inscription par liste ouverte de toutes les equipes
     * de la filiere/promotion). Memes regles metier qu'avant : meme
     * filiere/promotion que le chef, equipe pas pleine - seul le point
     * d'entree change (un code au lieu de parcourir une liste).
     */
    @Transactional
    public EquipeDto rejoindreParCode(String code, String emailEtudiant) {
        String codeNormalise = code == null ? "" : code.trim().toUpperCase();

        Equipe equipe = equipeRepository.findByCodeInvitation(codeNormalise)
                .orElseThrow(() -> BusinessException.introuvable("Code d'equipe invalide"));

        Etudiant candidat = trouverEtudiant(emailEtudiant);

        if (candidat.getEquipe() != null) {
            throw BusinessException.conflit("Vous appartenez deja a une equipe");
        }

        Etudiant chef = equipe.getChef();
        if (!candidat.getFiliere().getId().equals(chef.getFiliere().getId())
                || !candidat.getPromotion().getId().equals(chef.getPromotion().getId())) {
            throw new BusinessException(
                    "Vous devez etre de la meme filiere et de la meme promotion que cette equipe");
        }

        if (etudiantRepository.countByEquipeId(equipe.getId()) >= equipe.getTailleMax()) {
            throw BusinessException.conflit("Cette equipe a atteint sa taille maximale");
        }

        candidat.setEquipe(equipe);
        return versDto(equipe);
    }

    @Transactional
    public void retirerMembre(Long equipeId, String emailChef, Long etudiantId) {
        Equipe equipe = trouverEntite(equipeId);
        verifierChef(equipe, emailChef);

        if (equipe.getChef().getId().equals(etudiantId)) {
            throw new BusinessException("Le chef ne peut pas se retirer lui-meme ; dissolvez l'equipe");
        }

        Etudiant membre = etudiantRepository.findById(etudiantId)
                .orElseThrow(() -> BusinessException.introuvable("Etudiant introuvable"));

        if (membre.getEquipe() == null || !membre.getEquipe().getId().equals(equipeId)) {
            throw BusinessException.conflit("Cet etudiant n'appartient pas a cette equipe");
        }

        membre.setEquipe(null);
    }

    @Transactional
    public void quitter(Long equipeId, String emailEtudiant) {
        Etudiant etudiant = trouverEtudiant(emailEtudiant);
        Equipe equipe = trouverEntite(equipeId);

        if (etudiant.getEquipe() == null || !etudiant.getEquipe().getId().equals(equipeId)) {
            throw BusinessException.conflit("Vous n'appartenez pas a cette equipe");
        }
        if (equipe.getChef().getId().equals(etudiant.getId())) {
            throw new BusinessException("Le chef ne peut pas quitter l'equipe ; dissolvez-la");
        }

        etudiant.setEquipe(null);
    }

    @Transactional
    public void dissoudre(Long equipeId, String emailChef, boolean estAdmin) {
        Equipe equipe = trouverEntite(equipeId);
        if (!estAdmin) {
            verifierChef(equipe, emailChef);
        }

        etudiantRepository.findByEquipeId(equipeId).forEach(m -> m.setEquipe(null));
        equipeRepository.delete(equipe);
    }

    // ------------------------------------------------------------ prive

    private Equipe trouverEntite(Long id) {
        return equipeRepository.findById(id)
                .orElseThrow(() -> BusinessException.introuvable("Equipe introuvable"));
    }

    private Etudiant trouverEtudiant(String email) {
        return etudiantRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.introuvable("Etudiant introuvable"));
    }

    private void verifierChef(Equipe equipe, String emailChef) {
        if (!equipe.getChef().getEmail().equalsIgnoreCase(emailChef)) {
            throw BusinessException.interdit("Seul le chef d'equipe peut effectuer cette action");
        }
    }

    /** Retro-genere un code pour une equipe creee avant ce changement (cf. Javadoc de Equipe.codeInvitation). */
    private void garantirCodeInvitation(Equipe equipe) {
        if (equipe.getCodeInvitation() == null) {
            equipe.setCodeInvitation(genererCodeUnique());
        }
    }

    private String genererCodeUnique() {
        String code;
        do {
            code = genererCode();
        } while (equipeRepository.existsByCodeInvitation(code));
        return code;
    }

    private String genererCode() {
        StringBuilder code = new StringBuilder(LONGUEUR_CODE);
        for (int i = 0; i < LONGUEUR_CODE; i++) {
            code.append(ALPHABET_CODE.charAt(ALEA.nextInt(ALPHABET_CODE.length())));
        }
        return code.toString();
    }

    /** Vue complete, code d'invitation inclus - reservee au(x) membre(s) de l'equipe elle-meme. */
    private EquipeDto versDto(Equipe equipe) {
        return construireDto(equipe, equipe.getCodeInvitation());
    }

    /** Vue pour les listings generaux (admin, recherche par id) : jamais le code, qui n'a de sens que pour l'equipe elle-meme. */
    private EquipeDto versDtoSansCode(Equipe equipe) {
        return construireDto(equipe, null);
    }

    private EquipeDto construireDto(Equipe equipe, String code) {
        List<MembreEquipeDto> membres = etudiantRepository.findByEquipeId(equipe.getId()).stream()
                .map(e -> new MembreEquipeDto(e.getId(), e.getNomComplet(), e.getNumeroEtudiant()))
                .toList();

        return new EquipeDto(
                equipe.getId(),
                equipe.getNom(),
                equipe.getTailleMax(),
                equipe.getChef().getId(),
                equipe.getChef().getNomComplet(),
                code,
                membres,
                equipe.getDateCreation()
        );
    }
}
