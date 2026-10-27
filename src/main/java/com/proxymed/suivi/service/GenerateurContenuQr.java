package com.proxymed.suivi.service;

import com.proxymed.suivi.service.model.ContenuQrModel;
import com.proxymed.suivi.service.model.VisiteSuiviModel;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


@Component
public class GenerateurContenuQr {

    public static final String TYPE_FICHE_2_SUIVI = "FICHE_2_SUIVI";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    public String construire(VisiteSuiviModel visite) {
        Objects.requireNonNull(visite.patient(), "La visite doit porter son patient");
        return construire(ContenuQrModel.builder()
                .type(TYPE_FICHE_2_SUIVI)
                .patientId(visite.patient().id())
                .visiteId(visite.id())
                .build());
    }

    String construire(ContenuQrModel contenu) {
        Map<String, String> champs = new LinkedHashMap<>();
        champs.put(LecteurContenuQr.CHAMP_TYPE, Objects.requireNonNull(contenu.type(), "type"));
        champs.put(LecteurContenuQr.CHAMP_PATIENT, Objects.requireNonNull(contenu.patientId(), "patientId").toString());
        champs.put(LecteurContenuQr.CHAMP_VISITE, Objects.requireNonNull(contenu.visiteId(), "visiteId").toString());
        return JSON.writeValueAsString(champs);
    }
}
