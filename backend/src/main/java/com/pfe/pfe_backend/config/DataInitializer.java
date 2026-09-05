package com.pfe.pfe_backend.config;

import com.pfe.pfe_backend.domain.Administrateur;
import com.pfe.pfe_backend.domain.ArticleAide;
import com.pfe.pfe_backend.domain.Filiere;
import com.pfe.pfe_backend.domain.Promotion;
import com.pfe.pfe_backend.domain.enums.Role;
import com.pfe.pfe_backend.repository.ArticleAideRepository;
import com.pfe.pfe_backend.repository.FiliereRepository;
import com.pfe.pfe_backend.repository.PromotionRepository;
import com.pfe.pfe_backend.repository.UtilisateurRepository;
import com.pfe.pfe_backend.service.ArticleAideService;
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
    private final ArticleAideRepository articleAideRepository;
    private final ArticleAideService articleAideService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initialiserFilieres();
        initialiserPromotions();
        initialiserAdministrateur();
        initialiserArticlesAide();

        // Vectorisation synchrone au demarrage (Lot 8, etape 8.6) : le
        // corpus est petit (17 articles courts) et ne s'execute que pour
        // les articles pas encore VECTORISE, donc sans cout au demarrage
        // suivant une fois le seed initial vectorise avec succes.
        articleAideService.indexerEnAttente();
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

    /**
     * Corpus documentation/FAQ de l'assistant conversationnel RAG (Lot 8,
     * etape 8.6). Contenu redige a l'etape 8.5, valide par Zakaria
     * (docs/corpus-faq.md) avant ce chargement. Seed idempotent comme le
     * reste de cette classe : ne recree jamais un article dont le titre
     * existe deja, y compris si son contenu a ete modifie depuis (une
     * evolution ulterieure du corpus n'est pas geree ici).
     */
    private void initialiserArticlesAide() {
        for (ArticleSeed seed : ARTICLES_AIDE) {
            if (articleAideRepository.existsByTitre(seed.titre())) {
                continue;
            }
            ArticleAide article = ArticleAide.builder()
                    .categorie(seed.categorie())
                    .titre(seed.titre())
                    .contenu(seed.contenu())
                    .build();
            articleAideRepository.save(article);
            log.info("Article d'aide cree : {}", seed.titre());
        }
    }

    private record ArticleSeed(String categorie, String titre, String contenu) {}

    private static final List<ArticleSeed> ARTICLES_AIDE = List.of(

            new ArticleSeed(
                    "Compte et connexion",
                    "Comment créer mon compte étudiant ?",
                    """
                    Sur la page d'inscription, renseignez votre nom, prénom, email et un mot \
                    de passe d'au moins 8 caractères. Trois champs supplémentaires sont \
                    obligatoires pour un compte étudiant : votre numéro étudiant, votre \
                    filière et votre promotion. Une fois le compte créé, connectez-vous avec \
                    votre email et votre mot de passe pour accéder à votre espace."""),

            new ArticleSeed(
                    "Compte et connexion",
                    "J'ai oublié mon mot de passe, que faire ?",
                    """
                    Il n'existe pas encore de réinitialisation en libre-service sur la \
                    plateforme. Contactez l'administrateur : lui seul peut réinitialiser un \
                    mot de passe."""),

            new ArticleSeed(
                    "Compte et connexion",
                    "Pourquoi suis-je parfois déconnecté sans prévenir ?",
                    """
                    Votre session repose sur un jeton d'accès valable 15 minutes, renouvelé \
                    automatiquement en arrière-plan tant que vous naviguez activement sur la \
                    plateforme. Une déconnexion inattendue signifie généralement une longue \
                    période d'inactivité ou une déconnexion explicite depuis un autre \
                    onglet."""),

            new ArticleSeed(
                    "Sujet et équipe",
                    "Comment créer une équipe et devenir chef d'équipe ?",
                    """
                    Tout étudiant qui n'appartient pas encore à une équipe peut en créer \
                    une ; il en devient automatiquement le chef. Le chef peut ensuite \
                    ajouter des membres par leur numéro étudiant."""),

            new ArticleSeed(
                    "Sujet et équipe",
                    "Comment rejoindre une équipe existante ?",
                    """
                    Deux façons : soit le chef de l'équipe vous ajoute directement avec \
                    votre numéro étudiant, soit vous rejoignez vous-même une équipe ayant \
                    de la place, via le bouton prévu à cet effet — à condition d'appartenir \
                    à la même filière et à la même promotion que cette équipe, et qu'elle ne \
                    soit pas déjà complète."""),

            new ArticleSeed(
                    "Sujet et équipe",
                    "Comment mon équipe obtient-elle un sujet de PFE ?",
                    """
                    Les sujets sont proposés par les encadrants puis examinés par \
                    l'administrateur (statuts : proposé, en validation, puis validé, rejeté \
                    ou à corriger). Une fois un sujet validé, seul le chef de votre équipe \
                    peut demander l'affectation de l'équipe à ce sujet — c'est cette \
                    affectation qui crée le projet officiel de PFE. Une équipe ne peut avoir \
                    qu'un seul projet."""),

            new ArticleSeed(
                    "Sujet et équipe",
                    "Puis-je changer de sujet une fois mon équipe affectée à un projet ?",
                    """
                    Non : une équipe ne peut avoir qu'un seul projet actif. Si vous \
                    rencontrez un problème avec votre sujet, la solution passe par votre \
                    encadrant, pas par une nouvelle affectation."""),

            new ArticleSeed(
                    "Jalons",
                    "Comment soumettre un jalon (checkpoint) ?",
                    """
                    Seul le chef d'équipe peut soumettre un jalon, depuis l'espace jalons du \
                    projet : il fournit un lien vers le livrable correspondant et, s'il le \
                    souhaite, un commentaire. Le jalon passe alors en attente de validation \
                    par l'encadrant, qui peut le valider ou renvoyer un retour."""),

            new ArticleSeed(
                    "Jalons",
                    "Pourquoi mon jalon apparaît-il « en retard » ?",
                    """
                    Ce statut est calculé automatiquement, une fois par jour (tôt le matin), \
                    pour tout jalon dont l'échéance est dépassée sans avoir été validé. Il \
                    n'y a pas de déclenchement manuel : si vous soumettez un jalon après son \
                    échéance, le passage en retard peut prendre jusqu'au lendemain pour se \
                    refléter."""),

            new ArticleSeed(
                    "Documents",
                    "Comment déposer mon rapport ou un livrable ?",
                    """
                    Seul le chef d'équipe peut déposer un document, depuis l'espace \
                    documents du projet, en le rattachant éventuellement à un jalon précis. \
                    Si vous déposez un fichier portant le même nom qu'un document déjà \
                    présent, une nouvelle version est créée automatiquement — l'ancienne \
                    reste consultable dans l'historique, rien n'est écrasé."""),

            new ArticleSeed(
                    "Documents",
                    "Comment consulter les anciennes versions d'un document ?",
                    """
                    Depuis la fiche du document dans l'espace documents, l'historique \
                    complet des versions déposées sous ce nom est accessible, avec la \
                    possibilité de télécharger chacune d'elles."""),

            new ArticleSeed(
                    "Messagerie et notifications",
                    "Comment contacter mon encadrant sur la plateforme ?",
                    """
                    Chaque projet dispose d'une messagerie privée, accessible en lecture et \
                    en écriture uniquement au chef d'équipe et à l'encadrant du projet. Les \
                    nouveaux messages apparaissent automatiquement après quelques secondes, \
                    sans qu'il soit nécessaire de recharger la page."""),

            new ArticleSeed(
                    "Messagerie et notifications",
                    "À quoi sert la cloche de notifications ?",
                    """
                    Elle vous informe des événements qui vous concernent : décision sur un \
                    sujet, jalon soumis ou validé, nouveau document déposé, nouveau message, \
                    ou alerte de similarité (pour un encadrant). Le badge affiche le nombre \
                    de notifications non lues ; vous pouvez marquer une notification comme \
                    lue individuellement, ou tout marquer lu d'un coup."""),

            new ArticleSeed(
                    "Bibliothèque de ressources",
                    "Qu'est-ce que la bibliothèque de ressources et qui peut y déposer ?",
                    """
                    C'est un espace partagé de ressources utiles (liens externes et/ou \
                    fichiers), organisées par catégorie, accessible en consultation à tous \
                    les comptes de la plateforme. Le dépôt d'une nouvelle ressource est \
                    réservé aux encadrants et à l'administrateur."""),

            new ArticleSeed(
                    "Détection de similarité",
                    "Mon rapport est-il vérifié automatiquement contre le plagiat ? Puis-je consulter mon score ?",
                    """
                    Les rapports archivés par l'administrateur comme documents de référence \
                    sont comparés entre eux par une analyse de similarité sémantique, à \
                    l'initiative de l'encadrant. Ce résultat n'est toutefois jamais \
                    accessible à l'étudiant : ni le score, ni le rapport détaillé — seuls \
                    l'encadrant du projet concerné et l'administrateur peuvent le consulter. \
                    Si vous avez une question sur l'originalité attendue de votre travail, \
                    adressez-vous directement à votre encadrant."""),

            new ArticleSeed(
                    "Assistant IA",
                    "Qui répond dans cet assistant ? Est-ce une intelligence artificielle ?",
                    """
                    Oui. Les réponses de cet assistant sont générées automatiquement par un \
                    modèle de langage, à partir uniquement des articles de cette \
                    documentation interne — jamais à partir de vos données personnelles, qui \
                    ne sont pas transmises au modèle. Comme toute production issue de \
                    l'intelligence artificielle sur cette plateforme, ces réponses sont \
                    explicitement signalées comme telles et doivent être vérifiées avant \
                    d'être considérées comme définitives."""),

            new ArticleSeed(
                    "Assistant IA",
                    "Que se passe-t-il si l'assistant ne trouve pas de réponse à ma question ?",
                    """
                    L'assistant ne répond qu'à partir des articles disponibles dans cette \
                    documentation. S'il ne trouve aucun passage suffisamment pertinent pour \
                    votre question, il l'indique honnêtement plutôt que d'inventer une \
                    réponse, et vous propose d'être mis en relation avec votre encadrant.""")
    );
}
