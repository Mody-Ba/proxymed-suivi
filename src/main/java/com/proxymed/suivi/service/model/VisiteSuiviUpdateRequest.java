package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.SigneAlerteClinique;
import com.proxymed.suivi.enums.TypeVisite;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Mise a jour partielle des champs scalaires d'une visite en BROUILLON :
 * un champ absent (null) conserve sa valeur actuelle.
 * signesAlerteCliniques, s'il est envoye, remplace toute la liste cochee (liste vide = tout decocher).
 * Les constantes se gerent par leur endpoint dedie (/api/visites/{id}/constantes).
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
        TypeVisite typeVisite,

        // Section 3 : signes d'alerte cliniques et appel du medecin senior
        List<SigneAlerteClinique> signesAlerteCliniques,
        LocalTime heureAppelSenior,
        LocalTime heureReponseSenior,
        String decisionAppelSenior
) {
}
