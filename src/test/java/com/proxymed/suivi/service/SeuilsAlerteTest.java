package com.proxymed.suivi.service;

import com.proxymed.suivi.enums.CreneauMesure;
import com.proxymed.suivi.enums.TypeConstante;
import com.proxymed.suivi.service.model.ConstanteVisiteModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les seuils viennent de l'encadre "Seuils d'alerte" de la Fiche 2 : chaque test verifie la
 * valeur juste au seuil (pas d'alerte, sauf EVA) et la premiere valeur qui le franchit.
 */
class SeuilsAlerteTest {

    @ParameterizedTest
    @CsvSource({
            "FC, 120, false", "FC, 121, true", "FC, 50, false", "FC, 49, true",
            "SPO2, 90, false", "SPO2, 89, true", "SPO2, 100, false",
            "FR, 25, false", "FR, 26, true", "FR, 10, false", "FR, 9, true",
            "TEMPERATURE, 38.5, false", "TEMPERATURE, 38.6, true", "TEMPERATURE, 35, false", "TEMPERATURE, 34.9, true",
            "GLYCEMIE, 0.6, false", "GLYCEMIE, 0.59, true", "GLYCEMIE, 2.5, false", "GLYCEMIE, 2.51, true",
            "GLASGOW, 13, false", "GLASGOW, 12, true", "GLASGOW, 15, false",
            "EVA, 7, false", "EVA, 8, true", "EVA, 10, true"
    })
    void seuilsDesConstantesAUneValeur(TypeConstante type, String valeur, boolean alerteAttendue) {
        ConstanteVisiteModel resultat = SeuilsAlerte.appliquer(mesure(type, valeur, null));

        assertThat(resultat.alerte()).isEqualTo(alerteAttendue);
        assertThat(resultat.seuilFranchi() != null).isEqualTo(alerteAttendue);
    }

    @ParameterizedTest
    @CsvSource({
            "130, 80, false",
            "180, 110, false",
            "181, 80, true",
            "150, 111, true",
            "90, 60, false",
            "89, 70, true",
            "100, 59, true"
    })
    void seuilsDeLaTension_systoliqueOuDiastolique(String systolique, String diastolique, boolean alerteAttendue) {
        ConstanteVisiteModel resultat = SeuilsAlerte.appliquer(mesure(TypeConstante.TA, systolique, diastolique));

        assertThat(resultat.alerte()).isEqualTo(alerteAttendue);
    }

    @Test
    void seuilFranchi_indiqueLeSensDuDepassement() {
        assertThat(SeuilsAlerte.appliquer(mesure(TypeConstante.TA, "190", "100")).seuilFranchi()).isEqualTo("TA > 180/110 mmHg");
        assertThat(SeuilsAlerte.appliquer(mesure(TypeConstante.TA, "85", "55")).seuilFranchi()).isEqualTo("TA < 90/60 mmHg");
        assertThat(SeuilsAlerte.appliquer(mesure(TypeConstante.FC, "45", null)).seuilFranchi()).isEqualTo("FC < 50 batt/min");
        assertThat(SeuilsAlerte.appliquer(mesure(TypeConstante.GLYCEMIE, "3.1", null)).seuilFranchi()).isEqualTo("Glycémie > 2,5 g/l");
        assertThat(SeuilsAlerte.appliquer(mesure(TypeConstante.FC, "80", null)).seuilFranchi()).isNull();
    }

    private ConstanteVisiteModel mesure(TypeConstante type, String valeur, String diastolique) {
        return ConstanteVisiteModel.builder()
                .type(type)
                .creneau(CreneauMesure.H0)
                .valeur(new BigDecimal(valeur))
                .valeurDiastolique(diastolique != null ? new BigDecimal(diastolique) : null)
                .build();
    }
}
