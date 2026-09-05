package com.pfe.pfe_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Intercepte chaque requete HTTP, lit l'en-tete Authorization et, si le
 * jeton est valide, place l'utilisateur dans le contexte de securite.
 *
 * OncePerRequestFilter garantit une seule execution par requete, meme en cas
 * de forward interne.
 *
 * Ce filtre n'interdit rien : il se contente d'authentifier. Le refus (401/403)
 * est decide ensuite par SecurityConfig.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIXE = "Bearer ";

    /** Routes reellement anonymes : inutile d'y chercher un jeton. */
    private static final Set<String> CHEMINS_PUBLICS = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh"
    );

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extraireToken(request);

        if (token != null
                && jwtProvider.estAccessTokenValide(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String email = jwtProvider.extraireEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (userDetails.isEnabled()) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extraireToken(HttpServletRequest request) {
        String entete = request.getHeader(HEADER);
        if (entete != null && entete.startsWith(PREFIXE)) {
            return entete.substring(PREFIXE.length());
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String chemin = request.getServletPath();
        return CHEMINS_PUBLICS.contains(chemin) || chemin.startsWith("/actuator/");
    }

    /**
     * OncePerRequestFilter ne se re-execute jamais sur un dispatch ASYNC par
     * defaut (shouldNotFilterAsyncDispatch() = true). Ca ne posait aucun
     * probleme tant que rien n'utilisait de traitement asynchrone - mais
     * ChatController (etape 8.8) renvoie un SseEmitter : quand celui-ci se
     * termine (emitter.complete()), Tomcat effectue un dispatch ASYNC pour
     * finaliser la reponse, qui retraverse toute la chaine de filtres. Sans
     * ce filtre pour re-authentifier a cette etape, AuthorizationFilter (qui
     * s'applique lui sur tous les types de dispatch) ne trouve plus
     * d'Authentication et rejette avec AuthorizationDeniedException - une
     * exception sans consequence pour le client (la reponse SSE est deja
     * entierement envoyee a ce stade) mais qui polluait les logs.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }
}
