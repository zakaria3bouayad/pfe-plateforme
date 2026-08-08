package com.pfe.pfe_backend.dto;

import com.pfe.pfe_backend.domain.enums.Role;
import jakarta.validation.constraints.*;

/**
 * Donnees de creation de compte (EF-01, EF-03).
 *
 * Record Java : immuable, concis, pas de setter. Les DTO d'entree ne doivent
 * jamais etre modifies apres reception.
 *
 * filiereId / promotionId / numeroEtudiant ne sont exiges que pour un ETUDIANT ;
 * cette regle conditionnelle est verifiee dans AuthService, pas ici.
 */
public record RegisterRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 60)
        String nom,

        @NotBlank(message = "Le prenom est obligatoire")
        @Size(max = 60)
        String prenom,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        @Size(max = 120)
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 72, message = "Le mot de passe doit contenir au moins 8 caracteres")
        String motDePasse,

        @Size(max = 20)
        String telephone,

        @NotNull(message = "Le role est obligatoire")
        Role role,

        // --- specifique ETUDIANT ---
        @Size(max = 20)
        String numeroEtudiant,
        Long filiereId,
        Long promotionId,

        // --- specifique ENCADRANT ---
        @Size(max = 100)
        String specialite,
        @Size(max = 50)
        String grade,
        @Size(max = 100)
        String departement
) {}
