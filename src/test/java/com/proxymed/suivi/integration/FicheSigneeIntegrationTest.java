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
import static org.hamcrest.Matchers.containsString;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FicheSigneeIntegrationTest {

    private static final byte[] PDF = "%PDF-1.7\nfiche signee".getBytes(StandardCharsets.US_ASCII);

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

    private String visiteId;

    @BeforeEach
    void setUp() throws Exception {
        visiteId = creerVisite();
    }

    @Test
    void import_puisTelechargement_renvoieLeMemePdf() throws Exception {
        importer(visiteId, new MockMultipartFile("fichier", "fiche-signee.pdf", "application/pdf", PDF))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nomFichier", is("fiche-signee.pdf")))
                .andExpect(jsonPath("$.tailleOctets", is(PDF.length)))
                .andExpect(jsonPath("$.dateImport", notNullValue()))
                // Metadonnees seulement : jamais le contenu du fichier dans le JSON.
                .andExpect(jsonPath("$.contenu").doesNotExist());

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/visites/" + visiteId + "/fiche-signee"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        startsWith("attachment; filename=\"fiche-signee.pdf\"")))
                .andExpect(content().bytes(PDF));
    }

    @Test
    void telechargement_renvoieLaFicheLaPlusRecente() throws Exception {
        UUID id = UUID.fromString(visiteId);
        byte[] recent = "%PDF-1.7\nversion corrigee".getBytes(StandardCharsets.US_ASCII);
        enregistrerFiche(id, "fiche-ancienne.pdf", Instant.parse("2026-10-08T12:00:00Z"), PDF);
        enregistrerFiche(id, "fiche-recente.pdf", Instant.parse("2026-10-09T12:00:00Z"), recent);
        enregistrerFiche(id, "fiche-intermediaire.pdf", Instant.parse("2026-10-08T18:00:00Z"), PDF);

        mockMvc.perform(get("/api/visites/" + visiteId + "/fiche-signee"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        startsWith("attachment; filename=\"fiche-recente.pdf\"")))
                .andExpect(content().bytes(recent));
    }

    @Test
    void import_rejetteCeQuiNEstPasUnVraiPdf() throws Exception {
        importer(visiteId, new MockMultipartFile("fichier", "photo.pdf", "application/pdf",
                "pas un pdf".getBytes(StandardCharsets.US_ASCII)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur", is("Regle de gestion non respectee")))
                .andExpect(jsonPath("$.message", is("Le fichier importe doit etre un PDF")));
        importer(visiteId, new MockMultipartFile("fichier", "fiche.png", "image/png", PDF))
                .andExpect(status().isBadRequest());
        importer(visiteId, new MockMultipartFile("fichier", "vide.pdf", "application/pdf", new byte[0]))
                .andExpect(status().isBadRequest());

        assertThat(ficheSigneeRepository.findFirstByVisite_IdOrderByDateImportDesc(UUID.fromString(visiteId))).isEmpty();
    }

    @Test
    void import_rejetteUnFichierDePlusDe10Mo() throws Exception {
        byte[] gros = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy(PDF, 0, gros, 0, PDF.length);

        importer(visiteId, new MockMultipartFile("fichier", "gros.pdf", "application/pdf", gros))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("10 Mo")));
    }

    @Test
    void import_sansFichier_renvoie400() throws Exception {
        mockMvc.perform(multipart("/api/visites/" + visiteId + "/fiche-signee"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("fichier")));
    }

    @Test
    void importEtTelechargement_404_siVisiteIntrouvable_ouAucuneFiche() throws Exception {
        String inconnue = UUID.randomUUID().toString();
        importer(inconnue, new MockMultipartFile("fichier", "fiche.pdf", "application/pdf", PDF))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/visites/" + inconnue + "/fiche-signee").accept("application/pdf"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Visite introuvable")));

        // Swagger demande application/pdf : l'erreur doit quand meme revenir en JSON lisible.
        mockMvc.perform(get("/api/visites/" + visiteId + "/fiche-signee").accept("application/pdf"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Aucune fiche signee")))
                .andExpect(jsonPath("$.details", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void nomDeFichierAccentue_estTelechargeAvecSonNom() throws Exception {
        importer(visiteId, new MockMultipartFile("fichier", "fiche signée.pdf", "application/pdf", PDF))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomFichier", is("fiche signée.pdf")));

        mockMvc.perform(get("/api/visites/" + visiteId + "/fiche-signee"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("filename*=UTF-8''fiche%20sign%C3%A9e.pdf")));
    }

    private ResultActions importer(String id, MockMultipartFile fichier) throws Exception {
        return mockMvc.perform(multipart("/api/visites/" + id + "/fiche-signee").file(fichier));
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

    private String creerVisite() throws Exception {
        Patient patient = patientRepository.save(Patient.builder()
                .numeroDossierProxymed("PXM-" + System.nanoTime())
                .nomComplet("Abdoulaye Fall")
                .dateNaissance(LocalDate.of(1968, 11, 27))
                .sexe(Sexe.M)
                .build());
        Medecin senior = medecinRepository.save(Medecin.builder()
                .nom("Diop").prenom("Aminata").numeroOrdre("SEN-" + System.nanoTime()).role(RoleMedecin.SENIOR).build());
        Medecin junior = medecinRepository.save(Medecin.builder()
                .nom("Ndiaye").prenom("Ousmane").numeroOrdre("JUN-" + System.nanoTime()).role(RoleMedecin.JUNIOR).build());

        String reponse = mockMvc.perform(post("/api/patients/" + patient.getId() + "/visites")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "medecinJuniorId", junior.getId(),
                                "medecinSeniorId", senior.getId()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(reponse).get("id").asText();
    }
}
