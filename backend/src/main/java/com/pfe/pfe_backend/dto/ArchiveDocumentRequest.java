package com.pfe.pfe_backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Marquage ou demarquage d'un document comme rapport de reference archive
 * (Lot 6, etape 6.1).
 *
 * Booleen objet et non primitif : un corps de requete omettant le champ doit
 * etre rejete par la validation plutot que silencieusement interprete comme
 * un demarquage.
 */
public record ArchiveDocumentRequest(@NotNull Boolean archive) {}
