package com.pfe.pfe_backend.dto;

/**
 * Reponse d'authentification : le couple de jetons et le profil connecte.
 *
 * accessToken  : duree de vie courte (15 min), envoye a chaque requete
 * refreshToken : duree de vie longue (7 j), sert uniquement a renouveler l'access
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UtilisateurDto utilisateur
) {
    public static AuthResponse of(String accessToken, String refreshToken,
                                  long expiresIn, UtilisateurDto utilisateur) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, utilisateur);
    }
}
