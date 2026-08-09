package com.pfe.pfe_backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Intercepte toutes les exceptions des controleurs et les convertit en
 * reponse JSON homogene (ENF-34).
 *
 * Sans cette classe, une exception non geree renvoie une trace Java complete
 * au client : illisible pour l'utilisateur, et riche en informations pour un
 * attaquant.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Regle metier violee. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest request) {

        HttpStatus statut = ex.getStatut();
        return ResponseEntity.status(statut).body(ErrorResponse.of(
                statut.value(), statut.getReasonPhrase(), ex.getMessage(),
                request.getRequestURI()));
    }

    /** Echec de validation d'un DTO annote @Valid. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> champs = new HashMap<>();
        for (FieldError erreur : ex.getBindingResult().getFieldErrors()) {
            champs.put(erreur.getField(), erreur.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(ErrorResponse.validation(
                HttpStatus.BAD_REQUEST.value(), "Bad Request",
                "Certains champs sont invalides", request.getRequestURI(), champs));
    }

    /** Corps de requete illisible : JSON malforme, champ du mauvais type, etc. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), "Bad Request",
                "Corps de requete illisible ou mal forme", request.getRequestURI()));
    }

    /** Identifiants incorrects : message volontairement non discriminant. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(), "Unauthorized",
                "Email ou mot de passe incorrect", request.getRequestURI()));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(), "Forbidden",
                "Ce compte est desactive", request.getRequestURI()));
    }

    /** Role insuffisant pour la ressource demandee. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(), "Forbidden",
                "Acces refuse", request.getRequestURI()));
    }

    /** Filet de securite : toute exception non prevue. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {

        // La trace part dans les logs, jamais dans la reponse HTTP.
        log.error("Erreur non geree sur {}", request.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                "Une erreur interne est survenue", request.getRequestURI()));
    }
}
