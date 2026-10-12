package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.TypeVisite;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Payload pour creer une visite en BROUILLON (section 1 : identification de la visite).
 * Le patient vient de l'URL ; le numero de visite, l'age et le sexe ne sont jamais saisis.
 */
@Builder(toBuilder = true)
public record VisiteSuiviCreateRequest(
        @NotNull(message = "le medecin junior est obligatoire")
        Long medecinJuniorId,

        @NotNull(message = "le medecin senior est obligatoire")
        Long medecinSeniorId,

        LocalDate dateVisite,
        LocalTime heureArrivee,
        LocalTime heureDepart,
        String adresseVisite,
        String contactSenior,
        Boolean contactSeniorJoignable,
        TypeVisite typeVisite
) {
}
