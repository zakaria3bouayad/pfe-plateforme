package com.pfe.pfe_backend.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Format unique de reponse d'erreur de l'API (ENF-34).
 *
 * Le frontend n'a ainsi qu'une seule structure a traiter, quelle que soit
 * l'origine de l'erreur.
 *
 * champs : detail des erreurs de validation, champ par champ. Null sinon.
 */
public record ErrorResponse(
        LocalDateTime horodatage,
        int statut,
        String erreur,
        String message,
        String chemin,
        Map<String, String> champs
) {
    public static ErrorResponse of(int statut, String erreur, String message, String chemin) {
        return new ErrorResponse(LocalDateTime.now(), statut, erreur, message, chemin, null);
    }

    public static ErrorResponse validation(int statut, String erreur, String message,
                                           String chemin, Map<String, String> champs) {
        return new ErrorResponse(LocalDateTime.now(), statut, erreur, message, chemin, champs);
    }
}
