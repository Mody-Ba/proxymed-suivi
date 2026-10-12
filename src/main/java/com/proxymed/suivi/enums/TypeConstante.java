package com.proxymed.suivi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Lignes de la grille des constantes (section 3.1 de la Fiche 2), dans l'ordre de la fiche,
 * avec la colonne "Valeurs normales" telle qu'elle y est imprimee.
 */
@Getter
@RequiredArgsConstructor
public enum TypeConstante {
    TA("Tension artérielle", "mmHg", "< 140/90"),
    FC("Fréquence cardiaque", "batt/min", "60–100"),
    FR("Fréquence respiratoire", "R/min", "12–20"),
    SPO2("SpO2", "%", "≥ 95 %"),
    TEMPERATURE("Température", "°C", "36–37,5"),
    GLYCEMIE("Glycémie capillaire", "g/l", "0,7–1,1 (à jeun)"),
    EVA("Score de douleur EVA", "0–10", "Cible < 3"),
    GLASGOW("Score de Glasgow", "3–15", "15 (normal)");

    private final String libelle;
    private final String unite;
    private final String valeursNormales;
}
