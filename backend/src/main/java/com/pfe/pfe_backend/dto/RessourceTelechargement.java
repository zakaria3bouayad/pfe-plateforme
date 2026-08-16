package com.pfe.pfe_backend.dto;

import java.io.InputStream;

/**
 * Contenu du fichier d'une ressource pret a etre renvoye au client (Lot 5,
 * bloc B). Transport interne uniquement (service -> controleur) : a la
 * charge de l'appelant de fermer le flux une fois la reponse ecrite.
 */
public record RessourceTelechargement(String nom, String type, InputStream contenu) {}
