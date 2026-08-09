package com.pfe.pfe_backend.config;

import com.pfe.pfe_backend.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Regles d'acces de l'API (ENF-08).
 *
 * Principe : tout est interdit par defaut, on ouvre explicitement ce qui doit
 * l'etre. C'est l'inverse d'une liste noire, et c'est bien plus sur.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    /** BCrypt, cout 12 (EF-02). Plus le cout est eleve, plus le hachage est lent a casser. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // Pas de CSRF : l'API est sans etat et n'utilise pas de cookie de session.
            .csrf(csrf -> csrf.disable())

            // Aucune session serveur : l'identite est portee par le jeton (ENF-07).
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Renvoie 401 au lieu d'une redirection vers une page de connexion.
            .exceptionHandling(ex ->
                    ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

            .authorizeHttpRequests(auth -> auth
                    // --- routes publiques ---
                    .requestMatchers(
                            "/api/auth/register",
                            "/api/auth/login",
                            "/api/auth/refresh").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    .requestMatchers("/error").permitAll()

                    // Referentiel en lecture seule : necessaire au formulaire d'inscription,
                    // appele avant qu'un jeton n'existe. L'ecriture reste protegee par
                    // @PreAuthorize sur les controleurs (EF-07).
                    .requestMatchers(HttpMethod.GET, "/api/filieres", "/api/promotions").permitAll()

                    // --- routes reservees ---
                    .requestMatchers("/api/admin/**").hasRole("ADMINISTRATEUR")

                    // --- tout le reste exige un jeton valide (dont /api/auth/me) ---
                    .anyRequest().authenticated()
            )

            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
