package com.pfe.pfe_backend.dto;

import com.pfe.pfe_backend.domain.Message;

import java.time.LocalDateTime;

/**
 * Representation d'un message renvoye au frontend (Lot 5, bloc A).
 */
public record MessageDto(
        Long id,
        String contenu,
        Long auteurId,
        String auteurNom,
        Long projetId,
        LocalDateTime dateEnvoi
) {
    public static MessageDto from(Message m) {
        return new MessageDto(
                m.getId(),
                m.getContenu(),
                m.getAuteur().getId(),
                m.getAuteur().getNomComplet(),
                m.getProjet().getId(),
                m.getDateEnvoi()
        );
    }
}
