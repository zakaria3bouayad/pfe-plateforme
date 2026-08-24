package com.pfe.pfe_backend.security;

import com.pfe.pfe_backend.domain.Utilisateur;
import com.pfe.pfe_backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fait le pont entre nos entites et Spring Security.
 *
 * Spring Security ne connait pas la classe Utilisateur : il attend un UserDetails.
 * Ce service charge le compte par email et le convertit.
 *
 * Le role est prefixe par "ROLE_" car c'est la convention attendue par
 * hasRole('ETUDIANT') dans les annotations @PreAuthorize.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Aucun compte pour l'email : " + email));

        return User.builder()
                .username(utilisateur.getEmail())
                .password(utilisateur.getMotDePasse())
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name())))
                .disabled(!utilisateur.isActif())
                .build();
    }
}
