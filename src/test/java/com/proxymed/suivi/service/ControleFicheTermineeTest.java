package com.proxymed.suivi.service;

import com.proxymed.suivi.enums.CreneauMesure;
import com.proxymed.suivi.enums.DecisionVisite;
import com.proxymed.suivi.enums.EtatGeneral;
import com.proxymed.suivi.enums.ObservanceTraitement;
import com.proxymed.suivi.enums.SituationSociale;
import com.proxymed.suivi.enums.TypeConstante;
import com.proxymed.suivi.enums.TypeVisite;
import com.proxymed.suivi.service.model.ConstanteVisiteModel;
import com.proxymed.suivi.service.model.VisiteSuiviModel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ControleFicheTermineeTest {

    @Test
    void ficheComplete_naAucunManquement() {
        assertThat(ControleFicheTerminee.manquements(ficheComplete().build())).isEmpty();
    }

    @Test
    void ficheVide_listeTousLesChampsRequis() {
        List<String> manquements = ControleFicheTerminee.manquements(
                VisiteSuiviModel.builder().constantes(List.of()).alerteDeclenchee(false).build());

        assertThat(manquements).extracting(m -> m.substring(0, m.indexOf(" :"))).containsExactly(
                "dateVisite", "heureArrivee", "typeVisite", "contactSenior", "heureDepart", "etatGeneral",
                "constantes", "observanceTraitement", "situationSociale", "decision", "resumeClinique");
    }

    @Test
    void texteVide_compteCommeManquant() {
        assertThat(ControleFicheTerminee.manquements(ficheComplete().resumeClinique("   ").contactSenior("").build()))
                .hasSize(2)
                .anyMatch(m -> m.startsWith("resumeClinique"))
                .anyMatch(m -> m.startsWith("contactSenior"));
    }

    @Test
    void constantesH0_toutesRequises_lesAutresCreneauxNeComptentPas() {
        List<ConstanteVisiteModel> constantes = Arrays.stream(TypeConstante.values())
                .filter(type -> type != TypeConstante.GLYCEMIE && type != TypeConstante.EVA)
                .map(type -> mesure(type, CreneauMesure.H0))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        constantes.add(mesure(TypeConstante.GLYCEMIE, CreneauMesure.H_PLUS_1));

        assertThat(ControleFicheTerminee.manquements(ficheComplete().constantes(constantes).build()))
                .containsExactly("constantes : mesures du creneau H0 manquantes (GLYCEMIE, EVA)");
    }

    @Test
    void etatGeneralAltere_exigeLaPrecision() {
        assertThat(ControleFicheTerminee.manquements(ficheComplete().etatGeneral(EtatGeneral.ALTERE).build()))
                .singleElement().asString().startsWith("etatGeneralPrecision");
        assertThat(ControleFicheTerminee.manquements(ficheComplete()
                .etatGeneral(EtatGeneral.ALTERE).etatGeneralPrecision("Asthenie").build())).isEmpty();
    }

    @Test
    void alerteDeclenchee_exigeLHeureDAppelEtLaDecisionDuSenior() {
        assertThat(ControleFicheTerminee.manquements(ficheComplete().alerteDeclenchee(true).build()))
                .extracting(m -> m.substring(0, m.indexOf(" :")))
                .containsExactly("heureAppelSenior", "decisionAppelSenior");
        assertThat(ControleFicheTerminee.manquements(ficheComplete().alerteDeclenchee(true)
                .heureAppelSenior(LocalTime.of(9, 40)).decisionAppelSenior("Transfert EPS").build())).isEmpty();
    }

    @Test
    void signalementDass_exigeLHeureDeContact() {
        assertThat(ControleFicheTerminee.manquements(ficheComplete().signalementDass(true).build()))
                .singleElement().asString().startsWith("heureContactDass");
        assertThat(ControleFicheTerminee.manquements(ficheComplete().signalementDass(false).build())).isEmpty();
        assertThat(ControleFicheTerminee.manquements(ficheComplete()
                .signalementDass(true).heureContactDass(LocalTime.of(10, 30)).build())).isEmpty();
    }

    private VisiteSuiviModel.VisiteSuiviModelBuilder ficheComplete() {
        return VisiteSuiviModel.builder()
                .dateVisite(LocalDate.of(2026, 10, 5))
                .heureArrivee(LocalTime.of(9, 0))
                .heureDepart(LocalTime.of(11, 0))
                .typeVisite(TypeVisite.PROGRAMMEE)
                .contactSenior("+221 77 640 12 30")
                .etatGeneral(EtatGeneral.BON)
                .constantes(Arrays.stream(TypeConstante.values()).map(type -> mesure(type, CreneauMesure.H0)).toList())
                .alerteDeclenchee(false)
                .observanceTraitement(ObservanceTraitement.BONNE)
                .situationSociale(SituationSociale.STABLE)
                .decision(DecisionVisite.PATIENT_STABLE_SUIVI_PROGRAMME)
                .resumeClinique("Patient stable");
    }

    private ConstanteVisiteModel mesure(TypeConstante type, CreneauMesure creneau) {
        return ConstanteVisiteModel.builder().type(type).creneau(creneau).valeur(BigDecimal.TEN).build();
    }
}
