package com.pfe.pfe_backend.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marque une methode de service comme sujette au journal d'audit (etape
 * 7.7). Interceptee par AuditAspect, qui ecrit une EntreeAudit apres chaque
 * appel, succes ou echec (etape 7.6).
 *
 * Explicite, methode par methode, plutot qu'un aspect applique a tous les
 * controleurs : un journal exhaustif serait illisible, donc inutile
 * (decision assumee). La couverture effective (etape 7.8) est deliberement
 * courte.
 *
 * cible, detail et acteur sont des expressions SpEL evaluees sur les
 * parametres de la methode (accessibles par leur nom, prefixe #), plus
 * #resultat (la valeur retournee, succes uniquement) et #erreur (le message
 * de l'exception, echec uniquement). Toutes sont facultatives ; une
 * expression absente ou en echec d'evaluation ne fait jamais echouer la
 * methode auditee, seulement l'entree de journal correspondante.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audite {

    /**
     * Code de l'action auditee (ex. "SUJET_VALIDE"), stable pour permettre
     * de filtrer le journal (etape 7.9). En cas d'exception, l'aspect
     * enregistre "{action}_ECHEC" a la place.
     */
    String action();

    /**
     * Expression SpEL pour EntreeAudit.acteur. Vide par defaut : l'email de
     * l'utilisateur authentifie courant (SecurityContextHolder) est alors
     * utilise. A fournir explicitement pour une action qui peut survenir
     * hors contexte authentifie (ex. une connexion echouee : "#requete.email()").
     */
    String acteur() default "";

    /** Expression SpEL pour EntreeAudit.cible (ex. "'Sujet#' + #id"). Vide si l'action n'a pas de cible unique. */
    String cible() default "";

    /** Expression SpEL pour EntreeAudit.detail (ex. "#commentaire"). */
    String detail() default "";
}
