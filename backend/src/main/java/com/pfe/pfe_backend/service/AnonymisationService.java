package com.pfe.pfe_backend.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Anonymisation best-effort du texte envoye a un service externe (Lot 8,
 * etape 8.7 - cahier des charges ENF-15 : "aucune donnee nominative n'est
 * transmise a un service externe ; les textes sont anonymises avant
 * traitement").
 *
 * Detection par motifs structures (email, telephone, numero etudiant)
 * uniquement - PAS de detection de noms propres en texte libre, qui
 * demanderait une analyse linguistique hors de portee ici. Limite assumee
 * et documentee : un nom mentionne en toutes lettres dans une question
 * n'est pas filtre par ce service.
 *
 * Volontairement permissif plutot que precis : mieux vaut sur-masquer un
 * nombre qui n'etait pas une donnee personnelle que laisser passer un
 * email ou un numero de telephone.
 */
@Service
public class AnonymisationService {

    private static final Pattern EMAIL =
            Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");

    /** Numeros de telephone marocains/internationaux courants : +212612345678, 0612345678, 06 12 34 56 78... */
    private static final Pattern TELEPHONE =
            Pattern.compile("(\\+\\d{1,3}[\\s.-]?)?0?\\d(?:[\\s.-]?\\d{2}){4}");

    /** Numero etudiant : une lettre suivie de plusieurs chiffres (ex. E100), format observe a l'inscription (RegisterRequest.numeroEtudiant). */
    private static final Pattern NUMERO_ETUDIANT =
            Pattern.compile("\\b[A-Za-z]\\d{2,10}\\b");

    /**
     * Remplace les motifs de donnees personnelles detectes par un jeton
     * neutre. Ne modifie jamais la longueur globale de facon a rester
     * lisible pour un humain relisant les logs, mais ce n'est pas garanti
     * caractere pour caractere.
     */
    public String anonymiser(String texte) {
        if (texte == null) {
            return null;
        }
        String resultat = EMAIL.matcher(texte).replaceAll("[email masque]");
        resultat = TELEPHONE.matcher(resultat).replaceAll("[telephone masque]");
        resultat = NUMERO_ETUDIANT.matcher(resultat).replaceAll("[identifiant masque]");
        return resultat;
    }
}
