package com.pfe.pfe_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Depot d'un commentaire sur un document ou un jalon (Lot 4, bloc B).
 */
public record CommentaireRequest(

        @NotBlank(message = "Le commentaire ne peut pas etre vide")
        @Size(max = 2000, message = "Le commentaire ne doit pas depasser 2000 caracteres")
        String contenu
) {}
