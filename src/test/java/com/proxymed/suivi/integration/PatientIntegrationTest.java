package com.proxymed.suivi.integration;

import com.proxymed.suivi.enums.Sexe;
import com.proxymed.suivi.service.model.PatientRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PatientIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void creation_puisLecture_etListe() throws Exception {
        PatientRequest creation = requete("PXM-" + System.nanoTime());

        String reponse = mockMvc.perform(post("/api/patients")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(creation)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nomComplet", is("Abdoulaye Fall")))
                .andExpect(jsonPath("$.sexe", is("M")))
                .andExpect(jsonPath("$.age", greaterThanOrEqualTo(57)))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(reponse).get("id").asText();

        mockMvc.perform(get("/api/patients/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroDossierProxymed", is(creation.numeroDossierProxymed())));

        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]").exists());
    }

    @Test
    void create_rejette_siChampsObligatoiresManquants() throws Exception {
        PatientRequest invalide = new PatientRequest("", null, "", null, null, null, null, null, null);

        mockMvc.perform(post("/api/patients")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalide)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasSize(4)));
    }

    @Test
    void create_rejette_siNumeroDossierDejaUtilise() throws Exception {
        PatientRequest creation = requete("PXM-" + System.nanoTime());
        String json = objectMapper.writeValueAsString(creation);

        mockMvc.perform(post("/api/patients").contentType("application/json").content(json))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/patients").contentType("application/json").content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur", is("Regle de gestion non respectee")));
    }

    @Test
    void create_rejette_siChampInconnu() throws Exception {
        mockMvc.perform(post("/api/patients")
                        .contentType("application/json")
                        .content("""
                                {"numeroDossierProxymed":"PXM-X","nomComplet":"Test","dateNaissance":"1960-01-01","sexe":"F","age":66}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Champ inconnu dans la requete : age")));
    }

    @Test
    void findById_retourne404_siInexistant() throws Exception {
        mockMvc.perform(get("/api/patients/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private PatientRequest requete(String numeroDossier) {
        return new PatientRequest(numeroDossier, "DMI-1001", "Abdoulaye Fall", LocalDate.of(1968, 11, 27),
                Sexe.M, "+221 76 221 45 67", "Rue 10 x 13, Medina", "Dakar Plateau", "Medina");
    }
}
