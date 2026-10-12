package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.CreneauMesure;
import com.proxymed.suivi.enums.TypeConstante;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalTime;

@Builder(toBuilder = true)
public record ConstanteVisiteResponse(
        Long id,
        TypeConstante type,
        CreneauMesure creneau,
        BigDecimal valeur,
        BigDecimal valeurDiastolique,
        LocalTime heureMesure,
        Boolean alerte,
        String seuilFranchi
) {
}
