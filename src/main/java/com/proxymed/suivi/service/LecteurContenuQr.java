package com.proxymed.suivi.service;

import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.service.model.ContenuQrModel;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;


@Component
public class LecteurContenuQr {

    static final String CHAMP_TYPE = "type";
    static final String CHAMP_PATIENT = "patientId";
    static final String CHAMP_VISITE = "visiteId";

    static final String MESSAGE_PAS_PROXYMED = "Le code QR detecte n'est pas un code QR PROXYMED";

    private static final JsonMapper JSON = JsonMapper.builder().build();


    public ContenuQrModel lire(String texte) {
        JsonNode racine = lireJson(texte)
                .filter(JsonNode::isObject)
                .orElseThrow(() -> new RegleGestionException(MESSAGE_PAS_PROXYMED));
        String type = texte(racine, CHAMP_TYPE);
        UUID patientId = uuid(racine, CHAMP_PATIENT);
        UUID visiteId = uuid(racine, CHAMP_VISITE);
        if (type == null || patientId == null || visiteId == null) {
            throw new RegleGestionException(MESSAGE_PAS_PROXYMED);
        }
        if (!GenerateurContenuQr.TYPE_FICHE_2_SUIVI.equals(type)) {
            throw new RegleGestionException("Type de fiche non pris en charge : " + type
                    + " (seule la fiche " + GenerateurContenuQr.TYPE_FICHE_2_SUIVI + " peut etre importee ici)");
        }
        return ContenuQrModel.builder().type(type).patientId(patientId).visiteId(visiteId).build();
    }

    private Optional<JsonNode> lireJson(String texte) {
        if (texte == null || texte.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(JSON.readTree(texte));
        } catch (JacksonException e) {
            return Optional.empty();
        }
    }

    private String texte(JsonNode racine, String champ) {
        JsonNode valeur = racine.get(champ);
        return valeur != null && valeur.isString() && !valeur.asString().isBlank() ? valeur.asString() : null;
    }

    private UUID uuid(JsonNode racine, String champ) {
        String valeur = texte(racine, champ);
        if (valeur == null) {
            return null;
        }
        try {
            UUID uuid = UUID.fromString(valeur);
            return uuid.toString().equalsIgnoreCase(valeur) ? uuid : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
