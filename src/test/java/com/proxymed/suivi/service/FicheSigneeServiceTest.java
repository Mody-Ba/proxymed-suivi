package com.proxymed.suivi.service;

import com.proxymed.suivi.entity.FicheSignee;
import com.proxymed.suivi.entity.Patient;
import com.proxymed.suivi.entity.VisiteSuivi;
import com.proxymed.suivi.exception.FicheSigneeExistanteException;
import com.proxymed.suivi.exception.QrCodeIntrouvableException;
import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.exception.ResourceNotFoundException;
import com.proxymed.suivi.repository.FicheSigneeRepository;
import com.proxymed.suivi.repository.VisiteSuiviRepository;
import com.proxymed.suivi.service.mappers.FicheSigneeMapper;
import com.proxymed.suivi.service.model.ContenuQrModel;
import com.proxymed.suivi.service.model.FicheSigneeModel;
import com.proxymed.suivi.service.model.PatientModel;
import com.proxymed.suivi.service.model.VisiteSuiviModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Teste FicheSigneeService en isolation : repositories, services et detecteur de QR mockes
 * (la detection dans un vrai PDF est couverte par DetecteurQrPdfTest et le test d'integration),
 * lecteur de QR et mapper DB reels.
 */
@ExtendWith(MockitoExtension.class)
class FicheSigneeServiceTest {

    private static final byte[] PDF = "%PDF-1.7\nfiche signee".getBytes(StandardCharsets.US_ASCII);

    @Mock
    private FicheSigneeRepository ficheSigneeRepository;
    @Mock
    private VisiteSuiviRepository visiteSuiviRepository;
    @Mock
    private VisiteSuiviService visiteSuiviService;
    @Mock
    private PatientService patientService;
    @Mock
    private DetecteurQrPdf detecteurQrPdf;

    private final GenerateurContenuQr generateurContenuQr = new GenerateurContenuQr();

    private FicheSigneeService ficheSigneeService;

    private VisiteSuivi visite;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        ficheSigneeService = new FicheSigneeServiceImpl(ficheSigneeRepository, visiteSuiviService, patientService,
                new FicheSigneeMapper(visiteSuiviRepository), detecteurQrPdf, new LecteurContenuQr());

        patientId = UUID.randomUUID();
        visite = VisiteSuivi.builder().id(UUID.randomUUID()).numeroVisite(3)
                .patient(Patient.builder().id(patientId).build()).build();
        lenient().when(visiteSuiviRepository.getReferenceById(visite.getId())).thenReturn(visite);
        lenient().when(visiteSuiviService.findById(visite.getId())).thenReturn(VisiteSuiviModel.builder()
                .id(visite.getId()).numeroVisite(3).patient(PatientModel.builder().id(patientId).build()).build());
        lenient().when(detecteurQrPdf.detecter(any())).thenReturn(List.of(qr(patientId, visite.getId())));
        lenient().when(ficheSigneeRepository.saveAndFlush(any())).thenAnswer(inv -> {
            FicheSignee fiche = inv.getArgument(0);
            fiche.setId(UUID.randomUUID());
            return fiche;
        });
    }

    @Test
    void importer_rangeLePdfDansLaVisiteDuQrCode_avecSaTailleEtSaDateDImport() {
        FicheSigneeModel resultat = ficheSigneeService.importer(fichier("fiche-signee.pdf", "application/pdf", PDF));

        assertThat(resultat.id()).isNotNull();
        assertThat(resultat.visiteId()).isEqualTo(visite.getId());
        assertThat(resultat.patientId()).isEqualTo(patientId);
        assertThat(resultat.numeroVisite()).isEqualTo(3);
        assertThat(resultat.nomFichier()).isEqualTo("fiche-signee.pdf");
        assertThat(resultat.tailleOctets()).isEqualTo(PDF.length);
        assertThat(resultat.dateImport()).isNotNull();
        assertThat(resultat.contenu()).isEqualTo(PDF);
        verify(detecteurQrPdf).detecter(PDF);
    }

    @Test
    void importer_rejette_siLeTypeNEstPasPdf_ouSiLeContenuNeCommencePasParPdf() {
        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "image/png", PDF)))
                .isInstanceOf(RegleGestionException.class).hasMessageContaining("PDF");
        assertThatThrownBy(() -> ficheSigneeService.importer(
                fichier("photo.pdf", "application/pdf", "pas un pdf".getBytes(StandardCharsets.US_ASCII))))
                .isInstanceOf(RegleGestionException.class);
        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("vide.pdf", "application/pdf", new byte[0])))
                .isInstanceOf(RegleGestionException.class);
        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("sans-type.pdf", null, PDF)))
                .isInstanceOf(RegleGestionException.class);
        verifyNoInteractions(detecteurQrPdf);
        verify(ficheSigneeRepository, never()).saveAndFlush(any());
    }

    @Test
    void importer_rejette_auDelaDe10Mo_etAccepteExactement10Mo() {
        byte[] trop = pdfDeTaille(FicheSigneeServiceImpl.TAILLE_MAX_OCTETS + 1);
        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("gros.pdf", "application/pdf", trop)))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("10 Mo");
        verify(ficheSigneeRepository, never()).saveAndFlush(any());

        byte[] limite = pdfDeTaille(FicheSigneeServiceImpl.TAILLE_MAX_OCTETS);
        assertThat(ficheSigneeService.importer(fichier("limite.pdf", "application/pdf", limite)).tailleOctets())
                .isEqualTo(FicheSigneeServiceImpl.TAILLE_MAX_OCTETS);
    }

    @Test
    void importer_rejette_sansQrCode() {
        when(detecteurQrPdf.detecter(any())).thenReturn(List.of());

        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF)))
                .isInstanceOf(QrCodeIntrouvableException.class)
                .hasMessageContaining("Aucun code QR");
        verify(ficheSigneeRepository, never()).saveAndFlush(any());
    }

    @Test
    void importer_rejette_unQrQuiNEstPasProxymed_ouDUnAutreTypeDeFiche() {
        when(detecteurQrPdf.detecter(any())).thenReturn(List.of("https://exemple.com"));
        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF)))
                .isInstanceOf(RegleGestionException.class)
                .hasMessage("Le code QR detecte n'est pas un code QR PROXYMED");

        when(detecteurQrPdf.detecter(any())).thenReturn(List.of(generateurContenuQr.construire(
                new ContenuQrModel("FICHE_1_INCLUSION", patientId, visite.getId()))));
        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF)))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("Type de fiche non pris en charge");
        verify(ficheSigneeRepository, never()).saveAndFlush(any());
    }

    @Test
    void importer_retientLePremierQrProxymed_quandUneAutrePageEnPorteUnEtranger() {
        when(detecteurQrPdf.detecter(any())).thenReturn(List.of("https://exemple.com", qr(patientId, visite.getId())));

        assertThat(ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF)).visiteId())
                .isEqualTo(visite.getId());
    }

    @Test
    void importer_rejette_siPatientOuVisiteIntrouvable() {
        UUID patientInconnu = UUID.randomUUID();
        when(detecteurQrPdf.detecter(any())).thenReturn(List.of(qr(patientInconnu, visite.getId())));
        when(patientService.findById(patientInconnu))
                .thenThrow(new ResourceNotFoundException("Patient introuvable : " + patientInconnu));
        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient introuvable");

        UUID visiteInconnue = UUID.randomUUID();
        when(detecteurQrPdf.detecter(any())).thenReturn(List.of(qr(patientId, visiteInconnue)));
        when(visiteSuiviService.findById(visiteInconnue))
                .thenThrow(new ResourceNotFoundException("Visite introuvable : " + visiteInconnue));
        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Visite introuvable");
        verify(ficheSigneeRepository, never()).saveAndFlush(any());
    }

    @Test
    void importer_rejette_siLePatientDuQrNEstPasCeluiDeLaVisite() {
        UUID autrePatient = UUID.randomUUID();
        when(detecteurQrPdf.detecter(any())).thenReturn(List.of(qr(autrePatient, visite.getId())));

        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF)))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("ne correspond pas au patient de la visite");
        verify(ficheSigneeRepository, never()).saveAndFlush(any());
    }

    @Test
    void importer_rejette_siLaVisiteADejaUneFicheSignee() {
        when(ficheSigneeRepository.existsByVisite_Id(visite.getId())).thenReturn(true);

        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF)))
                .isInstanceOf(FicheSigneeExistanteException.class)
                .hasMessageContaining("Une fiche signee existe deja pour cette visite");
        verify(ficheSigneeRepository, never()).saveAndFlush(any());
    }

    @Test
    void importer_renvoieLeConflit_siUnImportSimultaneAGagneLaCourse() {
        // La verification passe pour les deux imports ; la contrainte d'unicite refuse le second.
        doThrow(new DataIntegrityViolationException("uk_fiche_signee_visite")).when(ficheSigneeRepository).saveAndFlush(any());

        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF)))
                .isInstanceOf(FicheSigneeExistanteException.class)
                .hasMessageContaining("Une fiche signee existe deja pour cette visite");
    }

    @Test
    void importer_gardeLeNomSansChemin_etDonneUnNomParDefaut() {
        ArgumentCaptor<FicheSignee> enregistree = ArgumentCaptor.forClass(FicheSignee.class);

        ficheSigneeService.importer(fichier("C:\\Users\\junior\\Scans\\fiche 12.pdf", "application/pdf", PDF));
        ficheSigneeService.importer(fichier("  ", "application/pdf", PDF));
        ficheSigneeService.importer(fichier(null, "application/pdf", PDF));

        verify(ficheSigneeRepository, org.mockito.Mockito.times(3)).saveAndFlush(enregistree.capture());
        assertThat(enregistree.getAllValues()).extracting(FicheSignee::getNomFichier)
                .containsExactly("fiche 12.pdf", "fiche-signee.pdf", "fiche-signee.pdf");
    }

    @Test
    void telecharger_renvoieLaFicheDeLaVisite() {
        FicheSignee fiche = FicheSignee.builder().id(UUID.randomUUID()).visite(visite).nomFichier("fiche.pdf")
                .typeContenu("application/pdf").tailleOctets(PDF.length).dateImport(Instant.now()).contenu(PDF).build();
        when(ficheSigneeRepository.findByVisite_Id(visite.getId())).thenReturn(Optional.of(fiche));

        FicheSigneeModel resultat = ficheSigneeService.telecharger(visite.getId());

        assertThat(resultat.id()).isEqualTo(fiche.getId());
        assertThat(resultat.contenu()).isEqualTo(PDF);
    }

    @Test
    void telecharger_rejette_siAucuneFicheImportee_ouVisiteIntrouvable() {
        when(ficheSigneeRepository.findByVisite_Id(visite.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ficheSigneeService.telecharger(visite.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Aucune fiche signee");

        UUID inconnue = UUID.randomUUID();
        when(visiteSuiviService.findById(inconnue)).thenThrow(new ResourceNotFoundException("Visite introuvable : " + inconnue));
        assertThatThrownBy(() -> ficheSigneeService.telecharger(inconnue))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Visite introuvable");
    }

    @Test
    void findByPatient_rejette_siPatientIntrouvable() {
        UUID inconnu = UUID.randomUUID();
        when(patientService.findById(inconnu)).thenThrow(new ResourceNotFoundException("Patient introuvable : " + inconnu));

        assertThatThrownBy(() -> ficheSigneeService.findByPatient(inconnu))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ficheSigneeRepository, never()).findResumesByPatientId(any());
    }

    private String qr(UUID patient, UUID visiteId) {
        return generateurContenuQr.construire(new ContenuQrModel(GenerateurContenuQr.TYPE_FICHE_2_SUIVI, patient, visiteId));
    }

    private FicheSigneeModel fichier(String nom, String type, byte[] contenu) {
        return FicheSigneeModel.builder().nomFichier(nom).typeContenu(type).tailleOctets(contenu.length).contenu(contenu).build();
    }

    private byte[] pdfDeTaille(long taille) {
        byte[] contenu = new byte[Math.toIntExact(taille)];
        Arrays.fill(contenu, (byte) ' ');
        System.arraycopy(PDF, 0, contenu, 0, PDF.length);
        return contenu;
    }
}
