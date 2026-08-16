package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.*;
import com.pfe.pfe_backend.domain.enums.Role;
import com.pfe.pfe_backend.dto.*;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.*;
import com.pfe.pfe_backend.security.JwtProvider;
import com.pfe.pfe_backend.security.RefreshTokenBlacklistService;
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
    private final RefreshTokenBlacklistService refreshTokenBlacklistService;

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

    /**
     * Renouvellement avec rotation (Lot 5, bloc C) : chaque refresh token
     * n'est utilisable qu'une seule fois. Une fois consomme ici, il est
     * immediatement place en liste noire (Redis) ; le couple de jetons
     * renvoye contient un nouveau refresh token qui prend le relais. Un
     * jeton vole et deja utilise par le veritable proprietaire echoue donc
     * au prochain essai, meme s'il n'est pas encore expire.
     */
    @Transactional(readOnly = true)
    public AuthResponse rafraichir(RefreshRequest requete) {

        String token = requete.refreshToken();

        if (!jwtProvider.estRefreshTokenValide(token)) {
            throw new BusinessException("Refresh token invalide ou expire",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        if (refreshTokenBlacklistService.estRevoque(token)) {
            throw new BusinessException("Refresh token revoque",
                    org.springframework.http.HttpStatus.UNAUTHORIZED);
        }

        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(jwtProvider.extraireEmail(token))
                .orElseThrow(() -> BusinessException.introuvable("Compte introuvable"));

        if (!utilisateur.isActif()) {
            throw new DisabledException("Compte desactive");
        }

        refreshTokenBlacklistService.revoquer(token);
        return genererReponse(utilisateur);
    }

    // ------------------------------------------------------------ deconnexion

    /**
     * Deconnexion explicite (Lot 5, bloc C) : revoque le refresh token
     * fourni pour qu'il ne puisse plus servir a /refresh. L'access token
     * deja emis reste valide jusqu'a son expiration naturelle (15 min) :
     * c'est la limite acceptee de ce mecanisme, coherent avec l'absence
     * d'etat serveur pour les access tokens (ENF-07).
     */
    public void deconnecter(RefreshRequest requete) {
        String token = requete.refreshToken();
        if (jwtProvider.estRefreshTokenValide(token)) {
            refreshTokenBlacklistService.revoquer(token);
        }
        // Jeton deja invalide/expire : rien a faire, il ne pourrait de toute facon plus servir.
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
