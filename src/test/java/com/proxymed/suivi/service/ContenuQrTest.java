package com.proxymed.suivi.service;

import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.service.model.ContenuQrModel;
import com.proxymed.suivi.service.model.PatientModel;
import com.proxymed.suivi.service.model.VisiteSuiviModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Construction (GenerateurContenuQr) et lecture (LecteurContenuQr) du texte du QR code de la Fiche 2.
 */
class ContenuQrTest {

    private static final String PATIENT = "6f1c2a3b-4d5e-4f60-8a71-92b3c4d5e6f7";
    private static final String VISITE = "0a1b2c3d-4e5f-4a6b-9c7d-8e9f0a1b2c3d";

    private final GenerateurContenuQr generateur = new GenerateurContenuQr();
    private final LecteurContenuQr lecteur = new LecteurContenuQr();

    @Test
    void construire_produitLeJsonAttendu() {
        VisiteSuiviModel visite = VisiteSuiviModel.builder()
                .id(UUID.fromString(VISITE))
                .patient(PatientModel.builder().id(UUID.fromString(PATIENT)).build())
                .build();

        assertThat(generateur.construire(visite)).isEqualTo(
                "{\"type\":\"FICHE_2_SUIVI\",\"patientId\":\"" + PATIENT + "\",\"visiteId\":\"" + VISITE + "\"}");
    }

    @Test
    void construire_puisLire_restitueLeTypeLePatientEtLaVisite() {
        UUID patientId = UUID.randomUUID();
        UUID visiteId = UUID.randomUUID();
        VisiteSuiviModel visite = VisiteSuiviModel.builder()
                .id(visiteId).patient(PatientModel.builder().id(patientId).build()).build();

        ContenuQrModel lu = lecteur.lire(generateur.construire(visite));

        assertThat(lu).isEqualTo(new ContenuQrModel("FICHE_2_SUIVI", patientId, visiteId));
    }

    @Test
    void lire_accepteLesChampsDansNImporteQuelOrdre_etLesChampsEnPlus() {
        ContenuQrModel lu = lecteur.lire("{ \"visiteId\": \"" + VISITE + "\", \"version\": 2, "
                + "\"patientId\": \"" + PATIENT.toUpperCase() + "\", \"type\": \"FICHE_2_SUIVI\" }");

        assertThat(lu.patientId()).isEqualTo(UUID.fromString(PATIENT));
        assertThat(lu.visiteId()).isEqualTo(UUID.fromString(VISITE));
    }

    @Test
    void construire_exigeLIdDuPatientEtDeLaVisite() {
        assertThatThrownBy(() -> generateur.construire(VisiteSuiviModel.builder()
                .id(UUID.randomUUID()).patient(PatientModel.builder().build()).build()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> generateur.construire(VisiteSuiviModel.builder()
                .patient(PatientModel.builder().id(UUID.randomUUID()).build()).build()))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            // Ancien format (avant l'import automatique) : l'UUID seul de la visite
            VISITE,
            "https://exemple.com/qr",
            "{pas du json",
            "[\"FICHE_2_SUIVI\"]",
            "{\"patientId\":\"" + PATIENT + "\",\"visiteId\":\"" + VISITE + "\"}",
            "{\"type\":\"FICHE_2_SUIVI\",\"visiteId\":\"" + VISITE + "\"}",
            "{\"type\":\"FICHE_2_SUIVI\",\"patientId\":\"" + PATIENT + "\"}",
            "{\"type\":\"FICHE_2_SUIVI\",\"patientId\":\"pas-un-uuid\",\"visiteId\":\"" + VISITE + "\"}",
            "{\"type\":\"FICHE_2_SUIVI\",\"patientId\":\"1-2-3-4-5\",\"visiteId\":\"" + VISITE + "\"}",
            "{\"type\":\"FICHE_2_SUIVI\",\"patientId\":\"" + PATIENT + "\",\"visiteId\":42}",
            "{\"type\":null,\"patientId\":\"" + PATIENT + "\",\"visiteId\":\"" + VISITE + "\"}"
    })
    void lire_rejetteCeQuiNEstPasUnQrProxymed(String texte) {
        assertThatThrownBy(() -> lecteur.lire(texte))
                .isInstanceOf(RegleGestionException.class)
                .hasMessage("Le code QR detecte n'est pas un code QR PROXYMED");
    }

    @Test
    void lire_rejetteUnAutreTypeDeFiche() {
        assertThatThrownBy(() -> lecteur.lire(
                "{\"type\":\"FICHE_1_INCLUSION\",\"patientId\":\"" + PATIENT + "\",\"visiteId\":\"" + VISITE + "\"}"))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("Type de fiche non pris en charge : FICHE_1_INCLUSION");
    }
}
