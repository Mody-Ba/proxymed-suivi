package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.StatutVisite;
import com.proxymed.suivi.enums.TypeVisite;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Ligne de la liste des visites d'un patient (le detail complet se lit par GET /api/visites/{id}).
 */
@Builder(toBuilder = true)
public record VisiteSuiviSummaryResponse(
        UUID id,
        Integer numeroVisite,
        LocalDate dateVisite,
        TypeVisite typeVisite,
        StatutVisite statut,
        Long medecinJuniorId,
        String medecinJuniorNomComplet,
        Long medecinSeniorId,
        String medecinSeniorNomComplet
) {
}
