package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.Filiere;
import com.pfe.pfe_backend.dto.FiliereDto;
import com.pfe.pfe_backend.dto.FiliereRequest;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.EtudiantRepository;
import com.pfe.pfe_backend.repository.FiliereRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion du referentiel des filieres (EF-07).
 * Lecture ouverte a tous (formulaire d'inscription), ecriture reservee a l'administrateur.
 */
@Service
@RequiredArgsConstructor
public class FiliereService {

    private final FiliereRepository filiereRepository;
    private final EtudiantRepository etudiantRepository;

    @Transactional(readOnly = true)
    public List<FiliereDto> lister() {
        return filiereRepository.findAllByOrderByLibelleAsc().stream()
                .map(FiliereDto::from)
                .toList();
    }

    @Transactional
    public FiliereDto creer(FiliereRequest requete) {
        String code = requete.code().trim().toUpperCase();

        if (filiereRepository.existsByCode(code)) {
            throw BusinessException.conflit("Ce code de filiere est deja utilise");
        }

        Filiere filiere = Filiere.builder()
                .code(code)
                .libelle(requete.libelle().trim())
                .departement(requete.departement())
                .build();

        return FiliereDto.from(filiereRepository.save(filiere));
    }

    @Transactional
    public FiliereDto modifier(Long id, FiliereRequest requete) {
        Filiere filiere = filiereRepository.findById(id)
                .orElseThrow(() -> BusinessException.introuvable("Filiere introuvable"));

        String code = requete.code().trim().toUpperCase();
        if (!code.equals(filiere.getCode()) && filiereRepository.existsByCode(code)) {
            throw BusinessException.conflit("Ce code de filiere est deja utilise");
        }

        filiere.setCode(code);
        filiere.setLibelle(requete.libelle().trim());
        filiere.setDepartement(requete.departement());

        return FiliereDto.from(filiere);
    }

    @Transactional
    public void supprimer(Long id) {
        Filiere filiere = filiereRepository.findById(id)
                .orElseThrow(() -> BusinessException.introuvable("Filiere introuvable"));

        if (etudiantRepository.existsByFiliereId(id)) {
            throw BusinessException.conflit(
                    "Impossible de supprimer cette filiere : des etudiants y sont rattaches");
        }

        filiereRepository.delete(filiere);
    }
}
