package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.SigneAlerteClinique;
import com.proxymed.suivi.enums.StatutVisite;
import com.proxymed.suivi.enums.TypeVisite;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record VisiteSuiviResponse(
        UUID id,
        Integer numeroVisite,
        StatutVisite statut,
        PatientResponse patient,
        LocalDate dateVisite,
        LocalTime heureArrivee,
        LocalTime heureDepart,
        String adresseVisite,
        MedecinResponse medecinJunior,
        MedecinResponse medecinSenior,
        String contactSenior,
        Boolean contactSeniorJoignable,
        TypeVisite typeVisite,

        // Section 3 : surveillance (alertes recalculees a chaque lecture)
        boolean alerteDeclenchee,
        List<AlerteResponse> alertes,
        List<LigneGrilleConstantesResponse> grilleConstantes,
        List<SigneAlerteClinique> signesAlerteCliniques,
        LocalTime heureAppelSenior,
        LocalTime heureReponseSenior,
        String decisionAppelSenior,

        Instant dateCreation,
        Instant dateMiseAJour
) {
}
