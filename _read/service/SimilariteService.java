package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.audit.Audite;
import com.pfe.pfe_backend.domain.*;
import com.pfe.pfe_backend.domain.enums.NiveauSimilarite;
import com.pfe.pfe_backend.domain.enums.Role;
import com.pfe.pfe_backend.domain.enums.StatutIndexation;
import com.pfe.pfe_backend.domain.enums.TypeNotification;
import com.pfe.pfe_backend.dto.RapportSimilariteDto;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Detection de similarite entre un rapport depose et le corpus des rapports
 * archives (Lot 6, etape 6.4).
 *
 * Principe : chaque morceau du document analyse est compare, par similarite
 * cosinus, aux morceaux du corpus. On ne conserve ensuite que la meilleure
 * correspondance par document archive, et le score du rapport est le maximum
 * de ces scores.
 *
 * Pourquoi le maximum et non la moyenne. Le plagiat reel est local : on
 * recopie un chapitre, rarement un rapport entier. Une moyenne sur
 * l'ensemble des morceaux diluerait completement ce signal - un rapport de
 * dix sections dont une est copiee afficherait une moyenne parfaitement
 * rassurante. Le maximum, lui, remonte le passage le plus problematique,
 * qui est precisement ce que l'encadrant doit lire.
 *
 * Ce que ce service ne fait pas : il ne conclut pas. Il classe des passages
 * par proximite decroissante et laisse le jugement a l'humain.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SimilariteService {

    private final DocumentRepository documentRepository;
    private final ProjetRepository projetRepository;
    private final IndexationDocumentRepository indexationRepository;
    private final MorceauDocumentRepository morceauRepository;
    private final RapportSimilariteRepository rapportRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final IndexationService indexationService;
    private final NotificationService notificationService;
    private final MailService mailService;

    @Value("${similarite.seuil.suspect:0.90}")
    private double seuilSuspect;

    @Value("${similarite.seuil.attention:0.80}")
    private double seuilAttention;

    /**
     * Nombre de voisins remontes par morceau analyse. Volontairement
     * superieur au nombre de documents attendus dans la reponse : plusieurs
     * morceaux d'une meme archive peuvent occuper les premieres places, et
     * on veut malgre tout voir apparaitre d'autres documents.
     */
    @Value("${similarite.voisins-par-morceau:10}")
    private int voisinsParMorceau;

    /**
     * Plancher de conservation. En dessous, une correspondance n'apprend
     * rien et encombrerait le rapport ; seule la meilleure est gardee dans
     * ce cas, pour que le rapport ne soit jamais vide.
     */
    @Value("${similarite.score-minimum-conserve:0.60}")
    private double scoreMinimumConserve;

    // ------------------------------------------------------------ public

    /**
     * Analyse un document contre le corpus archive et enregistre le rapport.
     *
     * Le document est indexe au prealable s'il ne l'est pas encore, ou si
     * son indexation precedente n'a pas abouti : l'analyse d'un rapport
     * depose ne suppose pas qu'il ait ete archive, seulement qu'on puisse
     * le vectoriser.
     *
     * Couverture du journal d'audit (etape 7.8) : analyse de similarite.
     */
    @Audite(action = "SIMILARITE_ANALYSEE", cible = "'Document#' + #documentId",
            detail = "#resultat != null ? 'Niveau ' + #resultat.niveau() : null")
    @Transactional
    public RapportSimilariteDto analyser(String emailDemandeur, Long documentId) {
        Document document = trouverDocumentActif(documentId);
        verifierAcces(document, emailDemandeur);

        // Un document archive peut etre analyse comme les autres. Le seul
        // risque, celui de se comparer a soi-meme, est deja ecarte par le
        // filtre d.id <> :documentExclu de la recherche vectorielle. En
        // revanche l'interdire empecherait de verifier qu'une archive n'est
        // pas elle-meme une copie d'une autre archive - controle utile avant
        // qu'un rapport ne serve de reference a toute une promotion - et
        // obligerait a demarquer puis remarquer, ce qui detruit l'indexation
        // et impose de refaire tous les appels a l'API d'embeddings.
        IndexationDocument indexation = preparerIndexation(document);
        List<MorceauDocument> morceaux = morceauRepository
                .findByIndexationIdOrderByOrdreAsc(indexation.getId());

        if (morceaux.isEmpty()) {
            throw new BusinessException(
                    "Aucun contenu vectorise pour ce document : analyse impossible");
        }

        Map<Long, MeilleureCorrespondance> parArchive = comparerAuCorpus(morceaux, documentId);
        RapportSimilarite rapport = construireRapport(
                document, emailDemandeur, morceaux.size(), parArchive);

        log.info("Analyse du document {} : score max {} ({}), {} archives comparees",
                documentId, rapport.getScoreMax(), rapport.getNiveau(),
                rapport.getNbDocumentsCompares());

        RapportSimilarite rapportEnregistre = rapportRepository.save(rapport);

        if (rapportEnregistre.getNiveau() == NiveauSimilarite.SUSPECT) {
            notifierSuspect(document, rapportEnregistre);
        }

        return RapportSimilariteDto.from(rapportEnregistre);
    }

    /** Derniere analyse en date d'un document. */
    @Transactional(readOnly = true)
    public RapportSimilariteDto dernierRapport(String emailDemandeur, Long documentId) {
        Document document = trouverDocumentActif(documentId);
        verifierAcces(document, emailDemandeur);

        return rapportRepository.findFirstByDocumentIdOrderByDateAnalyseDesc(documentId)
                .map(RapportSimilariteDto::from)
                .orElseThrow(() -> BusinessException.introuvable(
                        "Aucune analyse de similarite pour ce document"));
    }

    /** Rapport precis, par son identifiant. */
    @Transactional(readOnly = true)
    public RapportSimilariteDto consulter(String emailDemandeur, Long rapportId) {
        RapportSimilarite rapport = rapportRepository.findById(rapportId)
                .orElseThrow(() -> BusinessException.introuvable("Rapport de similarite introuvable"));
        verifierAcces(rapport.getDocument(), emailDemandeur);
        return RapportSimilariteDto.from(rapport);
    }

    /** Historique des analyses d'un document, en resume. */
    @Transactional(readOnly = true)
    public List<RapportSimilariteDto> historique(String emailDemandeur, Long documentId) {
        Document document = trouverDocumentActif(documentId);
        verifierAcces(document, emailDemandeur);

        return rapportRepository.findByDocumentIdOrderByDateAnalyseDesc(documentId).stream()
                .map(RapportSimilariteDto::resume).toList();
    }

    /** Rapports d'un projet, en resume. */
    @Transactional(readOnly = true)
    public List<RapportSimilariteDto> parProjet(String emailDemandeur, Long projetId) {
        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> BusinessException.introuvable("Projet introuvable"));
        verifierAccesProjet(projet, emailDemandeur);

        return rapportRepository.findParProjet(projetId).stream()
                .map(RapportSimilariteDto::resume).toList();
    }

    /** Cas a examiner pour un encadrant : niveaux ATTENTION et SUSPECT, du plus fort au plus faible. */
    @Transactional(readOnly = true)
    public List<RapportSimilariteDto> aExaminerPourEncadrant(String emailEncadrant) {
        Utilisateur encadrant = utilisateurRepository.findByEmail(emailEncadrant)
                .orElseThrow(() -> BusinessException.introuvable("Encadrant introuvable"));

        return rapportRepository.findParEncadrantEtNiveaux(
                        encadrant.getId(),
                        List.of(NiveauSimilarite.SUSPECT, NiveauSimilarite.ATTENTION)).stream()
                .map(RapportSimilariteDto::resume).toList();
    }

    // ------------------------------------------------------------ prive

    /**
     * Restreint l'acces a un rapport a l'encadrant du projet concerne et a
     * l'administrateur.
     *
     * Volontairement plus strict que le reste de l'API, ou les GET sur les
     * documents et commentaires restent ouverts a tout compte authentifie
     * (dette technique heritee du lot 4). Un rapport de similarite met en
     * cause un travail nomme : le laisser lisible par n'importe quel etudiant
     * inscrit reviendrait a diffuser une suspicion de plagiat avant meme
     * qu'un humain l'ait examinee.
     *
     * L'etudiant auteur est exclu lui aussi, y compris de ses propres
     * rapports : pouvoir relancer l'analyse et lire le score permettrait de
     * retoucher un texte jusqu'a passer sous le seuil, ce qui retournerait
     * l'outil contre son objet.
     */
    private void verifierAcces(Document document, String email) {
        verifierAccesProjet(document.getProjet(), email);
    }

    private void verifierAccesProjet(Projet projet, String email) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.introuvable("Utilisateur introuvable"));

        if (utilisateur.getRole() == Role.ADMINISTRATEUR) {
            return;
        }
        if (projet.getEncadrant() != null
                && projet.getEncadrant().getEmail().equalsIgnoreCase(email)) {
            return;
        }
        throw BusinessException.interdit(
                "Seul l'encadrant du projet ou un administrateur peut consulter "
                        + "les rapports de similarite");
    }

    private Document trouverDocumentActif(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> BusinessException.introuvable("Document introuvable"));
        if (document.isSupprime()) {
            throw BusinessException.introuvable("Document introuvable");
        }
        return document;
    }

    /**
     * Garantit que le document dispose de vecteurs exploitables. Reindexe si
     * necessaire, puis verifie le resultat : un PDF scanne ou illisible doit
     * produire une erreur explicite plutot qu'un rapport vide qu'on prendrait
     * a tort pour un verdict rassurant.
     */
    private IndexationDocument preparerIndexation(Document document) {
        IndexationDocument indexation = indexationRepository
                .findByDocumentId(document.getId())
                .orElse(null);

        if (indexation == null || indexation.getStatut() != StatutIndexation.VECTORISE) {
            indexation = indexationService.indexer(document);
        }

        if (indexation.getStatut() != StatutIndexation.VECTORISE) {
            throw new BusinessException(
                    "Le document n'a pas pu etre vectorise (" + indexation.getStatut() + ") : "
                            + indexation.getMessage());
        }
        return indexation;
    }

    /**
     * Interroge pgvector pour chaque morceau et ne retient que la meilleure
     * correspondance par document archive.
     */
    private Map<Long, MeilleureCorrespondance> comparerAuCorpus(
            List<MorceauDocument> morceaux, Long documentExclu) {

        Map<Long, MeilleureCorrespondance> parArchive = new LinkedHashMap<>();

        for (MorceauDocument morceau : morceaux) {
            String vecteur = formaterVecteur(morceau.getVecteur());
            if (vecteur == null) {
                continue;
            }

            List<MorceauDocumentRepository.MorceauProche> proches =
                    morceauRepository.chercherPlusProches(vecteur, documentExclu, voisinsParMorceau);

            for (MorceauDocumentRepository.MorceauProche proche : proches) {
                double score = proche.getScore() != null ? proche.getScore() : 0d;
                MeilleureCorrespondance existante = parArchive.get(proche.getDocumentId());

                if (existante == null || score > existante.score()) {
                    parArchive.put(proche.getDocumentId(), new MeilleureCorrespondance(
                            proche.getDocumentId(),
                            proche.getDocumentNom(),
                            score,
                            morceau.getOrdre(),
                            proche.getOrdre() != null ? proche.getOrdre() : 0,
                            morceau.getTexte(),
                            proche.getTexte()));
                }
            }
        }
        return parArchive;
    }

    private RapportSimilarite construireRapport(
            Document document, String emailDemandeur, int nbMorceaux,
            Map<Long, MeilleureCorrespondance> parArchive) {

        List<MeilleureCorrespondance> retenues = parArchive.values().stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .toList();

        double scoreMax = retenues.isEmpty() ? 0d : retenues.get(0).score();

        Utilisateur demandeur = utilisateurRepository.findByEmail(emailDemandeur).orElse(null);

        RapportSimilarite rapport = RapportSimilarite.builder()
                .document(document)
                .demandePar(demandeur)
                .scoreMax(scoreMax)
                .niveau(niveauPour(scoreMax))
                .nbDocumentsCompares(parArchive.size())
                .nbMorceauxAnalyses(nbMorceaux)
                .seuilSuspect(seuilSuspect)
                .seuilAttention(seuilAttention)
                .correspondances(new ArrayList<>())
                .build();

        // On conserve les correspondances au-dessus du plancher, et dans tous
        // les cas la meilleure : un rapport sans aucune ligne ne permettrait
        // pas de verifier que la comparaison a bien eu lieu.
        for (int i = 0; i < retenues.size(); i++) {
            MeilleureCorrespondance c = retenues.get(i);
            if (i > 0 && c.score() < scoreMinimumConserve) {
                break;
            }
            rapport.ajouterCorrespondance(CorrespondanceSimilarite.builder()
                    .documentArchive(documentRepository.getReferenceById(c.documentId()))
                    .documentArchiveNom(c.documentNom())
                    .score(c.score())
                    .ordreMorceauAnalyse(c.ordreAnalyse())
                    .ordreMorceauArchive(c.ordreArchive())
                    .extraitAnalyse(tronquerExtrait(c.texteAnalyse()))
                    .extraitArchive(tronquerExtrait(c.texteArchive()))
                    .build());
        }
        return rapport;
    }

    private NiveauSimilarite niveauPour(double score) {
        if (score >= seuilSuspect) {
            return NiveauSimilarite.SUSPECT;
        }
        if (score >= seuilAttention) {
            return NiveauSimilarite.ATTENTION;
        }
        return NiveauSimilarite.AUCUN;
    }

    /** Serialise un float[] au format litteral attendu par pgvector : [0.1,0.2,...]. */
    private String formaterVecteur(float[] vecteur) {
        if (vecteur == null || vecteur.length == 0) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (float valeur : vecteur) {
            joiner.add(Float.toString(valeur));
        }
        return joiner.toString();
    }

    private String tronquerExtrait(String texte) {
        if (texte == null) {
            return null;
        }
        int max = CorrespondanceSimilarite.LONGUEUR_EXTRAIT;
        return texte.length() > max ? texte.substring(0, max) + "..." : texte;
    }

    /**
     * Similarite SUSPECT (etape 7.4) : seul niveau notifie parmi les trois -
     * ATTENTION reste visible via "a examiner" (etape 6.4) sans alerter
     * immediatement. Dans la liste courte des mails (decision assumee).
     */
    private void notifierSuspect(Document document, RapportSimilarite rapport) {
        Utilisateur encadrant = document.getProjet().getEncadrant();
        if (encadrant == null) {
            return;
        }
        String message = "Similarite SUSPECT detectee sur le document \"" + document.getNom()
                + "\" (score " + String.format("%.2f", rapport.getScoreMax()) + ").";
        String lien = "/projets/" + document.getProjet().getId() + "/similarite/" + rapport.getId();

        notificationService.creer(encadrant, TypeNotification.SIMILARITE_SUSPECT, message, lien);
        mailService.envoyer(encadrant.getEmail(), "PFE - Similarite suspecte detectee", message);
    }

    /** Meilleure correspondance retenue pour un document archive donne. */
    private record MeilleureCorrespondance(
            Long documentId,
            String documentNom,
            double score,
            int ordreAnalyse,
            int ordreArchive,
            String texteAnalyse,
            String texteArchive) {}
}
