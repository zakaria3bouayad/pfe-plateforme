package com.pfe.pfe_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Envoi d'un message dans la messagerie d'un projet (Lot 5, bloc A).
 */
public record MessageRequest(

        @NotBlank(message = "Le message ne peut pas etre vide")
        @Size(max = 2000, message = "Le message ne doit pas depasser 2000 caracteres")
        String contenu
) {}
