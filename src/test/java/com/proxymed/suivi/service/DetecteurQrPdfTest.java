package com.proxymed.suivi.service;

import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.support.PdfDeTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DetecteurQrPdfTest {

    private final DetecteurQrPdf detecteur = new DetecteurQrPdf();

    @Test
    void detecter_chercheSurToutesLesPages_dansLOrdre() {
        byte[] pdf = PdfDeTest.pdfAvecQr(null, "page 2", null, "page 4");

        assertThat(detecteur.detecter(pdf)).containsExactly("page 2", "page 4");
    }

    @Test
    void detecter_litLeQrDUnPdfScanne() {
        byte[] scan = PdfDeTest.numeriser(PdfDeTest.pdfAvecQr("{\"type\":\"FICHE_2_SUIVI\"}"), 200);

        assertThat(detecteur.detecter(scan)).containsExactly("{\"type\":\"FICHE_2_SUIVI\"}");
    }

    @Test
    void detecter_renvoieUneListeVide_sansQrCode() {
        assertThat(detecteur.detecter(PdfDeTest.pdfAvecQr(null, null))).isEmpty();
    }

    @Test
    void detecter_rejetteUnPdfEndommage() {
        byte[] endommage = "%PDF-1.7\nfiche tronquee".getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> detecteur.detecter(endommage))
                .isInstanceOf(RegleGestionException.class)
                .hasMessage("Le fichier PDF est illisible ou endommage");
    }
}
