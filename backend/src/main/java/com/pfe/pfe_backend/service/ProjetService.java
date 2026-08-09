package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.Equipe;
import com.pfe.pfe_backend.domain.Etudiant;
import com.pfe.pfe_backend.domain.Projet;
import com.pfe.pfe_backend.domain.Sujet;
import com.pfe.pfe_backend.domain.Superviseur;
import com.pfe.pfe_backend.domain.enums.StatutProjet;
import com.pfe.pfe_backend.domain.enums.StatutSujet;
import com.pfe.pfe_backend.dto.ProjetDto;
import com.pfe.pfe_backend.dto.ProjetRequest;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.EtudiantRepository;
import com.pfe.pfe_backend.repository.ProjetRepository;
import com.pfe.pfe_backend.repository.SujetRepository;
import com.pfe.pfe_backend.repository.SuperviseurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Affectation d'un sujet valide a une equipe (EF-12).
 *
 * Cree le Projet et fait passer le sujet en AFFECTE, apres verification de
 * trois contraintes : le sujet doit etre VALIDE et non deja affecte,
 * l'effectif de l'equipe doit tenir dans la capacite du sujet, et le quota
 * de projets simultanes de l'encadrant ne doit pas etre depasse (EF-10).
 */
@Service
@RequiredArgsConstructor
public class ProjetService {

    private final ProjetRepository projetRepository;
    private final SujetRepository sujetRepository;
    private final EtudiantRepository etudiantRepository;
    private final SuperviseurRepository superviseurRepository;

    @Transactional(readOnly = true)
    public List<ProjetDto> lister() {
        return projetRepository.findAllByOrderByDateAffectationDesc().stream()
                .map(ProjetDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjetDto trouver(Long id) {
        return ProjetDto.from(trouverEntite(id));
    }

    @Transactional(readOnly = true)
    public ProjetDto trouverParEtudiant(String email) {
        Etudiant etudiant = etudiantRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.introuvable("Etudiant introuvable"));

        if (etudiant.getEquipe() == null) {
            throw BusinessException.introuvable("Vous n'appartenez a aucune equipe");
        }

        return projetRepository.findByEquipeId(etudiant.getEquipe().getId())
                .map(ProjetDto::from)
                .orElseThrow(() -> BusinessException.introuvable("Votre equipe n'a pas encore de projet"));
    }

    @Transactional(readOnly = true)
    public List<ProjetDto> listerParEncadrant(String emailEncadrant) {
        Superviseur encadrant = superviseurRepository.findByEmail(emailEncadrant)
                .orElseThrow(() -> BusinessException.introuvable("Encadrant introuvable"));

        return projetRepository.findByEncadrantIdOrderByDateAffectationDesc(encadrant.getId())
                .stream().map(ProjetDto::from).toList();
    }

    @Transactional
    public ProjetDto affecter(String emailChef, ProjetRequest requete) {
        Etudiant chef = etudiantRepository.findByEmail(emailChef)
                .orElseThrow(() -> BusinessException.introuvable("Etudiant introuvable"));

        Equipe equipe = chef.getEquipe();
        if (equipe == null) {
            throw new BusinessException("Vous devez appartenir a une equipe pour demander un sujet");
        }
        if (!equipe.getChef().getId().equals(chef.getId())) {
            throw BusinessException.interdit("Seul le chef d'equipe peut demander l'affectation d'un sujet");
        }
        if (projetRepository.findByEquipeId(equipe.getId()).isPresent()) {
            throw BusinessException.conflit("Votre equipe a deja un projet");
        }

        Sujet sujet = sujetRepository.findById(requete.sujetId())
                .orElseThrow(() -> BusinessException.introuvable("Sujet introuvable"));

        if (sujet.getStatut() != StatutSujet.VALIDE) {
            throw BusinessException.conflit("Ce sujet n'est pas valide, il ne peut pas etre affecte");
        }
        if (projetRepository.findBySujetId(sujet.getId()).isPresent()) {
            throw BusinessException.conflit("Ce sujet est deja affecte a une autre equipe");
        }

        long effectifEquipe = etudiantRepository.countByEquipeId(equipe.getId());
        if (effectifEquipe > sujet.getCapaciteMax()) {
            throw BusinessException.conflit(
                    "Votre equipe compte plus d'etudiants que la capacite de ce sujet ("
                            + sujet.getCapaciteMax() + ")");
        }

        Superviseur encadrant = sujet.getEncadrant();
        long projetsEnCours = projetRepository.countByEncadrantIdAndStatut(
                encadrant.getId(), StatutProjet.EN_COURS);
        if (projetsEnCours >= encadrant.getQuotaProjets()) {
            throw BusinessException.conflit("L'encadrant de ce sujet a atteint son quota de projets");
        }

        Projet projet = Projet.builder()
                .sujet(sujet)
                .equipe(equipe)
                .encadrant(encadrant)
                .statut(StatutProjet.EN_COURS)
                .build();
        projet = projetRepository.save(projet);

        sujet.setStatut(StatutSujet.AFFECTE);

        return ProjetDto.from(projet);
    }

    private Projet trouverEntite(Long id) {
        return projetRepository.findById(id)
                .orElseThrow(() -> BusinessException.introuvable("Projet introuvable"));
    }
}
