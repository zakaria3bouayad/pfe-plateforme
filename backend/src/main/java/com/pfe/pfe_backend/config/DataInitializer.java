package com.pfe.pfe_backend.config;

import com.pfe.pfe_backend.domain.Administrateur;
import com.pfe.pfe_backend.domain.Filiere;
import com.pfe.pfe_backend.domain.Promotion;
import com.pfe.pfe_backend.domain.enums.Role;
import com.pfe.pfe_backend.repository.FiliereRepository;
import com.pfe.pfe_backend.repository.PromotionRepository;
import com.pfe.pfe_backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Jeu de donnees de demonstration (ENF-32).
 *
 * S'execute a chaque demarrage mais n'insere que ce qui manque : relancer
 * l'application ne cree pas de doublon.
 *
 * @Profile("!prod") : ne s'active jamais en production.
 */
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final FiliereRepository filiereRepository;
    private final PromotionRepository promotionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initialiserFilieres();
        initialiserPromotions();
        initialiserAdministrateur();
    }

    private void initialiserFilieres() {
        List<Filiere> filieres = List.of(
                Filiere.builder().code("GI").libelle("Genie Informatique")
                        .departement("Informatique").build(),
                Filiere.builder().code("GE").libelle("Genie Electrique")
                        .departement("Electrique").build(),
                Filiere.builder().code("GC").libelle("Genie Civil")
                        .departement("Civil").build(),
                Filiere.builder().code("GIND").libelle("Genie Industriel")
                        .departement("Industriel").build()
        );

        for (Filiere f : filieres) {
            if (!filiereRepository.existsByCode(f.getCode())) {
                filiereRepository.save(f);
                log.info("Filiere creee : {} - {}", f.getCode(), f.getLibelle());
            }
        }
    }

    private void initialiserPromotions() {
        List<Promotion> promotions = List.of(
                Promotion.builder().annee(2025).libelle("2024-2025").build(),
                Promotion.builder().annee(2026).libelle("2025-2026").build(),
                Promotion.builder().annee(2027).libelle("2026-2027").build()
        );

        for (Promotion p : promotions) {
            if (!promotionRepository.existsByLibelle(p.getLibelle())) {
                promotionRepository.save(p);
                log.info("Promotion creee : {}", p.getLibelle());
            }
        }
    }

    private void initialiserAdministrateur() {
        String email = "admin@pfe.local";

        if (utilisateurRepository.existsByEmail(email)) {
            return;
        }

        Administrateur admin = new Administrateur();
        admin.setNom("Admin");
        admin.setPrenom("Systeme");
        admin.setEmail(email);
        admin.setMotDePasse(passwordEncoder.encode("Admin@2026"));
        admin.setRole(Role.ADMINISTRATEUR);
        admin.setActif(true);
        admin.setNiveauAcces("SUPER_ADMIN");

        utilisateurRepository.save(admin);
        log.warn("Compte administrateur de demonstration cree : {} / Admin@2026", email);
    }
}
