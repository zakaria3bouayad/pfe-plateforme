package com.pfe.pfe_backend.domain.enums;

/**
 * Verdict d'une analyse de similarite (Lot 6, etape 6.4).
 *
 * Volontairement en trois niveaux et non en booleen. Les scores issus
 * d'embeddings ne presentent aucune rupture nette entre "original" et
 * "copie" : deux rapports du meme domaine, ecrits independamment, se
 * ressemblent deja notablement. Trancher au couteau sur un seuil unique
 * produirait des accusations infondees. Le niveau ATTENTION signale une
 * proximite a verifier sans rien affirmer, et laisse le jugement a
 * l'encadrant.
 *
 * IMPORTANT : aucun de ces niveaux ne constitue une preuve de plagiat. Ils
 * designent des passages a examiner humainement.
 */
public enum NiveauSimilarite {

    /** Aucune proximite notable avec le corpus archive. */
    AUCUN,

    /** Proximite reelle mais explicable par un domaine commun : a verifier. */
    ATTENTION,

    /** Proximite trop forte pour etre fortuite : passage a examiner en priorite. */
    SUSPECT
}
