package com.pfe.pfe_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Generation et validation des jetons JWT (EF-01).
 *
 * Deux types de jetons :
 *  - access token  : 15 min, porte l'identite et le role, envoye a chaque requete
 *  - refresh token : 7 jours, ne sert qu'a obtenir un nouvel access token
 *
 * Le refresh token porte un claim "type" pour empecher qu'un refresh soit
 * presente comme un access token (confusion de jetons).
 */
@Component
public class JwtProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey cle;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessExpiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration) {

        byte[] octets = secret.getBytes(StandardCharsets.UTF_8);
        if (octets.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret doit faire au moins 32 caracteres (HMAC-SHA256).");
        }
        this.cle = Keys.hmacShaKeyFor(octets);
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    // ------------------------------------------------------------ generation

    public String genererAccessToken(String email, String role) {
        return construire(email, role, TYPE_ACCESS, accessExpiration);
    }

    public String genererRefreshToken(String email) {
        return construire(email, null, TYPE_REFRESH, refreshExpiration);
    }

    private String construire(String email, String role, String type, long dureeMs) {
        Date maintenant = new Date();
        Date expiration = new Date(maintenant.getTime() + dureeMs);

        var builder = Jwts.builder()
                .subject(email)
                .claim(CLAIM_TYPE, type)
                .issuedAt(maintenant)
                .expiration(expiration);

        if (role != null) {
            builder.claim(CLAIM_ROLE, role);
        }
        return builder.signWith(cle).compact();
    }

    // ------------------------------------------------------------ lecture

    public String extraireEmail(String token) {
        return lireClaims(token).getSubject();
    }

    public String extraireRole(String token) {
        return lireClaims(token).get(CLAIM_ROLE, String.class);
    }

    public Date extraireExpiration(String token) {
        return lireClaims(token).getExpiration();
    }

    private Claims lireClaims(String token) {
        return Jwts.parser()
                .verifyWith(cle)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ------------------------------------------------------------ validation

    /** Valide la signature, l'expiration et le type attendu. */
    public boolean estAccessTokenValide(String token) {
        return estValide(token, TYPE_ACCESS);
    }

    public boolean estRefreshTokenValide(String token) {
        return estValide(token, TYPE_REFRESH);
    }

    private boolean estValide(String token, String typeAttendu) {
        try {
            Claims claims = lireClaims(token);
            return typeAttendu.equals(claims.get(CLAIM_TYPE, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            // signature invalide, jeton expire, format incorrect...
            log.debug("Jeton rejete : {}", e.getMessage());
            return false;
        }
    }

    public long getAccessExpirationSecondes() {
        return accessExpiration / 1000;
    }
}
