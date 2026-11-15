package com.proxymed.suivi.integration;

import com.proxymed.suivi.entity.Patient;
import com.proxymed.suivi.entity.VisiteSuivi;
import com.proxymed.suivi.enums.Sexe;
import com.proxymed.suivi.enums.StatutVisite;
import com.proxymed.suivi.repository.PatientRepository;
import com.proxymed.suivi.repository.VisiteSuiviRepository;
import com.proxymed.suivi.service.DetecteurQrPdf;
import com.proxymed.suivi.service.GenerateurContenuQr;
import com.proxymed.suivi.service.LecteurContenuQr;
import com.proxymed.suivi.service.model.ContenuQrModel;
import com.proxymed.suivi.support.FabriqueBadge;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /api/visites/impression-lot : reserve a l'admin, une visite vierge par numero de dossier
 * PROXYMED et un seul PDF combine (identite + QR). Tout ou rien.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FabriqueBadge.class)
@Transactional
class ImpressionLotIntegrationTest {

    private static final String URL = "/api/visites/impression-lot";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FabriqueBadge fabriqueBadge;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private VisiteSuiviRepository visiteSuiviRepository;
    @Autowired
    private DetecteurQrPdf detecteurQrPdf;
    @Autowired
    private LecteurContenuQr lecteurContenuQr;

    private Patient awa;
    private Patient moussa;

    @BeforeEach
    void setUp() {
        awa = patient("Awa Ndiaye", Sexe.F);
        moussa = patient("Moussa Diop", Sexe.M);
    }

