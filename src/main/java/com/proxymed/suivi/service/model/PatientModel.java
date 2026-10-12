package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.Sexe;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Couche interne neutre : ni le contrat API (PatientRequest/Response), ni l'entite JPA.
 */
@Builder(toBuilder = true)
public record PatientModel(
        UUID id,
        String numeroDossierProxymed,
        String numeroDmi,
        String nomComplet,
        LocalDate dateNaissance,
        Integer age,
        Sexe sexe,
        String telephone,
        String adresseDomicile,
        String commune,
        String quartier
) {
}
