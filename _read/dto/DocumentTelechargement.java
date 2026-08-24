package com.pfe.pfe_backend.dto;

import java.io.InputStream;

/**
 * Contenu d'un document pret a etre renvoye au client (Lot 4, bloc A).
 * Transport interne uniquement (service -> controleur) : a la charge de
 * l'appelant de fermer le flux une fois la reponse ecrite.
 */
public record DocumentTelechargement(String nom, String type, InputStream contenu) {}
