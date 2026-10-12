package com.proxymed.suivi.service;

import com.proxymed.suivi.enums.CreneauMesure;
import com.proxymed.suivi.enums.TypeConstante;
import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.service.model.ConstanteVisiteModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BornesConstantesVitalesTest {

    @ParameterizedTest
    @CsvSource({
            "FC, 20", "FC, 250", "FR, 4", "FR, 70", "SPO2, 50", "SPO2, 100",
            "TEMPERATURE, 30", "TEMPERATURE, 45", "GLYCEMIE, 0.1", "GLYCEMIE, 10",
            "EVA, 0", "EVA, 10", "GLASGOW, 3", "GLASGOW, 15.0"
    })
    void accepte_lesValeursAuxBornes(TypeConstante type, String valeur) {
        assertThatCode(() -> BornesConstantesVitales.valider(mesure(type, valeur, null)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "FC, 19", "FC, 251", "FR, 3", "FR, 71", "SPO2, 49", "SPO2, 101",
            "TEMPERATURE, 29.9", "TEMPERATURE, 45.1", "GLYCEMIE, 0", "GLYCEMIE, 10.1",
            "EVA, -1", "EVA, 11", "GLASGOW, 2", "GLASGOW, 16"
    })
    void rejette_lesValeursImpossibles(TypeConstante type, String valeur) {
        assertThatThrownBy(() -> BornesConstantesVitales.valider(mesure(type, valeur, null)))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("impossible");
    }

    @Test
    void tension_accepteUneMesurePlausible_memeEnAlerte() {
        assertThatCode(() -> BornesConstantesVitales.valider(mesure(TypeConstante.TA, "210", "120")))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"301, 90", "39, 25", "120, 201", "120, 19"})
    void tension_rejetteUneSystoliqueOuUneDiastoliqueImpossible(String systolique, String diastolique) {
        assertThatThrownBy(() -> BornesConstantesVitales.valider(mesure(TypeConstante.TA, systolique, diastolique)))
                .isInstanceOf(RegleGestionException.class);
    }

    @Test
    void tension_rejette_siDiastoliqueSuperieureOuEgaleALaSystolique() {
        assertThatThrownBy(() -> BornesConstantesVitales.valider(mesure(TypeConstante.TA, "110", "110")))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("diastolique");
    }

    @Test
    void scores_rejettentLesValeursNonEntieres() {
        assertThatThrownBy(() -> BornesConstantesVitales.valider(mesure(TypeConstante.EVA, "7.5", null)))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("entier");
        assertThatThrownBy(() -> BornesConstantesVitales.valider(mesure(TypeConstante.GLASGOW, "14.5", null)))
                .isInstanceOf(RegleGestionException.class);
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
