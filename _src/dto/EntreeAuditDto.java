package com.pfe.pfe_backend.dto;

import com.pfe.pfe_backend.domain.EntreeAudit;

import java.time.LocalDateTime;

/**
 * Ligne du journal d'audit renvoyee au frontend (etape 7.9).
 */
public record EntreeAuditDto(
        Long id,
        String acteur,
        String action,
        String cible,
        String detail,
        LocalDateTime horodatage,
        String ip
) {
    public static EntreeAuditDto from(EntreeAudit e) {
        return new EntreeAuditDto(
                e.getId(), e.getActeur(), e.getAction(), e.getCible(),
                e.getDetail(), e.getHorodatage(), e.getIp());
    }
}
