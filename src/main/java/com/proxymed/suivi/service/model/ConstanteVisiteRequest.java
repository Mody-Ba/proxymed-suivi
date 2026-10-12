package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.CreneauMesure;
import com.proxymed.suivi.enums.TypeConstante;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Une mesure de la grille. Pour la tension arterielle (TA), valeur est la systolique et
 * valeurDiastolique est obligatoire ; pour les autres types, valeurDiastolique est interdite.
 */
public record ConstanteVisiteRequest(
        @NotNull(message = "le type de constante est obligatoire")
        TypeConstante type,

        @NotNull(message = "le creneau est obligatoire")
        CreneauMesure creneau,

        @NotNull(message = "la valeur est obligatoire")
        BigDecimal valeur,

        BigDecimal valeurDiastolique,
        LocalTime heureMesure
) {
}
