package com.proxymed.suivi.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.proxymed.suivi.enums.ComprehensionPatient;
import com.proxymed.suivi.enums.CreneauMesure;
import com.proxymed.suivi.enums.DecisionVisite;
import com.proxymed.suivi.enums.EtatGeneral;
import com.proxymed.suivi.enums.EvenementIntercurrent;
import com.proxymed.suivi.enums.MoyenTransmission;
import com.proxymed.suivi.enums.ObservanceTraitement;
import com.proxymed.suivi.enums.RoleMedecin;
import com.proxymed.suivi.enums.Sexe;
import com.proxymed.suivi.enums.SignatairePatient;
import com.proxymed.suivi.enums.SigneAlerteClinique;
import com.proxymed.suivi.enums.SituationSociale;
import com.proxymed.suivi.enums.StatutPlanPrevention;
import com.proxymed.suivi.enums.StatutVisite;
import com.proxymed.suivi.enums.ThemeEducation;
import com.proxymed.suivi.enums.TypeConstante;
import com.proxymed.suivi.enums.TypeVisite;
import com.proxymed.suivi.service.model.AlerteModel;
import com.proxymed.suivi.service.model.ConstanteVisiteModel;
import com.proxymed.suivi.service.model.MedecinModel;
import com.proxymed.suivi.service.model.PatientModel;
import com.proxymed.suivi.service.model.TraitementVisiteModel;
import com.proxymed.suivi.service.model.VisiteSuiviModel;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste le rendu complet de la Fiche 2 (template Thymeleaf reel + QR code + conversion PDF),
 * sans contexte Spring. Le PDF d'exemple est ecrit dans target/ pour verification visuelle.
 */
class PdfGenerationServiceTest {

