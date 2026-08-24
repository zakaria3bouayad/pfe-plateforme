package com.pfe.pfe_backend.audit;

import com.pfe.pfe_backend.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Intercepte les methodes annotees @Audite (etape 7.7).
 *
 * Ecrit une entree apres chaque appel, succes ou echec (via @Around, la
 * seule forme de conseil qui voit les deux issues). Ne modifie jamais le
 * resultat ni ne bloque l'appel : toute erreur pendant la construction ou
 * l'ecriture de l'entree (y compris une expression SpEL mal formee ou
 * referencant un parametre absent) est journalisee en warn et avalee -
 * jamais propagee a la methode auditee. Un journal d'audit qui casserait la
 * validation d'un sujet parce qu'une expression est mal ecrite serait pire
 * que l'absence de journal.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(audite)")
    public Object autourDeLaMethodeAuditee(ProceedingJoinPoint pointDeJonction, Audite audite) throws Throwable {
        try {
            Object resultat = pointDeJonction.proceed();
            enregistrer(pointDeJonction, audite, audite.action(), resultat, null);
            return resultat;
        } catch (Throwable erreur) {
            enregistrer(pointDeJonction, audite, audite.action() + "_ECHEC", null, erreur);
            throw erreur;
        }
    }

    private void enregistrer(ProceedingJoinPoint pointDeJonction, Audite audite,
                              String action, Object resultat, Throwable erreur) {
        try {
            EvaluationContext contexte = construireContexte(pointDeJonction, resultat, erreur);

            String acteur = !audite.acteur().isBlank()
                    ? evaluer(audite.acteur(), contexte)
                    : acteurConnecte();
            String cible = !audite.cible().isBlank() ? evaluer(audite.cible(), contexte) : null;
            String detail = !audite.detail().isBlank() ? evaluer(audite.detail(), contexte) : null;

            auditService.enregistrer(acteur, action, cible, detail);
        } catch (Exception e) {
            log.warn("Ecriture d'audit ignoree pour {} ({}) : {}",
                    pointDeJonction.getSignature().toShortString(), action, e.getMessage());
        }
    }

    /** Lie chaque parametre de la methode a son nom, plus #resultat et #erreur selon l'issue de l'appel. */
    private EvaluationContext construireContexte(ProceedingJoinPoint pointDeJonction, Object resultat, Throwable erreur) {
        StandardEvaluationContext contexte = new StandardEvaluationContext();

        MethodSignature signature = (MethodSignature) pointDeJonction.getSignature();
        Method methode = signature.getMethod();
        String[] noms = parameterNameDiscoverer.getParameterNames(methode);
        Object[] valeurs = pointDeJonction.getArgs();

        if (noms != null) {
            for (int i = 0; i < noms.length && i < valeurs.length; i++) {
                contexte.setVariable(noms[i], valeurs[i]);
            }
        }
        if (resultat != null) {
            contexte.setVariable("resultat", resultat);
        }
        if (erreur != null) {
            contexte.setVariable("erreur", erreur.getMessage());
        }
        return contexte;
    }

    private String evaluer(String expression, EvaluationContext contexte) {
        Object valeur = parser.parseExpression(expression).getValue(contexte);
        return valeur != null ? valeur.toString() : null;
    }

    /** Email de l'utilisateur authentifie courant, ou "anonyme" hors contexte de securite. */
    private String acteurConnecte() {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        return authentification != null ? authentification.getName() : "anonyme";
    }
}
