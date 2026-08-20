package com.pfe.pfe_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Decoupage d'un texte en morceaux vectorisables (Lot 6, etape 6.3).
 *
 * Deux reglages, tous deux configurables :
 *
 *  - la longueur d'un morceau. Plus il est court, plus la detection est fine
 *    (un paragraphe recopie ressort), mais plus il faut d'appels a l'API.
 *    6000 caracteres (environ deux pages) est un compromis : assez large
 *    pour rester sous la limite de 8192 tokens du modele meme dans le pire
 *    cas d'un texte dense, assez etroit pour qu'un chapitre plagie ne soit
 *    pas noye.
 *
 *  - le chevauchement entre morceaux consecutifs. Sans lui, un passage
 *    copie tombant a cheval sur une frontiere serait coupe en deux moities
 *    dont aucune ne ressemblerait franchement a l'original.
 *
 * Le decoupage cherche une frontiere de phrase pres de la coupe theorique,
 * plutot que de trancher au milieu d'un mot : un morceau commencant en
 * plein milieu d'une phrase produit un vecteur bruite.
 */
@Service
@Slf4j
public class DecoupageTexteService {

    /** Fenetre de recherche d'une fin de phrase autour de la coupe theorique. */
    private static final int MARGE_FRONTIERE = 300;

    @Value("${similarite.morceau.longueur:6000}")
    private int longueurMorceau;

    @Value("${similarite.morceau.chevauchement:600}")
    private int chevauchement;

    /**
     * Decoupe le texte en morceaux successifs et chevauchants.
     * Renvoie une liste vide si le texte est vide.
     */
    public List<String> decouper(String texte) {
        List<String> morceaux = new ArrayList<>();
        if (texte == null || texte.isBlank()) {
            return morceaux;
        }

        int longueur = Math.max(500, longueurMorceau);
        int recouvrement = Math.max(0, Math.min(chevauchement, longueur / 2));
        int pas = longueur - recouvrement;

        int debut = 0;
        while (debut < texte.length()) {
            int finTheorique = Math.min(debut + longueur, texte.length());
            int fin = finTheorique == texte.length()
                    ? finTheorique
                    : chercherFinDePhrase(texte, finTheorique);

            String morceau = texte.substring(debut, fin).trim();
            if (!morceau.isBlank()) {
                morceaux.add(morceau);
            }

            if (fin >= texte.length()) {
                break;
            }
            debut += pas;
        }

        log.debug("Texte de {} caracteres decoupe en {} morceaux", texte.length(), morceaux.size());
        return morceaux;
    }

    // ------------------------------------------------------------ prive

    /**
     * Recule jusqu'a la derniere fin de phrase situee dans la marge avant la
     * coupe theorique. A defaut, recule jusqu'au dernier espace. A defaut
     * encore (mot interminable, texte sans ponctuation), coupe net.
     */
    private int chercherFinDePhrase(String texte, int finTheorique) {
        int borneBasse = Math.max(0, finTheorique - MARGE_FRONTIERE);

        for (int i = finTheorique - 1; i > borneBasse; i--) {
            char c = texte.charAt(i);
            if ((c == '.' || c == '!' || c == '?') && i + 1 < texte.length() && texte.charAt(i + 1) == ' ') {
                return i + 1;
            }
        }
        int dernierEspace = texte.lastIndexOf(' ', finTheorique);
        return dernierEspace > borneBasse ? dernierEspace : finTheorique;
    }
}
