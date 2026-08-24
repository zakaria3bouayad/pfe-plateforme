package com.pfe.pfe_backend.dto;

import com.pfe.pfe_backend.domain.Utilisateur;
import com.pfe.pfe_backend.domain.enums.Role;

/**
 * Representation d'un utilisateur renvoyee au frontend.
 *
 * Ne contient JAMAIS le mot de passe, meme hache (EF-02).
 * C'est la raison d'etre des DTO : controler precisement ce qui sort de l'API.
 */
public record UtilisateurDto(
        Long id,
        String nom,
        String prenom,
        String nomComplet,
        String email,
        String telephone,
        Role role,
        boolean actif
) {
    public static UtilisateurDto from(Utilisateur u) {
        return new UtilisateurDto(
                u.getId(),
                u.getNom(),
                u.getPrenom(),
                u.getNomComplet(),
                u.getEmail(),
                u.getTelephone(),
                u.getRole(),
                u.isActif()
        );
    }
}
