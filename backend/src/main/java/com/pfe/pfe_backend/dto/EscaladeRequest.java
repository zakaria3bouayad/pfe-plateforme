package com.pfe.pfe_backend.dto;

import jakarta.validation.constraints.Size;

/**
 * Corps de POST /api/assistant/escalade (etape 8.9). La question est
 * optionnelle : elle sert uniquement de contexte pour la journalisation, le
 * bouton d'escalade (etape 8.11) pouvant etre declenche juste apres une
 * reponse "je ne sais pas" comme independamment de toute question precise.
 */
public record EscaladeRequest(
        @Size(max = 2000, message = "La question est limitee a 2000 caracteres")
        String question
) {}