    private PdfGenerationService pdfGenerationService;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        pdfGenerationService = new PdfGenerationService(templateEngine, ZoneId.of("Africa/Dakar"));
    }

    @Test
    void genererFicheVisite_produitUnPdfAvecLesNeufSectionsEtLesDonnees() throws Exception {
        VisiteSuiviModel visite = ficheComplete();

        byte[] pdf = lire(pdfGenerationService.genererFicheVisite(visite));
        Files.write(Path.of("target", "fiche-visite-exemple.pdf"), pdf);

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String texte = new PDFTextStripper().getText(document);
            assertThat(texte).contains(
                    "1. Identification de la visite",
                    "2. État général du patient",
                    "3. Constantes vitales et surveillance",
                    "4. Examen clinique ciblé",
                    "5. Observance thérapeutique et traitement",
                    "6. Éducation thérapeutique et conseils",
                    "7. Bilan social et coordination DASS",
                    "8. Conclusion de la visite et décisions",
                    "9. Signature et validation");
            assertThat(texte).contains(
                    "Abdoulaye Fall", "PXM-2026-0002", "57 ans", "Visite n° 3", "05/10/2026",
                    "Dr Ousmane Ndiaye", "JUN-042", "Dr Aminata Diop", "+221 77 640 12 30",
                    "190/100", "37,2", "ALERTE DÉCLENCHÉE", "TA > 180/110 mmHg (H0)", "Douleur thoracique",
                    "Asthenie marquee", "Chute dans la cour", "Pouls regulier",
                    "Amlodipine 5 mg", "1 cp le matin", "Oedemes des chevilles",
                    "Prevention des chutes", "Aidant absent la semaine", "HTA mal equilibree",
                    "05/10/2026 18:00", "19/10/2026",
                    "Signé le 05/10/2026 12:15", "Visé le 06/10/2026 08:30", "Signataire : aidant",
                    "Dr Ndiaye");
            assertThat(texte).contains(visite.id().toString());
            // La police PDF standard n'a pas le glyphe "superieur ou egal" : il ne doit pas sortir en "#".
            assertThat(texte).contains(">= 95 %").doesNotContain("#");
        }
    }

    @Test
    void genererFicheVisite_qrCodeEncodeUniquementLUuid() throws Exception {
        VisiteSuiviModel visite = ficheComplete();

        byte[] pdf = lire(pdfGenerationService.genererFicheVisite(visite));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            BufferedImage page = new PDFRenderer(document).renderImageWithDPI(0, 200);
            assertThat(decoderQrCode(page)).isEqualTo(visite.id().toString());
        }
    }

    @Test
    void genererQrCodeBase64_produitUnPngDecodableContenantLUuid() throws Exception {
        UUID id = UUID.randomUUID();

        byte[] png = Base64.getDecoder().decode(pdfGenerationService.genererQrCodeBase64(id));

        BufferedImage image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        assertThat(decoderQrCode(image)).isEqualTo(id.toString());
    }

    @Test
    void remplirTemplate_cocheLesCasesSelonLesDonnees() {
        String html = pdfGenerationService.remplirTemplate(ficheComplete());

        assertThat(html)
                .contains(caseCochee("Visite urgente"))
                .contains(caseCochee("État altéré"))
                .contains(caseCochee("Chute / accident"))
                .contains(caseCochee("TA &gt; 180/110 ou &lt; 90/60"))
                .contains(caseCochee("Douleur thoracique"))
                .contains(caseCochee("Partielle (50-80 %)"))
                .contains(caseCochee("Gestion médicaments"))
                .contains(caseCochee("À réexpliquer"))
                .contains(caseCochee("Détériorée"))
                .contains(caseCochee("Suivi renforcé - fréquence augmentée"))
                .contains(caseCochee("WhatsApp Pro"));
        assertThat(html)
                .doesNotContain(caseCochee("Visite programmée"))
                .doesNotContain(caseCochee("Bon état général"))
                .doesNotContain(caseCochee("Hospitalisation"))
                .doesNotContain(caseCochee("SpO2 &lt; 90 %"))
                .doesNotContain(caseCochee("Détresse respiratoire"))
                .doesNotContain(caseCochee("Patient stable - poursuite du suivi programmé"));
        // Seule la mesure hors seuil est mise en evidence dans la grille.
        assertThat(html).contains("class=\"centre alerte\">190/100<").contains("class=\"centre\">78<");
    }

    @Test
    void genererFicheVisite_supporteUnBrouillonQuasiVide() throws Exception {
        VisiteSuiviModel brouillon = VisiteSuiviModel.builder()
                .id(UUID.randomUUID())
                .numeroVisite(1)
                .statut(StatutVisite.BROUILLON)
                .patient(PatientModel.builder().nomComplet("Awa Ndiaye").numeroDossierProxymed("PXM-2026-0001").build())
                .build();

        byte[] pdf = lire(pdfGenerationService.genererFicheVisite(brouillon));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String texte = new PDFTextStripper().getText(document);
            assertThat(texte).contains("Awa Ndiaye", "BROUILLON", "Aucun traitement saisi", "Non signé", "Non visé")
                    .doesNotContain("ALERTE DÉCLENCHÉE");
        }
    }

    @Test
    void genererFicheVisite_ecritUnFichierPdfTemporaireSupprimeApresLecture() throws Exception {
        Resource pdf = pdfGenerationService.genererFicheVisite(ficheComplete());

        assertThat(pdf.getFile()).isFile().hasExtension("pdf");
        assertThat(pdf.contentLength()).isPositive();

        byte[] contenu = lire(pdf);

        assertThat(new String(contenu, 0, 5)).isEqualTo("%PDF-");
        assertThat(pdf.getFile()).doesNotExist();
    }

    private String caseCochee(String libelle) {
        return "<span class=\"case coche\">X</span><span>" + libelle + "</span>";
    }

    private byte[] lire(Resource pdf) throws Exception {
        try (InputStream flux = pdf.getInputStream()) {
            return flux.readAllBytes();
        }
    }

    private String decoderQrCode(BufferedImage image) throws Exception {
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        return new QRCodeReader().decode(bitmap, Map.of(DecodeHintType.TRY_HARDER, Boolean.TRUE)).getText();
    }

    private VisiteSuiviModel ficheComplete() {
        ConstanteVisiteModel tension = ConstanteVisiteModel.builder().type(TypeConstante.TA).creneau(CreneauMesure.H0)
                .valeur(new BigDecimal("190.00")).valeurDiastolique(new BigDecimal("100.00"))
                .alerte(true).seuilFranchi("TA > 180/110 mmHg").build();
        return VisiteSuiviModel.builder()
                .id(UUID.randomUUID())
                .numeroVisite(3)
                .statut(StatutVisite.VISEE)
                .patient(PatientModel.builder().id(UUID.randomUUID()).numeroDossierProxymed("PXM-2026-0002")
                        .nomComplet("Abdoulaye Fall").age(57).sexe(Sexe.M).adresseDomicile("Medina, rue 11").build())
                .dateVisite(LocalDate.of(2026, 10, 5))
                .heureArrivee(LocalTime.of(9, 30))
                .heureDepart(LocalTime.of(11, 45))
                .medecinJunior(MedecinModel.builder().id(2L).prenom("Ousmane").nom("Ndiaye").numeroOrdre("JUN-042")
                        .role(RoleMedecin.JUNIOR).build())
                .medecinSenior(MedecinModel.builder().id(1L).prenom("Aminata").nom("Diop").numeroOrdre("SEN-007")
                        .role(RoleMedecin.SENIOR).telephone("+221 77 640 12 30").build())
                .contactSenior("+221 77 640 12 30")
                .contactSeniorJoignable(true)
                .typeVisite(TypeVisite.URGENTE)
                .etatGeneral(EtatGeneral.ALTERE)
                .etatGeneralPrecision("Asthenie marquee")
                .plaintePrincipale("Cephalees depuis 3 jours")
                .evenementsIntercurrents(List.of(EvenementIntercurrent.CHUTE_ACCIDENT, EvenementIntercurrent.NON_OBSERVANCE))
                .evenementsPrecision("Chute dans la cour")
                .constantes(List.of(
                        tension,
                        ConstanteVisiteModel.builder().type(TypeConstante.TA).creneau(CreneauMesure.H_PLUS_1)
                                .valeur(new BigDecimal("160")).valeurDiastolique(new BigDecimal("95")).alerte(false).build(),
                        ConstanteVisiteModel.builder().type(TypeConstante.FC).creneau(CreneauMesure.H0)
                                .valeur(new BigDecimal("78.00")).alerte(false).build(),
                        ConstanteVisiteModel.builder().type(TypeConstante.TEMPERATURE).creneau(CreneauMesure.H0)
                                .valeur(new BigDecimal("37.20")).alerte(false).build()))
                .signesAlerteCliniques(List.of(SigneAlerteClinique.DOULEUR_THORACIQUE))
                .alerteDeclenchee(true)
                .alertes(List.of(
                        AlerteModel.builder().constante(TypeConstante.TA).creneau(CreneauMesure.H0)
                                .valeur(tension.valeur()).valeurDiastolique(tension.valeurDiastolique())
                                .seuilFranchi("TA > 180/110 mmHg").build(),
                        AlerteModel.builder().signeClinique(SigneAlerteClinique.DOULEUR_THORACIQUE)
                                .seuilFranchi("Douleur thoracique").build()))
                .heureAppelSenior(LocalTime.of(9, 40))
                .heureReponseSenior(LocalTime.of(9, 43))
                .decisionAppelSenior("Ajustement du traitement, controle a H+1")
                .examenCardiovasculaire("Pouls regulier, pas d'OMI")
                .examenRespiratoire("MV symetrique")
                .observanceTraitement(ObservanceTraitement.PARTIELLE)
                .raisonsNonObservance("Rupture de stock")
                .traitements(List.of(
                        TraitementVisiteModel.builder().id(1L).medicament("Amlodipine 5 mg").posologie("1 cp le matin")
                                .voie("Orale").observant(true).probleme(true).problemePrecision("Oedemes des chevilles").build(),
                        TraitementVisiteModel.builder().id(2L).medicament("Metformine 850 mg").observant(false).probleme(false).build()))
                .themesAbordes(List.of(ThemeEducation.GESTION_MEDICAMENTS, ThemeEducation.AUTRES))
                .themesAutresPrecision("Prevention des chutes")
                .comprehensionPatient(ComprehensionPatient.A_REEXPLIQUER)
                .statutPlanPrevention(StatutPlanPrevention.A_REVISER_PAR_SENIOR)
                .situationSociale(SituationSociale.DETERIOREE)
                .problemesSociaux("Aidant absent la semaine")
                .signalementDass(true)
                .heureContactDass(LocalTime.of(10, 30))
                .decision(DecisionVisite.SUIVI_RENFORCE)
                .resumeClinique("HTA mal equilibree, observance partielle")
                .dateEnvoiCompteRendu(LocalDateTime.of(2026, 10, 5, 18, 0))
                .compteRenduEnRetard(false)
                .moyenTransmission(MoyenTransmission.WHATSAPP_PRO)
                .prochaineVisiteDate(LocalDate.of(2026, 10, 19))
                .dateSignatureJunior(Instant.parse("2026-10-05T12:15:00Z"))
                .dateVisaSenior(Instant.parse("2026-10-06T08:30:00Z"))
                .visaEnRetard(false)
                .dateSignaturePatient(Instant.parse("2026-10-05T12:20:00Z"))
                .signatairePatient(SignatairePatient.AIDANT)
                .dateSaisieDmi(LocalDate.of(2026, 10, 6))
                .saisieDmiPar("Dr Ndiaye")
                .build();
    }
}
