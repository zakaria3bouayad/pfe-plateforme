package com.pfe.pfe_backend.service;

import java.util.List;

/**
 * Callback de streaming pour RagService.repondre (Lot 8, etape 8.7,
 * signature completee a l'etape 8.8).
 *
 * RagService ne connait rien du transport HTTP/SSE : c'est a l'appelant
 * (ChatController, etape 8.8) d'implementer cette interface pour relayer
 * chaque evenement vers le frontend via SseEmitter. Ce decouplage garde
 * RagService testable independamment de la couche web.
 *
 * La question anonymisee est repassee sur surFin/surReponseImpossible
 * (jamais la question brute) : c'est le seul moyen pour l'appelant de la
 * journaliser (etape 8.8, ENF-15) sans avoir a l'anonymiser une seconde
 * fois de son cote - une seule source de verite pour l'anonymisation.
 */
public interface RagCallback {

    /** Un fragment de la reponse generee, dans l'ordre de reception. */
    void surToken(String token);

    /** Fin normale : reponse generee entierement. Titres des articles utilises comme sources (etape 8.11). */
    void surFin(String questionAnonymisee, List<String> sources);

    /**
     * Aucune reponse generee : contexte insuffisant (aucun article
     * suffisamment pertinent) ou service Python injoignable (degradation
     * controlee, ENF-20) - les deux se traduisent de la meme facon pour
     * l'utilisateur. Le message est deja redige ; a l'appelant de proposer
     * l'escalade vers l'encadrant (etape 8.9).
     */
    void surReponseImpossible(String questionAnonymisee, String message);
}
