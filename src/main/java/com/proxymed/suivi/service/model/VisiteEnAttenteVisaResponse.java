package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.DecisionVisite;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Ligne de la liste des fiches terminees en attente du visa du medecin senior
 * (le detail complet se lit par GET /api/visites/{id}).
 */
@Builder(toBuilder = true)
public record VisiteEnAttenteVisaResponse(
        UUID id,
        Integer numeroVisite,
        LocalDate dateVisite,
        UUID patientId,
        String patientNomComplet,
        String numeroDossierProxymed,
        Long medecinJuniorId,
        String medecinJuniorNomComplet,
        Long medecinSeniorId,
        String medecinSeniorNomComplet,
        DecisionVisite decision,
        boolean alerteDeclenchee,
        Instant dateSignatureJunior,
        Instant echeanceVisa,
        boolean visaEnRetard
) {
}
