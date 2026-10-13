package com.proxymed.suivi.integration;

import com.proxymed.suivi.entity.Medecin;
import com.proxymed.suivi.entity.Patient;
import com.proxymed.suivi.enums.RoleMedecin;
import com.proxymed.suivi.enums.Sexe;
import com.proxymed.suivi.repository.MedecinRepository;
import com.proxymed.suivi.repository.PatientRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PdfVisiteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private MedecinRepository medecinRepository;

    private String visiteId;

    @BeforeEach
    void setUp() throws Exception {
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
        visiteId = objectMapper.readTree(reponse).get("id").asText();
    }

    @Test
    void pdf_estTelechargeDirectement_etContientLesDonneesDeLaVisite() throws Exception {
        mockMvc.perform(put("/api/visites/" + visiteId).contentType("application/json")
                        .content("""
                                {"etatGeneral": "BON", "resumeClinique": "Patient stable", "themesAbordes": ["HYGIENE_PREVENTION"]}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/visites/" + visiteId + "/constantes").contentType("application/json")
                        .content("""
                                {"type": "TA", "creneau": "H0", "valeur": 190, "valeurDiastolique": 100}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/visites/" + visiteId + "/traitements").contentType("application/json")
                        .content("""
                                {"medicament": "Amlodipine 5 mg", "posologie": "1 cp le matin"}
                                """))
                .andExpect(status().isCreated());

        byte[] pdf = mockMvc.perform(get("/api/visites/" + visiteId + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"fiche-visite-" + visiteId + ".pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String texte = new PDFTextStripper().getText(document);
            assertThat(texte).contains("Abdoulaye Fall", "BROUILLON", "Visite n° 1", "Dr Ousmane Ndiaye",
                    "190/100", "ALERTE DÉCLENCHÉE", "Amlodipine 5 mg", "Patient stable", visiteId);
        }
    }

    @Test
    void pdf_retourne404EnJson_siVisiteIntrouvable() throws Exception {
        // Swagger demande application/pdf : l'erreur doit quand meme revenir en JSON lisible.
        mockMvc.perform(get("/api/visites/" + UUID.randomUUID() + "/pdf").accept("application/pdf"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erreur", is("Ressource introuvable")));
    }
}
