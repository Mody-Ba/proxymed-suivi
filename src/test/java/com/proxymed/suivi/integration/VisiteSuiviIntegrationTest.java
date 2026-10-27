package com.proxymed.suivi.integration;

import com.proxymed.suivi.entity.Medecin;
import com.proxymed.suivi.entity.Patient;
import com.proxymed.suivi.enums.RoleMedecin;
import com.proxymed.suivi.enums.Sexe;
import com.proxymed.suivi.enums.StatutVisite;
import com.proxymed.suivi.repository.MedecinRepository;
import com.proxymed.suivi.repository.PatientRepository;
import com.proxymed.suivi.repository.VisiteSuiviRepository;
import com.proxymed.suivi.support.BadgeMedecinParDefaut;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(BadgeMedecinParDefaut.class)
@Transactional
class VisiteSuiviIntegrationTest {

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

    private Patient patient;
    private Medecin medecinSenior;
    private Medecin medecinJunior;

    @BeforeEach
    void setUp() {
        patient = creerPatient("Abdoulaye Fall");
        medecinSenior = medecinRepository.save(Medecin.builder()
                .nom("Diop").prenom("Aminata").numeroOrdre("SEN-" + System.nanoTime()).role(RoleMedecin.SENIOR).build());
        medecinJunior = medecinRepository.save(Medecin.builder()
                .nom("Ndiaye").prenom("Ousmane").numeroOrdre("JUN-" + System.nanoTime()).role(RoleMedecin.JUNIOR).build());
    }

