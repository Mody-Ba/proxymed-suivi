package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.CreneauMesure;
import com.proxymed.suivi.enums.SigneAlerteClinique;
import com.proxymed.suivi.enums.TypeConstante;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Une alerte de la visite (appel immediat du senior). Soit une constante hors seuil
 * (constante, creneau, valeur renseignes), soit un signe clinique coche (signeClinique).
 */
@Builder(toBuilder = true)
public record AlerteModel(
        TypeConstante constante,
        CreneauMesure creneau,
        BigDecimal valeur,
        BigDecimal valeurDiastolique,
        SigneAlerteClinique signeClinique,
        String seuilFranchi
) {
}
