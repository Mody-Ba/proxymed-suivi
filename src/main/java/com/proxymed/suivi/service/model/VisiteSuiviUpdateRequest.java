package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.TypeVisite;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Mise a jour partielle des champs scalaires d'une visite en BROUILLON :
 * un champ absent (null) conserve sa valeur actuelle.
 */
@Builder(toBuilder = true)
public record VisiteSuiviUpdateRequest(
        LocalDate dateVisite,
        LocalTime heureArrivee,
        LocalTime heureDepart,
        String adresseVisite,
        Long medecinJuniorId,
        Long medecinSeniorId,
        String contactSenior,
        Boolean contactSeniorJoignable,
        TypeVisite typeVisite
) {
}
