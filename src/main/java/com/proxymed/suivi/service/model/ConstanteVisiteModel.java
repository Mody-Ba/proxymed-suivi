package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.CreneauMesure;
import com.proxymed.suivi.enums.TypeConstante;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Couche interne neutre : ni le contrat API (ConstanteVisiteRequest/Response), ni l'entite JPA.
 * alerte et seuilFranchi ne sont jamais persistes : le service les renseigne a chaque lecture
 * (SeuilsAlerte). seuilFranchi est null quand la mesure ne declenche pas d'alerte.
 */
@Builder(toBuilder = true)
public record ConstanteVisiteModel(
        Long id,
        UUID visiteId,
        TypeConstante type,
        CreneauMesure creneau,
        BigDecimal valeur,
        BigDecimal valeurDiastolique,
        LocalTime heureMesure,
        Boolean alerte,
        String seuilFranchi
) {
}
