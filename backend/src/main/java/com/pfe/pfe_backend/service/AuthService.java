package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.*;
import com.pfe.pfe_backend.domain.enums.Role;
import com.pfe.pfe_backend.dto.*;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.*;
import com.pfe.pfe_backend.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logique d'inscription, de connexion et de renouvellement de jeton (EF-01, EF-02).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UtilisateurRepository utilisateurRepository;
    private final FiliereRepository filiereRepository;
    private final PromotionRepository promotionRepository;
    private final EtudiantRepository etudiantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // ------------------------------------------------------------ inscription

    @Transactional
    public AuthResponse inscrire(RegisterRequest requete) {

        if (utilisateurRepository.existsByEmail(requete.email())) {
            throw BusinessException.conflit("Un compte existe deja avec cet email");
        }

        Utilisateur utilisateur = switch (requete.role()) {
            case ETUDIANT -> construireEtudiant(requete);
            case ENCADRANT -> construireSuperviseur(requete);
            case ADMINISTRATEUR -> construireAdministrateur(requete);
        };

        renseignerChampsCommuns(utilisateur, requete);
        utilisateurRepository.save(utilisateur);

        log.info("Nouveau compte cree : {} ({})", utilisateur.getEmail(), utilisateur.getRole());
        return genererReponse(utilisateur);
    }

    private Etudiant construireEtudiant(RegisterRequest r) {
        if (r.numeroEtudiant() == null || r.numeroEtudiant().isBlank()) {
            throw new BusinessException("Le numero etudiant est obligatoire pour un etudiant");
        }
        if (r.filiereId() == null || r.promotionId() == null) {
            throw new BusinessException("La filiere et la promotion sont obligatoires pour un etudiant");
        }
        if (etudiantRepository.existsByNumeroEtudiant(r.numeroEtudiant())) {
            throw BusinessException.conflit("Ce numero etudiant est deja utilise");
        }

        Filiere filiere = filiereRepository.findById(r.filiereId())
                .orElseThrow(() -> BusinessException.introuvable("Filiere introuvable"));
        Promotion promotion = promotionRepository.findById(r.promotionId())
                .orElseThrow(() -> BusinessException.introuvable("Promotion introuvable"));

        Etudiant etudiant = new Etudiant();
        etudiant.setNumeroEtudiant(r.numeroEtudiant());
        etudiant.setFiliere(filiere);
        etudiant.setPromotion(promotion);
        return etudiant;
    }

    private Superviseur construireSuperviseur(RegisterRequest r) {
        Superviseur superviseur = new Superviseur();
        superviseur.setSpecialite(r.specialite());
        superviseur.setGrade(r.grade());
        superviseur.setDepartement(r.departement());
        return superviseur;
    }

    private Administrateur construireAdministrateur(RegisterRequest r) {
        Administrateur admin = new Administrateur();
        admin.setNiveauAcces("STANDARD");
        return admin;
    }

    private void renseignerChampsCommuns(Utilisateur u, RegisterRequest r) {
        u.setNom(r.nom());
        u.setPrenom(r.prenom());
        u.setEmail(r.email().toLowerCase().trim());
        u.setMotDePasse(passwordEncoder.encode(r.motDePasse()));
        u.setTelephone(r.telephone());
        u.setRole(r.role());
        u.setActif(true);
    }

    // ------------------------------------------------------------ connexion

    @Transactional(readOnly = true)
    public AuthResponse connecter(LoginRequest requete) {

        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(requete.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));

        if (!passwordEncoder.matches(requete.motDePasse(), utilisateur.getMotDePasse())) {
            log.warn("Echec de connexion pour {}", requete.email());
            throw new BadCredentialsException("Identifiants invalides");
        }

        if (!utilisateur.isActif()) {
            throw new DisabledException("Compte desactive");
        }

        log.info("Connexion reussie : {}", utilisateur.getEmail());
        return genererReponse(utilisateur);
    }

    // ------------------------------------------------------------ refresh

    @Transactional(readOnly = true)
    public AuthResponse rafraichir(RefreshRequest requete) {

        String token = requete.refreshToken();

        if (!jwtProvider.estRefreshTokenValide(token)) {
            throw new BusinessException("Refresh token invalide ou expire",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }

        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(jwtProvider.extraireEmail(token))
                .orElseThrow(() -> BusinessException.introuvable("Compte introuvable"));

        if (!utilisateur.isActif()) {
            throw new DisabledException("Compte desactive");
        }

        return genererReponse(utilisateur);
    }

    // ------------------------------------------------------------ commun

    private AuthResponse genererReponse(Utilisateur utilisateur) {
        String accessToken = jwtProvider.genererAccessToken(
                utilisateur.getEmail(), utilisateur.getRole().name());
        String refreshToken = jwtProvider.genererRefreshToken(utilisateur.getEmail());

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtProvider.getAccessExpirationSecondes(),
                UtilisateurDto.from(utilisateur));
    }
}
