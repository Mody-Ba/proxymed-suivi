package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.CreneauMesure;
import com.proxymed.suivi.enums.SigneAlerteClinique;
import com.proxymed.suivi.enums.TypeConstante;
import lombok.Builder;

import java.math.BigDecimal;

@Builder(toBuilder = true)
public record AlerteResponse(
        TypeConstante constante,
        CreneauMesure creneau,
        BigDecimal valeur,
        BigDecimal valeurDiastolique,
        SigneAlerteClinique signeClinique,
        String seuilFranchi
) {
}
