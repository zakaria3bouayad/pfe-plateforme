package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.enums.StatutIndexation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Extraction de la couche texte d'un PDF via Apache PDFBox
 * (Lot 6, etape 6.2).
 *
 * Ne connait ni la base ni MinIO : recoit des octets, rend un resultat.
 * Cette separation permet de reutiliser l'extraction telle quelle a
 * l'etape 6.4, pour le rapport candidat qu'on compare au corpus sans
 * jamais l'archiver.
 *
 * Aucune exception n'est propagee : un PDF illisible est un cas metier
 * normal (fichier corrompu, protege par mot de passe, document scanne),
 * pas une erreur technique. Le resultat porte le statut correspondant.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExtractionTexteService {

    private static final String TYPE_PDF = "application/pdf";

    /**
     * Longueur maximale de texte conservee. Une these de 300 pages tourne
     * autour de 600 000 caracteres ; au-dela on est face a un fichier
     * anormal, et le texte serait de toute facon tronque par le decoupage
     * en entree du modele d'embeddings (etape 6.3).
     */
    @Value("${similarite.texte.longueur-max:1000000}")
    private int longueurMax;

    /**
     * Resultat d'une extraction.
     *
     * @param statut        issue de l'extraction
     * @param texte         texte normalise, vide si le statut n'est pas EXTRAIT
     * @param nbPages       nombre de pages du PDF, null si le fichier n'a pas pu etre ouvert
     * @param tronque       vrai si le texte a ete coupe a la longueur maximale
     * @param message       cause de l'echec, null si l'extraction a abouti
     */
    public record ResultatExtraction(
            StatutIndexation statut,
            String texte,
            Integer nbPages,
            boolean tronque,
            String message) {

        public boolean estExploitable() {
            return statut == StatutIndexation.EXTRAIT;
        }
    }

    /**
     * Extrait le texte d'un fichier.
     *
     * @param contenu     octets du fichier
     * @param typeContenu type MIME declare a l'upload
     * @param nomFichier  nom d'origine, utilise uniquement pour les traces
     */
    public ResultatExtraction extraire(byte[] contenu, String typeContenu, String nomFichier) {
        if (!estPdf(typeContenu, nomFichier)) {
            return new ResultatExtraction(
                    StatutIndexation.TYPE_NON_SUPPORTE, "", null, false,
                    "Type non supporte : " + typeContenu + " (seul le PDF est traite)");
        }
        if (contenu == null || contenu.length == 0) {
            return new ResultatExtraction(
                    StatutIndexation.ECHEC, "", null, false, "Fichier vide");
        }

        try (PDDocument pdf = Loader.loadPDF(contenu)) {
            int nbPages = pdf.getNumberOfPages();

            PDFTextStripper extracteur = new PDFTextStripper();
            // Reordonne les fragments selon leur position sur la page : sans
            // cela, un PDF en deux colonnes ressort entrelace, ce qui fausse
            // completement l'embedding calcule ensuite.
            extracteur.setSortByPosition(true);
            String brut = extracteur.getText(pdf);

            String texte = normaliser(brut);
            if (texte.isBlank()) {
                return new ResultatExtraction(
                        StatutIndexation.VIDE, "", nbPages, false,
                        "Aucune couche texte : document probablement scanne (pas d'OCR)");
            }

            boolean tronque = texte.length() > longueurMax;
            if (tronque) {
                texte = texte.substring(0, longueurMax);
                log.warn("Texte tronque a {} caracteres pour le fichier {}", longueurMax, nomFichier);
            }

            return new ResultatExtraction(StatutIndexation.EXTRAIT, texte, nbPages, tronque, null);

        } catch (Exception e) {
            log.warn("Extraction PDF impossible pour {} : {}", nomFichier, e.toString());
            return new ResultatExtraction(
                    StatutIndexation.ECHEC, "", null, false, tronquerMessage(e));
        }
    }

    // ------------------------------------------------------------ prive

    /**
     * Le type MIME vient du navigateur et n'est pas fiable : on accepte donc
     * aussi l'extension .pdf, sans quoi un depot annonce en
     * application/octet-stream serait ecarte a tort.
     */
    private boolean estPdf(String typeContenu, String nomFichier) {
        if (typeContenu != null && typeContenu.toLowerCase().startsWith(TYPE_PDF)) {
            return true;
        }
        return nomFichier != null && nomFichier.toLowerCase().endsWith(".pdf");
    }

    /**
     * Normalise les blancs : PDFBox produit beaucoup de sauts de ligne et
     * d'espaces lies a la mise en page, qui ne portent aucun sens et
     * ajouteraient du bruit au vecteur d'embedding.
     */
    private String normaliser(String brut) {
        if (brut == null) {
            return "";
        }
        // L'espace insecable n'est pas couvert par \s en Java : il faut le
        // convertir en espace ordinaire avant la normalisation.
        return brut.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** La colonne message est limitee a 500 caracteres. */
    private String tronquerMessage(Exception e) {
        String message = e.getClass().getSimpleName()
                + (e.getMessage() != null ? " : " + e.getMessage() : "");
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
