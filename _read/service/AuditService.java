package com.pfe.pfe_backend.service;

import com.pfe.pfe_backend.domain.EntreeAudit;
import com.pfe.pfe_backend.dto.EntreeAuditDto;
import com.pfe.pfe_backend.repository.EntreeAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Ecriture et consultation du journal d'audit (etapes 7.7 et 7.9).
 *
 * REQUIRES_NEW (etape 7.6) : une entree d'audit doit survivre au rollback de
 * l'action auditee - une validation de sujet qui echoue en base doit tout de
 * meme laisser une trace de la tentative. L'ecrire dans la transaction de
 * l'action auditee la ferait disparaitre avec elle en cas de rollback.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final EntreeAuditRepository entreeAuditRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(String acteur, String action, String cible, String detail) {
        EntreeAudit entree = EntreeAudit.builder()
                .acteur(acteur)
                .action(action)
                .cible(cible)
                .detail(detail)
                .ip(ipCourante())
                .build();
        entreeAuditRepository.save(entree);
    }

    /**
     * Consultation paginee du journal pour AuditController (etape 7.9).
     * Chaque filtre est facultatif ; voir EntreeAuditRepository.rechercher.
     */
    @Transactional(readOnly = true)
    public Page<EntreeAuditDto> rechercher(
            String acteur, String action, LocalDateTime depuis, LocalDateTime jusqua, Pageable pageable) {
        return entreeAuditRepository.rechercher(acteur, action, depuis, jusqua, pageable)
                .map(EntreeAuditDto::from);
    }

    /** IP du client HTTP a l'origine de l'appel courant, absente hors requete web (ex. tache planifiee). */
    private String ipCourante() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getRemoteAddr();
        }
        return null;
    }
}
