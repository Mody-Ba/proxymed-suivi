package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.StatutVisite;
import com.proxymed.suivi.enums.TypeVisite;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Couche interne neutre : ni le contrat API (VisiteSuiviCreateRequest/UpdateRequest/Response),
 * ni l'entite JPA. Pour une intention de creation ou de mise a jour, patient et medecins ne
 * portent que leur id ; sur le chemin de lecture ils sont complets.
 */
@Builder(toBuilder = true)
public record VisiteSuiviModel(
        UUID id,
        PatientModel patient,
        Integer numeroVisite,
        LocalDate dateVisite,
        LocalTime heureArrivee,
        LocalTime heureDepart,
        String adresseVisite,
        MedecinModel medecinJunior,
        MedecinModel medecinSenior,
        String contactSenior,
        Boolean contactSeniorJoignable,
        TypeVisite typeVisite,
        StatutVisite statut,
        Instant dateCreation,
        Instant dateMiseAJour
) {
}
