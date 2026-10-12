package com.proxymed.suivi.service;

import com.proxymed.suivi.service.model.ConstanteVisiteModel;

import java.math.BigDecimal;

/**
 * Seuils d'alerte de la Fiche 2 (encadre "Appel immediat du medecin senior si", section 3).
 * Ils viennent du document et sont distincts des bornes de validite (BornesConstantesVitales).
 * Les comparaisons sont strictes, sauf la douleur EVA (alerte des 8/10).
 */
final class SeuilsAlerte {

    private SeuilsAlerte() {
    }

    /**
     * Renseigne alerte et seuilFranchi sur la mesure (seuilFranchi reste null sans alerte).
     */
    static ConstanteVisiteModel appliquer(ConstanteVisiteModel constante) {
        String seuil = seuilFranchi(constante);
        return constante.toBuilder().alerte(seuil != null).seuilFranchi(seuil).build();
    }

    private static String seuilFranchi(ConstanteVisiteModel constante) {
        BigDecimal valeur = constante.valeur();
        if (constante.type() == null || valeur == null) {
            return null;
        }
        return switch (constante.type()) {
            case TA -> seuilTension(valeur, constante.valeurDiastolique());
            case FC -> horsIntervalle(valeur, "50", "120", "FC < 50 batt/min", "FC > 120 batt/min");
            case FR -> horsIntervalle(valeur, "10", "25", "FR < 10 R/min", "FR > 25 R/min");
            case SPO2 -> inferieur(valeur, "90") ? "SpO2 < 90 %" : null;
            case TEMPERATURE -> horsIntervalle(valeur, "35", "38.5", "T° < 35 °C", "T° > 38,5 °C");
            case GLYCEMIE -> horsIntervalle(valeur, "0.6", "2.5", "Glycémie < 0,6 g/l", "Glycémie > 2,5 g/l");
            case GLASGOW -> inferieur(valeur, "13") ? "Glasgow < 13" : null;
            case EVA -> !inferieur(valeur, "8") ? "Douleur EVA ≥ 8/10" : null;
        };
    }

    /**
     * TA > 180/110 ou < 90/60 : l'alerte se declenche des que la systolique OU la diastolique
     * franchit son seuil.
     */
    private static String seuilTension(BigDecimal systolique, BigDecimal diastolique) {
        if (superieur(systolique, "180") || (diastolique != null && superieur(diastolique, "110"))) {
            return "TA > 180/110 mmHg";
        }
        if (inferieur(systolique, "90") || (diastolique != null && inferieur(diastolique, "60"))) {
            return "TA < 90/60 mmHg";
        }
        return null;
    }

    private static String horsIntervalle(BigDecimal valeur, String bas, String haut, String seuilBas, String seuilHaut) {
        if (inferieur(valeur, bas)) {
            return seuilBas;
        }
        return superieur(valeur, haut) ? seuilHaut : null;
    }

    private static boolean inferieur(BigDecimal valeur, String seuil) {
        return valeur.compareTo(new BigDecimal(seuil)) < 0;
    }

    private static boolean superieur(BigDecimal valeur, String seuil) {
        return valeur.compareTo(new BigDecimal(seuil)) > 0;
    }
}
