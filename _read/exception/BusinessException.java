package com.pfe.pfe_backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception metier : une regle du domaine est violee.
 * Porte le code HTTP a renvoyer, ce qui evite de le decider dans le controleur.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus statut;

    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String message, HttpStatus statut) {
        super(message);
        this.statut = statut;
    }

    public HttpStatus getStatut() {
        return statut;
    }

    // --- fabriques pour les cas les plus frequents ---

    public static BusinessException introuvable(String message) {
        return new BusinessException(message, HttpStatus.NOT_FOUND);
    }

    public static BusinessException conflit(String message) {
        return new BusinessException(message, HttpStatus.CONFLICT);
    }

    public static BusinessException interdit(String message) {
        return new BusinessException(message, HttpStatus.FORBIDDEN);
    }
}
