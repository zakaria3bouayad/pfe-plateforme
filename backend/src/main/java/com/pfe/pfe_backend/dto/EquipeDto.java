package com.pfe.pfe_backend.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Representation d'une equipe renvoyee au frontend (EF-11).
 *
 * codeInvitation : ajoute hors plan (lot 8) avec le passage a une adhesion
 * par code. Reste dans le meme DTO plutot que dans un DTO separe reserve au
 * chef, par simplicite - la portee reelle (qui peut voir/utiliser ce code)
 * est deja limitee par les regles metier de rejoindreParCode (filiere,
 * promotion, place disponible), pas par la simple connaissance de la chaine.
 */
public record EquipeDto(
        Long id,
        String nom,
        int tailleMax,
        Long chefId,
        String chefNom,
        String codeInvitation,
        List<MembreEquipeDto> membres,
        LocalDateTime dateCreation
) {}
