package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.Document;
import com.pfe.pfe_backend.domain.IndexationDocument;
import com.pfe.pfe_backend.domain.MorceauDocument;
import com.pfe.pfe_backend.domain.enums.StatutIndexation;
import com.pfe.pfe_backend.dto.IndexationDocumentDto;
import com.pfe.pfe_backend.exception.BusinessException;
import com.pfe.pfe_backend.repository.DocumentRepository;
import com.pfe.pfe_backend.repository.IndexationDocumentRepository;
import com.pfe.pfe_backend.repository.MorceauDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestration de l'indexation d'un document (Lot 6, etape 6.2) :
 * telechargement depuis MinIO, extraction du texte, ecriture du resultat.
 *
 * Complementaire de ExtractionTexteService, qui ne fait que transformer des
 * octets en texte sans rien connaitre du stockage ni de la base.
 *
 * Une seule ligne d'indexation par document : reindexer met a jour la ligne
 * existante. Un echec d'extraction est enregistre comme tel (statut ECHEC ou
 * VIDE) plutot que remonte en erreur HTTP, pour que l'administrateur voie
 * dans l'interface pourquoi un document ne participe pas au corpus.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IndexationService {

    private final DocumentRepository documentRepository;
    private final IndexationDocumentRepository indexationRepository;
    private final MorceauDocumentRepository morceauRepository;
    private final DocumentStorageService documentStorageService;
    private final ExtractionTexteService extractionTexteService;
    private final DecoupageTexteService decoupageTexteService;
    private final EmbeddingClient embeddingClient;

    /**
     * (Re)indexe un document : extrait son texte et enregistre le resultat.
     * Idempotent, l'indexation precedente est ecrasee.
     */
    @Transactional
    public IndexationDocumentDto indexer(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> BusinessException.introuvable("Document introuvable"));
        if (document.isSupprime()) {
            throw BusinessException.introuvable("Document introuvable");
        }
        return IndexationDocumentDto.from(indexer(document));
    }

    /**
     * Variante interne prenant l'entite deja chargee, appelee par
     * DocumentService lors du marquage comme archive.
     */
    @Transactional
    public IndexationDocument indexer(Document document) {
        ExtractionTexteService.ResultatExtraction resultat = extraire(document);

        IndexationDocument indexation = indexationRepository
                .findByDocumentId(document.getId())
                .orElseGet(() -> IndexationDocument.builder().document(document).build());

        indexation.setStatut(resultat.statut());
        indexation.setTexte(resultat.estExploitable() ? resultat.texte() : null);
        indexation.setNbCaracteres(resultat.estExploitable() ? resultat.texte().length() : 0);
        indexation.setNbPages(resultat.nbPages());
        indexation.setTronque(resultat.tronque());
        indexation.setMessage(resultat.message());
        indexation.setNbMorceaux(0);

        indexation = indexationRepository.save(indexation);

        // Les morceaux de l'indexation precedente portent des vecteurs
        // calcules sur un texte qui n'est plus celui-ci : ils doivent
        // disparaitre avant tout recalcul.
        morceauRepository.supprimerParIndexation(indexation.getId());

        if (resultat.estExploitable()) {
            vectoriser(indexation, resultat.texte());
        }

        log.info("Indexation du document {} ({}) : statut={} caracteres={} morceaux={}",
                document.getId(), document.getNom(),
                indexation.getStatut(), indexation.getNbCaracteres(), indexation.getNbMorceaux());

        return indexationRepository.save(indexation);
    }

    /**
     * Decoupe le texte, appelle l'API d'embeddings et enregistre un morceau
     * par vecteur (Lot 6, etape 6.3).
     *
     * Un echec de l'API n'annule pas l'extraction deja reussie : le statut
     * bascule sur ECHEC_EMBEDDING, le texte reste en base, et un simple
     * appel a POST /api/admin/documents/{id}/indexer rejouera la
     * vectorisation sans avoir a retelecharger ni reextraire le PDF.
     */
    private void vectoriser(IndexationDocument indexation, String texte) {
        List<String> morceaux = decoupageTexteService.decouper(texte);
        if (morceaux.isEmpty()) {
            indexation.setStatut(StatutIndexation.VIDE);
            indexation.setMessage("Aucun morceau exploitable apres decoupage");
            return;
        }

        try {
            List<float[]> vecteurs = embeddingClient.vectoriserTout(morceaux);

            List<MorceauDocument> aEnregistrer = new ArrayList<>(morceaux.size());
            for (int i = 0; i < morceaux.size(); i++) {
                aEnregistrer.add(MorceauDocument.builder()
                        .indexation(indexation)
                        .ordre(i)
                        .texte(morceaux.get(i))
                        .vecteur(vecteurs.get(i))
                        .build());
            }
            morceauRepository.saveAll(aEnregistrer);

            indexation.setStatut(StatutIndexation.VECTORISE);
            indexation.setNbMorceaux(aEnregistrer.size());
            indexation.setMessage(null);

        } catch (BusinessException e) {
            log.warn("Vectorisation du document {} impossible : {}",
                    indexation.getDocument().getId(), e.getMessage());
            indexation.setStatut(StatutIndexation.ECHEC_EMBEDDING);
            indexation.setNbMorceaux(0);
            indexation.setMessage(tronquerMessage(e.getMessage()));
        }
    }

    /** Etat de l'indexation d'un document. 404 si le document n'a jamais ete indexe. */
    @Transactional(readOnly = true)
    public IndexationDocumentDto consulter(Long documentId) {
        return indexationRepository.findByDocumentId(documentId)
                .map(IndexationDocumentDto::from)
                .orElseThrow(() -> BusinessException.introuvable(
                        "Ce document n'a pas encore ete indexe"));
    }

    /** Supprime l'indexation d'un document (demarquage comme archive, suppression du document). */
    @Transactional
    public void supprimerIndexation(Long documentId) {
        indexationRepository.deleteByDocumentId(documentId);
    }

    // ------------------------------------------------------------ prive

    /**
     * Telecharge l'objet MinIO et delegue l'extraction. Une panne de stockage
     * est convertie en resultat ECHEC : l'indexation doit rester tracable
     * meme quand MinIO est indisponible.
     */
    private ExtractionTexteService.ResultatExtraction extraire(Document document) {
        try (InputStream flux = documentStorageService.telechargerObjet(document.getCheminMinio())) {
            byte[] contenu = flux.readAllBytes();
            return extractionTexteService.extraire(contenu, document.getType(), document.getNom());
        } catch (IOException | BusinessException e) {
            log.warn("Lecture MinIO impossible pour le document {} : {}", document.getId(), e.toString());
            return new ExtractionTexteService.ResultatExtraction(
                    StatutIndexation.ECHEC, "", null, false,
                    tronquerMessage("Fichier illisible depuis le stockage : " + e.getMessage()));
        }
    }

    /** La colonne message est limitee a 500 caracteres. */
    private String tronquerMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
