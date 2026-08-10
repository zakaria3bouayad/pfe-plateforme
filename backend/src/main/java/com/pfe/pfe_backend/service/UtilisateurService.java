package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.Utilisateur;
import com.pfe.pfe_backend.domain.enums.Role;
import com.pfe.pfe_backend.dto.UtilisateurDto;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion des comptes par l'administrateur (EF-03). Un compte n'est jamais
 * supprime, seulement desactive logiquement (champ actif) : l'historique
 * (sujets, projets, jalons...) reste coherent.
 */
@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;

    @Transactional(readOnly = true)
    public List<UtilisateurDto> lister(Role role) {
        List<Utilisateur> utilisateurs = role != null
                ? utilisateurRepository.findByRoleOrderByNomAscPrenomAsc(role)
                : utilisateurRepository.findAllByOrderByNomAscPrenomAsc();
        return utilisateurs.stream().map(UtilisateurDto::from).toList();
    }

    @Transactional
    public UtilisateurDto changerStatutActif(Long id, boolean actif, String emailAdminConnecte) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> BusinessException.introuvable("Utilisateur introuvable"));

        if (utilisateur.getEmail().equalsIgnoreCase(emailAdminConnecte)) {
            throw BusinessException.conflit("Vous ne pouvez pas modifier le statut de votre propre compte");
        }

        utilisateur.setActif(actif);
        return UtilisateurDto.from(utilisateur);
    }
}
