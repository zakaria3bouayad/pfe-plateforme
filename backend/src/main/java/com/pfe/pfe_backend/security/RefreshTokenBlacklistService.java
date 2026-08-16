package com.pfe.pfe_backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;

/**
 * Liste noire des refresh tokens revoques (Lot 5, bloc C - dette technique
 * du lot 2). Backend Redis : le conteneur est present dans docker-compose
 * depuis le lot 2 mais n'avait jamais ete utilise.
 *
 * Un refresh token revoque (deconnexion explicite, ou remplace lors de la
 * rotation appliquee a chaque appel a /refresh) est enregistre dans Redis
 * avec un TTL egal a sa duree de validite restante. Passe ce delai, le
 * jeton serait de toute facon rejete par la verification de signature et
 * d'expiration du JWT lui-meme : inutile de le garder plus longtemps en
 * liste noire, Redis le purge tout seul (pas de purge manuelle a prevoir).
 *
 * La cle stockee est un hash SHA-256 du jeton, pas le jeton en clair :
 * evite de faire apparaitre des jetons valides dans les journaux ou outils
 * d'administration Redis.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenBlacklistService {

    private static final String PREFIXE_CLE = "refresh:revoque:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProvider jwtProvider;

    /** Revoque un refresh token : ne pourra plus servir a /refresh, meme avant son expiration naturelle. */
    public void revoquer(String refreshToken) {
        Duration dureeRestante = dureeRestante(refreshToken);
        if (dureeRestante.isZero() || dureeRestante.isNegative()) {
            return; // deja expire, inutile de le stocker
        }
        redisTemplate.opsForValue().set(cle(refreshToken), "1", dureeRestante);
    }

    /** Indique si ce refresh token a ete revoque (deconnexion ou rotation). */
    public boolean estRevoque(String refreshToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(cle(refreshToken)));
    }

    private Duration dureeRestante(String refreshToken) {
        Date expiration = jwtProvider.extraireExpiration(refreshToken);
        return Duration.ofMillis(expiration.getTime() - System.currentTimeMillis());
    }

    private String cle(String refreshToken) {
        return PREFIXE_CLE + hacher(refreshToken);
    }

    private String hacher(String valeur) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] octets = digest.digest(valeur.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(octets);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 est garanti disponible sur toute JVM standard.
            throw new IllegalStateException(e);
        }
    }
}
