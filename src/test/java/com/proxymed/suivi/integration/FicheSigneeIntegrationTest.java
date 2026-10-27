package com.proxymed.suivi.integration;

import com.proxymed.suivi.entity.FicheSignee;
import com.proxymed.suivi.entity.Medecin;
import com.proxymed.suivi.entity.Patient;
import com.proxymed.suivi.enums.RoleMedecin;
import com.proxymed.suivi.enums.Sexe;
import com.proxymed.suivi.repository.FicheSigneeRepository;
import com.proxymed.suivi.repository.MedecinRepository;
import com.proxymed.suivi.repository.PatientRepository;
import com.proxymed.suivi.repository.VisiteSuiviRepository;
import com.proxymed.suivi.support.PdfDeTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import sans id (POST /api/fiches-signees/import) : la visite est retrouvee grace au QR code
 * du PDF scanne. Telechargement par visite et liste du dossier patient.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FicheSigneeIntegrationTest {

    private static final String URL_IMPORT = "/api/fiches-signees/import";
    private static final byte[] PDF_SANS_QR = PdfDeTest.pdfAvecQr((String) null);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private MedecinRepository medecinRepository;
    @Autowired
    private VisiteSuiviRepository visiteSuiviRepository;
    @Autowired
    private FicheSigneeRepository ficheSigneeRepository;
    @Autowired
    private EntityManager entityManager;

    private Medecin junior;
    private Medecin senior;
    private UUID patientId;
    private String visiteId;

    @BeforeEach
    void setUp() throws Exception {
        senior = medecinRepository.save(Medecin.builder()
                .nom("Diop").prenom("Aminata").numeroOrdre("SEN-" + System.nanoTime()).role(RoleMedecin.SENIOR).build());
        junior = medecinRepository.save(Medecin.builder()
                .nom("Ndiaye").prenom("Ousmane").numeroOrdre("JUN-" + System.nanoTime()).role(RoleMedecin.JUNIOR).build());
        patientId = creerPatient("Abdoulaye Fall");
        visiteId = creerVisite(patientId);
    }

    @Test
    void allerRetour_pdfGenere_puisScanne_estRangeSansIdDansLaBonneVisite() throws Exception {
        // Le patient a deja une visite (setUp) : on importe la fiche de la deuxieme.
        String deuxiemeVisite = creerVisite(patientId);
        byte[] pdfGenere = mockMvc.perform(get("/api/visites/" + deuxiemeVisite + "/pdf"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        byte[] scan = PdfDeTest.numeriser(pdfGenere, 200);

        importer(new MockMultipartFile("fichier", "scan-0001.pdf", "application/pdf", scan))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.patientId", is(patientId.toString())))
                .andExpect(jsonPath("$.visiteId", is(deuxiemeVisite)))
                .andExpect(jsonPath("$.numeroVisite", is(2)))
                .andExpect(jsonPath("$.nomFichier", is("scan-0001.pdf")))
                .andExpect(jsonPath("$.tailleOctets", is(scan.length)))
                .andExpect(jsonPath("$.dateImport", notNullValue()))
                // Metadonnees seulement : jamais le contenu du fichier dans le JSON.
                .andExpect(jsonPath("$.contenu").doesNotExist());

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/visites/" + deuxiemeVisite + "/fiche-signee"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", startsWith("attachment; filename=\"scan-0001.pdf\"")))
                .andExpect(content().bytes(scan));
        assertThat(ficheSigneeRepository.findByVisite_Id(UUID.fromString(visiteId))).isEmpty();
        mockMvc.perform(get("/api/patients/" + patientId + "/fiches-signees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].visiteId", is(deuxiemeVisite)))
                .andExpect(jsonPath("$[0].numeroVisite", is(2)));
    }

    @Test
    void import_trouveLeQrSurUnePageSuivante() throws Exception {
        byte[] pdf = PdfDeTest.pdfAvecQr(null, null, qr("FICHE_2_SUIVI", patientId, visiteId));

        importer(new MockMultipartFile("fichier", "fiche.pdf", "application/pdf", pdf))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visiteId", is(visiteId)));
    }

    @Test
    void deuxiemeImport_surLaMemeVisite_renvoie409_etLaPremiereFicheEstConservee() throws Exception {
        byte[] premier = PdfDeTest.pdfAvecQr(qr("FICHE_2_SUIVI", patientId, visiteId));
        byte[] second = PdfDeTest.pdfAvecQr(qr("FICHE_2_SUIVI", patientId, visiteId), null);

        importer(new MockMultipartFile("fichier", "premier.pdf", "application/pdf", premier))
                .andExpect(status().isCreated());
        importer(new MockMultipartFile("fichier", "second.pdf", "application/pdf", second))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erreur", is("Fiche signee deja importee")))
                .andExpect(jsonPath("$.message", containsString("Une fiche signee existe deja pour cette visite")));

        mockMvc.perform(get("/api/visites/" + visiteId + "/fiche-signee"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(premier));
        mockMvc.perform(get("/api/patients/" + patientId + "/fiches-signees"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nomFichier", is("premier.pdf")));
    }

    @Test
    void memeFichierImporteDeuxFois_renvoie409() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "fiche.pdf", "application/pdf",
                PdfDeTest.pdfAvecQr(qr("FICHE_2_SUIVI", patientId, visiteId)));

        importer(fichier).andExpect(status().isCreated());
        importer(fichier)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Une fiche signee existe deja pour cette visite")));

        mockMvc.perform(get("/api/patients/" + patientId + "/fiches-signees"))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void contrainteDUnicite_refuseUneSecondeFichePourLaMemeVisite_enBase() {
        // Garde-fou contre deux imports simultanes, qui passeraient tous deux la verification du service.
        UUID id = UUID.fromString(visiteId);
        enregistrerFiche(id, "premiere.pdf", Instant.parse("2026-10-08T12:00:00Z"), PDF_SANS_QR);
        entityManager.flush();

        assertThatThrownBy(() -> {
            enregistrerFiche(id, "seconde.pdf", Instant.parse("2026-10-09T12:00:00Z"), PDF_SANS_QR);
            entityManager.flush();
        }).rootCause().message().containsIgnoringCase("uk_fiche_signee_visite");
    }

    @Test
    void dossierPatient_listeLesFichesDeToutesSesVisites_plusRecenteDAbord() throws Exception {
        UUID premiere = UUID.fromString(visiteId);
        UUID deuxieme = UUID.fromString(creerVisite(patientId));
        enregistrerFiche(premiere, "visite-1.pdf", Instant.parse("2026-09-05T10:00:00Z"), PDF_SANS_QR);
        enregistrerFiche(deuxieme, "visite-2.pdf", Instant.parse("2026-10-05T10:00:00Z"), PDF_SANS_QR);
        // Fiche d'un autre patient : absente du dossier.
        enregistrerFiche(UUID.fromString(creerVisite(creerPatient("Awa Ndiaye"))), "autre.pdf",
                Instant.parse("2026-10-06T10:00:00Z"), PDF_SANS_QR);

        mockMvc.perform(get("/api/patients/" + patientId + "/fiches-signees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].visiteId", is(deuxieme.toString())))
                .andExpect(jsonPath("$[0].numeroVisite", is(2)))
                .andExpect(jsonPath("$[0].nomFichier", is("visite-2.pdf")))
                .andExpect(jsonPath("$[0].dateImport", is("2026-10-05T10:00:00Z")))
                .andExpect(jsonPath("$[0].patientId", is(patientId.toString())))
                .andExpect(jsonPath("$[1].visiteId", is(premiere.toString())))
                .andExpect(jsonPath("$[1].numeroVisite", is(1)))
                .andExpect(jsonPath("$[1].nomFichier", is("visite-1.pdf")))
                .andExpect(jsonPath("$[0].contenu").doesNotExist());
    }

    @Test
    void dossierPatient_videOu404() throws Exception {
        mockMvc.perform(get("/api/patients/" + patientId + "/fiches-signees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/patients/" + UUID.randomUUID() + "/fiches-signees"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Patient introuvable")));
    }

    @Test
    void import_sansQrCode_renvoie422() throws Exception {
        importer(new MockMultipartFile("fichier", "fiche.pdf", "application/pdf", PDF_SANS_QR))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.erreur", is("Code QR introuvable")))
                .andExpect(jsonPath("$.message", containsString("Aucun code QR n'a ete trouve dans le PDF importe")));

        assertThat(ficheSigneeRepository.findAll()).isEmpty();
    }

    @Test
    void import_qrQuiNEstPasProxymed_renvoie400() throws Exception {
        for (String texte : new String[]{"https://exemple.com", visiteId, "{\"type\":\"FICHE_2_SUIVI\"}"}) {
            importer(new MockMultipartFile("fichier", "fiche.pdf", "application/pdf", PdfDeTest.pdfAvecQr(texte)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("Le code QR detecte n'est pas un code QR PROXYMED")));
        }
        assertThat(ficheSigneeRepository.findAll()).isEmpty();
    }

    @Test
    void import_autreTypeDeFiche_renvoie400() throws Exception {
        byte[] pdf = PdfDeTest.pdfAvecQr(qr("FICHE_1_INCLUSION", patientId, visiteId));

        importer(new MockMultipartFile("fichier", "fiche.pdf", "application/pdf", pdf))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Type de fiche non pris en charge : FICHE_1_INCLUSION")));
        assertThat(ficheSigneeRepository.findAll()).isEmpty();
    }

    @Test
    void import_patientOuVisiteInconnus_renvoie404() throws Exception {
        importer(new MockMultipartFile("fichier", "fiche.pdf", "application/pdf",
                PdfDeTest.pdfAvecQr(qr("FICHE_2_SUIVI", UUID.randomUUID(), visiteId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Patient introuvable")));
        importer(new MockMultipartFile("fichier", "fiche.pdf", "application/pdf",
                PdfDeTest.pdfAvecQr(qr("FICHE_2_SUIVI", patientId, UUID.randomUUID().toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Visite introuvable")));
        assertThat(ficheSigneeRepository.findAll()).isEmpty();
    }

    @Test
    void import_patientDuQrDifferentDuPatientDeLaVisite_renvoie400() throws Exception {
        UUID autrePatient = creerPatient("Awa Ndiaye");
        byte[] pdf = PdfDeTest.pdfAvecQr(qr("FICHE_2_SUIVI", autrePatient, visiteId));

        importer(new MockMultipartFile("fichier", "fiche.pdf", "application/pdf", pdf))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("ne correspond pas au patient de la visite")));
        assertThat(ficheSigneeRepository.findAll()).isEmpty();
    }

    @Test
    void import_rejetteCeQuiNEstPasUnVraiPdf() throws Exception {
        importer(new MockMultipartFile("fichier", "photo.pdf", "application/pdf",
                "pas un pdf".getBytes(StandardCharsets.US_ASCII)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur", is("Regle de gestion non respectee")))
                .andExpect(jsonPath("$.message", is("Le fichier importe doit etre un PDF")));
        importer(new MockMultipartFile("fichier", "fiche.png", "image/png", PDF_SANS_QR))
                .andExpect(status().isBadRequest());
        importer(new MockMultipartFile("fichier", "vide.pdf", "application/pdf", new byte[0]))
                .andExpect(status().isBadRequest());
        importer(new MockMultipartFile("fichier", "tronque.pdf", "application/pdf",
                "%PDF-1.7\nfiche tronquee".getBytes(StandardCharsets.US_ASCII)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Le fichier PDF est illisible ou endommage")));

        assertThat(ficheSigneeRepository.findAll()).isEmpty();
    }

    @Test
    void import_rejetteUnFichierDePlusDe10Mo() throws Exception {
        byte[] gros = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy(PDF_SANS_QR, 0, gros, 0, PDF_SANS_QR.length);

        importer(new MockMultipartFile("fichier", "gros.pdf", "application/pdf", gros))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("10 Mo")));
    }

    @Test
    void import_sansFichier_renvoie400() throws Exception {
        mockMvc.perform(multipart(URL_IMPORT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("fichier")));
    }

    @Test
    void telechargement_404_siVisiteIntrouvable_ouAucuneFiche() throws Exception {
        mockMvc.perform(get("/api/visites/" + UUID.randomUUID() + "/fiche-signee").accept("application/pdf"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Visite introuvable")));

        // Swagger demande application/pdf : l'erreur doit quand meme revenir en JSON lisible.
        mockMvc.perform(get("/api/visites/" + visiteId + "/fiche-signee").accept("application/pdf"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Aucune fiche signee")))
                .andExpect(jsonPath("$.details", hasSize(0)));
    }

    @Test
    void nomDeFichierAccentue_estTelechargeAvecSonNom() throws Exception {
        byte[] pdf = PdfDeTest.pdfAvecQr(qr("FICHE_2_SUIVI", patientId, visiteId));
        importer(new MockMultipartFile("fichier", "fiche signée.pdf", "application/pdf", pdf))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomFichier", is("fiche signée.pdf")));

        mockMvc.perform(get("/api/visites/" + visiteId + "/fiche-signee"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("filename*=UTF-8''fiche%20sign%C3%A9e.pdf")));
    }

    private ResultActions importer(MockMultipartFile fichier) throws Exception {
        return mockMvc.perform(multipart(URL_IMPORT).file(fichier));
    }

    /**
     * Texte de QR ecrit a la main (et non via GenerateurContenuQr) : verifie aussi le format attendu.
     */
    private String qr(String type, UUID patient, String visite) {
        return "{\"type\":\"" + type + "\",\"patientId\":\"" + patient + "\",\"visiteId\":\"" + visite + "\"}";
    }

    private void enregistrerFiche(UUID id, String nom, Instant dateImport, byte[] contenu) {
        ficheSigneeRepository.save(FicheSignee.builder()
                .visite(visiteSuiviRepository.getReferenceById(id))
                .nomFichier(nom)
                .typeContenu("application/pdf")
                .tailleOctets(contenu.length)
                .dateImport(dateImport)
                .contenu(contenu)
                .build());
    }

    private UUID creerPatient(String nom) {
        return patientRepository.save(Patient.builder()
                .numeroDossierProxymed("PXM-" + System.nanoTime())
                .nomComplet(nom)
                .dateNaissance(LocalDate.of(1968, 11, 27))
                .sexe(Sexe.M)
                .build()).getId();
    }

    private String creerVisite(UUID patient) throws Exception {
        String reponse = mockMvc.perform(post("/api/patients/" + patient + "/visites")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "medecinJuniorId", junior.getId(),
                                "medecinSeniorId", senior.getId()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(reponse).get("id").asText();
    }
}