    @Test
    void admin_pdfCombine_uneFicheViergeParPatient() throws Exception {
        byte[] pdf = imprimer(fabriqueBadge.bearer("ADMIN"), List.of(awa.getNumeroDossierProxymed(), moussa.getNumeroDossierProxymed()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        List<VisiteSuivi> visites = visiteSuiviRepository.findAll().stream()
                .filter(v -> List.of(awa.getId(), moussa.getId()).contains(v.getPatient().getId())).toList();
        assertThat(visites).hasSize(2).allSatisfy(v -> {
            assertThat(v.getStatut()).isEqualTo(StatutVisite.BROUILLON);
            assertThat(v.getMedecinJunior()).isNull();
            assertThat(v.getMedecinSenior()).isNull();
            assertThat(v.getTypeVisite()).isNull();
        });

        try (PDDocument document = Loader.loadPDF(pdf)) {
            // Une fiche vierge = le gabarit fiche-visite complet (3 pages A4 a remplir a la main).
            int pagesParFiche = 3;
            assertThat(document.getNumberOfPages()).isEqualTo(2 * pagesParFiche);
            PDFTextStripper texte = new PDFTextStripper();
            // Chaque fiche commence sur une nouvelle page, dans l'ordre de la liste.
            assertThat(textePages(texte, document, 1, pagesParFiche)).contains("Awa Ndiaye").doesNotContain("Moussa Diop");
            assertThat(textePages(texte, document, pagesParFiche + 1, 2 * pagesParFiche)).contains("Moussa Diop").doesNotContain("Awa Ndiaye");
        }

        List<ContenuQrModel> qrs = detecteurQrPdf.detecter(pdf).stream().map(lecteurContenuQr::lire).toList();
        assertThat(qrs).hasSize(2).allSatisfy(qr -> assertThat(qr.type()).isEqualTo(GenerateurContenuQr.TYPE_FICHE_2_SUIVI));
        assertThat(qrs).extracting(ContenuQrModel::patientId).containsExactly(awa.getId(), moussa.getId());
        assertThat(qrs).extracting(ContenuQrModel::visiteId)
                .containsExactlyInAnyOrderElementsOf(visites.stream().map(VisiteSuivi::getId).toList());
    }

    @Test
    void admin_enTeteVisitesIds_dansLOrdreDesFiches() throws Exception {
        String ids = imprimer(fabriqueBadge.bearer("ADMIN"), List.of(moussa.getNumeroDossierProxymed(), awa.getNumeroDossierProxymed()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("X-Visites-Ids");
        List<UUID> visiteIds = Arrays.stream(ids.split(",")).map(UUID::fromString).toList();
        assertThat(visiteIds).hasSize(2);
        assertThat(visiteSuiviRepository.findById(visiteIds.get(0)).orElseThrow().getPatient().getId()).isEqualTo(moussa.getId());
        assertThat(visiteSuiviRepository.findById(visiteIds.get(1)).orElseThrow().getPatient().getId()).isEqualTo(awa.getId());
    }

    @Test
    void ficheVierge_terminer_400_medecinsNonAffectes() throws Exception {
        String ids = imprimer(fabriqueBadge.bearer("ADMIN"), List.of(awa.getNumeroDossierProxymed()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("X-Visites-Ids");

        mockMvc.perform(post("/api/visites/" + ids + "/terminer")
                        .header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer("MEDECIN_JUNIOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasItem(startsWith("medecinJunior"))))
                .andExpect(jsonPath("$.details", hasItem(startsWith("medecinSenior"))));
    }

    @Test
    void medecinJunior_403() throws Exception {
        imprimer(fabriqueBadge.bearer("MEDECIN_JUNIOR"), List.of(awa.getNumeroDossierProxymed()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erreur", is("Acces refuse")));
        assertThat(visiteSuiviRepository.count()).isZero();
    }

    @Test
    void medecinSenior_403() throws Exception {
        imprimer(fabriqueBadge.bearer("MEDECIN_SENIOR"), List.of(awa.getNumeroDossierProxymed()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erreur", is("Acces refuse")));
        assertThat(visiteSuiviRepository.count()).isZero();
    }

    @Test
    void sansBadge_401() throws Exception {
        imprimer(null, List.of(awa.getNumeroDossierProxymed()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erreur", is("Non authentifie")));
        assertThat(visiteSuiviRepository.count()).isZero();
    }

    @Test
    void listeVide_400() throws Exception {
        imprimer(fabriqueBadge.bearer("ADMIN"), List.of())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.erreur", is("Validation echouee")));
    }

    @Test
    void listeTropLongue_400() throws Exception {
        List<String> numeros = IntStream.rangeClosed(1, 51).mapToObj(i -> "PXM-LOT-" + i).toList();
        imprimer(fabriqueBadge.bearer("ADMIN"), numeros)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("au plus 50 fiches")));
        assertThat(visiteSuiviRepository.count()).isZero();
    }

    @Test
    void numeroVide_400() throws Exception {
        imprimer(fabriqueBadge.bearer("ADMIN"), List.of(awa.getNumeroDossierProxymed(), " "))
                .andExpect(status().isBadRequest());
        assertThat(visiteSuiviRepository.count()).isZero();
    }

    @Test
    void doublon_400_rienCree() throws Exception {
        imprimer(fabriqueBadge.bearer("ADMIN"), List.of(awa.getNumeroDossierProxymed(), awa.getNumeroDossierProxymed()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("en double")));
        assertThat(visiteSuiviRepository.count()).isZero();
    }

    @Test
    void numerosIntrouvables_404_listes_rienCree() throws Exception {
        imprimer(fabriqueBadge.bearer("ADMIN"), List.of(awa.getNumeroDossierProxymed(), "PXM-INCONNU-1", "PXM-INCONNU-2"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("introuvable")))
                .andExpect(jsonPath("$.details", containsInAnyOrder("PXM-INCONNU-1", "PXM-INCONNU-2")));
        assertThat(visiteSuiviRepository.count()).isZero();
    }

    private ResultActions imprimer(String bearer, List<String> numeros) throws Exception {
        var requete = post(URL).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("numerosDossierProxymed", numeros)));
        if (bearer != null) {
            requete.header(HttpHeaders.AUTHORIZATION, bearer);
        }
        return mockMvc.perform(requete);
    }

    private static String textePages(PDFTextStripper texte, PDDocument document, int debut, int fin) throws Exception {
        texte.setStartPage(debut);
        texte.setEndPage(fin);
        return texte.getText(document);
    }

    private Patient patient(String nom, Sexe sexe) {
        return patientRepository.save(Patient.builder()
                .numeroDossierProxymed("PXM-" + System.nanoTime())
                .nomComplet(nom)
                .dateNaissance(LocalDate.of(1960, 5, 14))
                .sexe(sexe)
                .build());
    }
}
