package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.Promotion;
import com.pfe.pfe_backend.dto.PromotionDto;
import com.pfe.pfe_backend.dto.PromotionRequest;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.EtudiantRepository;
import com.pfe.pfe_backend.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion du referentiel des promotions (EF-07).
 * Lecture ouverte a tous (formulaire d'inscription), ecriture reservee a l'administrateur.
 */
@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final EtudiantRepository etudiantRepository;

    @Transactional(readOnly = true)
    public List<PromotionDto> lister() {
        return promotionRepository.findAllByOrderByAnneeDesc().stream()
                .map(PromotionDto::from)
                .toList();
    }

    @Transactional
    public PromotionDto creer(PromotionRequest requete) {
        String libelle = requete.libelle().trim();

        if (promotionRepository.existsByLibelle(libelle)) {
            throw BusinessException.conflit("Ce libelle de promotion est deja utilise");
        }

        Promotion promotion = Promotion.builder()
                .annee(requete.annee())
                .libelle(libelle)
                .build();

        return PromotionDto.from(promotionRepository.save(promotion));
    }

    @Transactional
    public PromotionDto modifier(Long id, PromotionRequest requete) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> BusinessException.introuvable("Promotion introuvable"));

        String libelle = requete.libelle().trim();
        if (!libelle.equals(promotion.getLibelle()) && promotionRepository.existsByLibelle(libelle)) {
            throw BusinessException.conflit("Ce libelle de promotion est deja utilise");
        }

        promotion.setAnnee(requete.annee());
        promotion.setLibelle(libelle);

        return PromotionDto.from(promotion);
    }

    @Transactional
    public void supprimer(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> BusinessException.introuvable("Promotion introuvable"));

        if (etudiantRepository.existsByPromotionId(id)) {
            throw BusinessException.conflit(
                    "Impossible de supprimer cette promotion : des etudiants y sont rattaches");
        }

        promotionRepository.delete(promotion);
    }
}