    @Test
    void creation_puisLecture_puisMiseAJourPartielle() throws Exception {
        String reponse = mockMvc.perform(post("/api/patients/" + patient.getId() + "/visites")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "medecinJuniorId", medecinJunior.getId(),
                                "medecinSeniorId", medecinSenior.getId(),
                                "dateVisite", "2026-10-05",
                                "heureArrivee", "09:30:00",
                                "typeVisite", "PROGRAMMEE",
                                "contactSenior", "+221 77 640 12 30"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut", is("BROUILLON")))
                .andExpect(jsonPath("$.numeroVisite", is(1)))
                .andExpect(jsonPath("$.patient.nomComplet", is("Abdoulaye Fall")))
                .andExpect(jsonPath("$.patient.sexe", is("M")))
                .andExpect(jsonPath("$.patient.age", notNullValue()))
                .andExpect(jsonPath("$.medecinJunior.role", is("JUNIOR")))
                .andExpect(jsonPath("$.medecinSenior.nom", is("Diop")))
                .andExpect(jsonPath("$.heureDepart", nullValue()))
                .andReturn().getResponse().getContentAsString();
        String visiteId = objectMapper.readTree(reponse).get("id").asText();

        mockMvc.perform(get("/api/visites/" + visiteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateVisite", is("2026-10-05")))
                .andExpect(jsonPath("$.heureArrivee", is("09:30:00")))
                .andExpect(jsonPath("$.typeVisite", is("PROGRAMMEE")));

        // PUT partiel : seuls les champs envoyes changent
        mockMvc.perform(put("/api/visites/" + visiteId)
                        .contentType("application/json")
                        .content("""
                                {"heureDepart":"11:15:00","contactSeniorJoignable":true,"adresseVisite":"Chez sa fille, Ouakam"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heureDepart", is("11:15:00")))
                .andExpect(jsonPath("$.contactSeniorJoignable", is(true)))
                .andExpect(jsonPath("$.adresseVisite", is("Chez sa fille, Ouakam")))
                .andExpect(jsonPath("$.heureArrivee", is("09:30:00")))
                .andExpect(jsonPath("$.typeVisite", is("PROGRAMMEE")))
                .andExpect(jsonPath("$.contactSenior", is("+221 77 640 12 30")))
                .andExpect(jsonPath("$.numeroVisite", is(1)));
    }

    @Test
    void creation_avecLeMinimum_appliqueLesValeursParDefaut() throws Exception {
        mockMvc.perform(post("/api/patients/" + patient.getId() + "/visites")
                        .contentType("application/json")
                        .content(corpsMinimal()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dateVisite", is(LocalDate.now().toString())))
                .andExpect(jsonPath("$.heureArrivee", notNullValue()))
                .andExpect(jsonPath("$.dateCreation", notNullValue()));
    }

    @Test
    void numeroDeVisite_estUnCumulParPatient_etLaListeEstDeLaPlusRecenteALaPlusAncienne() throws Exception {
        Patient autrePatient = creerPatient("Fatou Sow");
        creerVisite(patient.getId());
        creerVisite(patient.getId());
        creerVisite(autrePatient.getId());
        String troisieme = creerVisite(patient.getId());

        mockMvc.perform(get("/api/visites/" + troisieme))
                .andExpect(jsonPath("$.numeroVisite", is(3)));

        mockMvc.perform(get("/api/patients/" + patient.getId() + "/visites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].numeroVisite", is(3)))
                .andExpect(jsonPath("$[1].numeroVisite", is(2)))
                .andExpect(jsonPath("$[2].numeroVisite", is(1)))
                .andExpect(jsonPath("$[0].medecinJuniorNomComplet", is("Ousmane Ndiaye")));

        mockMvc.perform(get("/api/patients/" + autrePatient.getId() + "/visites"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numeroVisite", is(1)));
    }

    @Test
    void creation_rejette_siNumeroVisiteFourniParLeClient() throws Exception {
        mockMvc.perform(post("/api/patients/" + patient.getId() + "/visites")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "medecinJuniorId", medecinJunior.getId(),
                                "medecinSeniorId", medecinSenior.getId(),
                                "numeroVisite", 12))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Champ inconnu dans la requete : numeroVisite")));
    }

    @Test
    void creation_rejette_siMedecinsManquants() throws Exception {
        mockMvc.perform(post("/api/patients/" + patient.getId() + "/visites")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasSize(2)));
    }

    @Test
    void creation_retourne404_siPatientOuMedecinIntrouvable() throws Exception {
        mockMvc.perform(post("/api/patients/" + UUID.randomUUID() + "/visites")
                        .contentType("application/json")
                        .content(corpsMinimal()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/patients/" + patient.getId() + "/visites")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "medecinJuniorId", 999999,
                                "medecinSeniorId", medecinSenior.getId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void creation_rejette_siLesRolesDesMedecinsSontInverses() throws Exception {
        mockMvc.perform(post("/api/patients/" + patient.getId() + "/visites")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "medecinJuniorId", medecinSenior.getId(),
                                "medecinSeniorId", medecinJunior.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur", is("Regle de gestion non respectee")));
    }

    @Test
    void miseAJour_rejette_siMedecinSeniorNAPasLeRoleSenior() throws Exception {
        String visiteId = creerVisite(patient.getId());

        mockMvc.perform(put("/api/visites/" + visiteId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("medecinSeniorId", medecinJunior.getId()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void miseAJour_retourne409_siLaFicheNEstPlusEnBrouillon() throws Exception {
        String visiteId = creerVisite(patient.getId());
        // Pas encore d'endpoint "terminer" (etape 4) : on force le statut via le repository.
        visiteSuiviRepository.findById(UUID.fromString(visiteId)).orElseThrow().setStatut(StatutVisite.TERMINEE);

        mockMvc.perform(put("/api/visites/" + visiteId)
                        .contentType("application/json")
                        .content("""
                                {"heureDepart":"12:00:00"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void lecture_retourne404_siVisiteOuPatientIntrouvable() throws Exception {
        mockMvc.perform(get("/api/visites/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/patients/" + UUID.randomUUID() + "/visites"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/visites/" + UUID.randomUUID()).contentType("application/json").content("{}"))
                .andExpect(status().isNotFound());
    }

    private Patient creerPatient(String nomComplet) {
        return patientRepository.save(Patient.builder()
                .numeroDossierProxymed("PXM-" + System.nanoTime())
                .nomComplet(nomComplet)
                .dateNaissance(LocalDate.of(1968, 11, 27))
                .sexe(Sexe.M)
                .build());
    }

    private String corpsMinimal() {
        return objectMapper.writeValueAsString(Map.of(
                "medecinJuniorId", medecinJunior.getId(),
                "medecinSeniorId", medecinSenior.getId()));
    }

    private String creerVisite(UUID patientId) throws Exception {
        String reponse = mockMvc.perform(post("/api/patients/" + patientId + "/visites")
                        .contentType("application/json")
                        .content(corpsMinimal()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(reponse).get("id").asText();
    }
}
