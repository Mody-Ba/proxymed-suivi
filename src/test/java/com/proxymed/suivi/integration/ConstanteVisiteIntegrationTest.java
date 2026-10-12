package com.proxymed.suivi.integration;

import com.proxymed.suivi.entity.Medecin;
import com.proxymed.suivi.entity.Patient;
import com.proxymed.suivi.enums.RoleMedecin;
import com.proxymed.suivi.enums.Sexe;
import com.proxymed.suivi.enums.StatutVisite;
import com.proxymed.suivi.repository.MedecinRepository;
import com.proxymed.suivi.repository.PatientRepository;
import com.proxymed.suivi.repository.VisiteSuiviRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConstanteVisiteIntegrationTest {

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
                .andExpect(jsonPath("$.alerteDeclenchee", is(false)))
                .andExpect(jsonPath("$.alertes", hasSize(0)))
                .andExpect(jsonPath("$.grilleConstantes", hasSize(8)))
                .andReturn().getResponse().getContentAsString();
        visiteId = objectMapper.readTree(reponse).get("id").asText();
    }

    @Test
    void grille_contientLes8ConstantesDeLaFiche_avecLeursValeursNormales() throws Exception {
        mockMvc.perform(get("/api/visites/" + visiteId + "/constantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].type", contains(
                        "TA", "FC", "FR", "SPO2", "TEMPERATURE", "GLYCEMIE", "EVA", "GLASGOW")))
                .andExpect(jsonPath("$[0].libelle", is("Tension artérielle")))
                .andExpect(jsonPath("$[0].unite", is("mmHg")))
                .andExpect(jsonPath("$[0].valeursNormales", is("< 140/90")))
                .andExpect(jsonPath("$[0].mesures", hasSize(0)));
    }

    @Test
    void mesuresNormales_neDeclenchentPasDAlerte() throws Exception {
        enregistrer("TA", "H0", "130", "80").andExpect(status().isCreated())
                .andExpect(jsonPath("$.alerte", is(false)))
                .andExpect(jsonPath("$.seuilFranchi", nullValue()));
        enregistrer("FC", "H0", "78", null).andExpect(status().isCreated());
        enregistrer("TEMPERATURE", "H0", "37.2", null).andExpect(status().isCreated());

        mockMvc.perform(get("/api/visites/" + visiteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerteDeclenchee", is(false)))
                .andExpect(jsonPath("$.alertes", hasSize(0)))
                .andExpect(jsonPath("$.grilleConstantes[0].mesures[0].valeur", is(130)))
                .andExpect(jsonPath("$.grilleConstantes[0].mesures[0].valeurDiastolique", is(80)));
    }

    @Test
    void constanteHorsSeuil_declencheUneAlerteDansLeDetailDeLaFiche() throws Exception {
        enregistrer("TA", "H0", "190", "100").andExpect(status().isCreated())
                .andExpect(jsonPath("$.alerte", is(true)))
                .andExpect(jsonPath("$.seuilFranchi", is("TA > 180/110 mmHg")));
        enregistrer("SPO2", "H_PLUS_1", "88", null).andExpect(status().isCreated());
        enregistrer("FC", "H0", "80", null).andExpect(status().isCreated());

        mockMvc.perform(get("/api/visites/" + visiteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerteDeclenchee", is(true)))
                .andExpect(jsonPath("$.alertes", hasSize(2)))
                .andExpect(jsonPath("$.alertes[0].constante", is("TA")))
                .andExpect(jsonPath("$.alertes[0].creneau", is("H0")))
                .andExpect(jsonPath("$.alertes[0].valeur", is(190)))
                .andExpect(jsonPath("$.alertes[0].valeurDiastolique", is(100)))
                .andExpect(jsonPath("$.alertes[0].seuilFranchi", is("TA > 180/110 mmHg")))
                .andExpect(jsonPath("$.alertes[1].constante", is("SPO2")))
                .andExpect(jsonPath("$.alertes[1].creneau", is("H_PLUS_1")))
                .andExpect(jsonPath("$.alertes[1].seuilFranchi", is("SpO2 < 90 %")));
    }

    @Test
    void memeTypeEtMemeCreneau_remplaceLaMesure_etLAlerteDisparait() throws Exception {
        enregistrer("FC", "H0", "135", null).andExpect(jsonPath("$.alerte", is(true)));
        enregistrer("FC", "H0", "82", null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.alerte", is(false)));
        enregistrer("FC", "H_PLUS_1", "84", null).andExpect(status().isCreated());

        mockMvc.perform(get("/api/visites/" + visiteId + "/constantes"))
                .andExpect(jsonPath("$[1].type", is("FC")))
                .andExpect(jsonPath("$[1].mesures", hasSize(2)))
                .andExpect(jsonPath("$[1].mesures[0].creneau", is("H0")))
                .andExpect(jsonPath("$[1].mesures[0].valeur", is(82)))
                .andExpect(jsonPath("$[1].mesures[1].creneau", is("H_PLUS_1")));

        mockMvc.perform(get("/api/visites/" + visiteId))
                .andExpect(jsonPath("$.alerteDeclenchee", is(false)));
    }

    @Test
    void suppressionDUneMesure_laRetireDeLaGrilleEtDesAlertes() throws Exception {
        String reponse = enregistrer("GLASGOW", "H0", "11", null).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long constanteId = objectMapper.readTree(reponse).get("id").asLong();

        mockMvc.perform(get("/api/visites/" + visiteId))
                .andExpect(jsonPath("$.alerteDeclenchee", is(true)));

        mockMvc.perform(delete("/api/visites/" + visiteId + "/constantes/" + constanteId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/visites/" + visiteId))
                .andExpect(jsonPath("$.alerteDeclenchee", is(false)))
                .andExpect(jsonPath("$.grilleConstantes[7].mesures", hasSize(0)));

        mockMvc.perform(delete("/api/visites/" + visiteId + "/constantes/" + constanteId))
                .andExpect(status().isNotFound());
    }

    @Test
    void tension_exigeLaDiastolique_etLesAutresTypesLaRefusent() throws Exception {
        enregistrer("TA", "H0", "130", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur", is("Regle de gestion non respectee")));
        enregistrer("FC", "H0", "80", "60").andExpect(status().isBadRequest());
    }

    @Test
    void valeursImpossibles_sontRefusees() throws Exception {
        enregistrer("SPO2", "H0", "140", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("impossible")));
        enregistrer("TEMPERATURE", "H0", "370", null).andExpect(status().isBadRequest());
        enregistrer("EVA", "H0", "11", null).andExpect(status().isBadRequest());
        enregistrer("TA", "H0", "80", "120").andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/visites/" + visiteId + "/constantes"))
                .andExpect(jsonPath("$[*].mesures[*]", hasSize(0)));
    }

    @Test
    void champsObligatoiresEtValeursInconnues_sontRefuses() throws Exception {
        mockMvc.perform(post("/api/visites/" + visiteId + "/constantes")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasSize(3)));
        enregistrer("POIDS", "H0", "70", null).andExpect(status().isBadRequest());
        enregistrer("FC", "H_PLUS_5", "70", null).andExpect(status().isBadRequest());
    }

    @Test
    void signeAlerteCliniqueEtAppelDuSenior_passentParLePutDeLaVisite() throws Exception {
        mockMvc.perform(put("/api/visites/" + visiteId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "signesAlerteCliniques": ["DOULEUR_THORACIQUE", "DETRESSE_RESPIRATOIRE"],
                                  "heureAppelSenior": "09:40:00",
                                  "heureReponseSenior": "09:43:00",
                                  "decisionAppelSenior": "Transfert vers EPS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerteDeclenchee", is(true)))
                .andExpect(jsonPath("$.signesAlerteCliniques", contains("DETRESSE_RESPIRATOIRE", "DOULEUR_THORACIQUE")))
                .andExpect(jsonPath("$.alertes", hasSize(2)))
                .andExpect(jsonPath("$.alertes[0].signeClinique", is("DETRESSE_RESPIRATOIRE")))
                .andExpect(jsonPath("$.alertes[0].seuilFranchi", is("Détresse respiratoire")))
                .andExpect(jsonPath("$.heureAppelSenior", is("09:40:00")))
                .andExpect(jsonPath("$.decisionAppelSenior", is("Transfert vers EPS")));

        // Liste vide = tout decocher ; l'appel du senior deja saisi est conserve.
        mockMvc.perform(put("/api/visites/" + visiteId)
                        .contentType("application/json")
                        .content("""
                                {"signesAlerteCliniques": []}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerteDeclenchee", is(false)))
                .andExpect(jsonPath("$.signesAlerteCliniques", hasSize(0)))
                .andExpect(jsonPath("$.heureAppelSenior", is("09:40:00")));
    }

    @Test
    void alerteDeclenchee_nEstJamaisSaisie() throws Exception {
        mockMvc.perform(put("/api/visites/" + visiteId)
                        .contentType("application/json")
                        .content("""
                                {"alerteDeclenchee": true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Champ inconnu dans la requete : alerteDeclenchee")));
    }

    @Test
    void ficheNonModifiable_ouVisiteIntrouvable() throws Exception {
        mockMvc.perform(post("/api/visites/" + UUID.randomUUID() + "/constantes")
                        .contentType("application/json")
                        .content(corps("FC", "H0", "80", null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/visites/" + UUID.randomUUID() + "/constantes"))
                .andExpect(status().isNotFound());

        String reponse = enregistrer("FC", "H0", "80", null).andReturn().getResponse().getContentAsString();
        long constanteId = objectMapper.readTree(reponse).get("id").asLong();
        // Pas encore d'endpoint "terminer" (etape 4) : on force le statut via le repository.
        visiteSuiviRepository.findById(UUID.fromString(visiteId)).orElseThrow().setStatut(StatutVisite.TERMINEE);

        enregistrer("FC", "H_PLUS_1", "82", null).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/visites/" + visiteId + "/constantes/" + constanteId))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/visites/" + visiteId + "/constantes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].mesures", hasSize(1)));
    }

    private ResultActions enregistrer(String type, String creneau, String valeur, String diastolique) throws Exception {
        return mockMvc.perform(post("/api/visites/" + visiteId + "/constantes")
                .contentType("application/json")
                .content(corps(type, creneau, valeur, diastolique)));
    }

    private String corps(String type, String creneau, String valeur, String diastolique) {
        String dia = diastolique != null ? ", \"valeurDiastolique\": " + diastolique : "";
        return "{\"type\": \"" + type + "\", \"creneau\": \"" + creneau + "\", \"valeur\": " + valeur + dia + "}";
    }
}
