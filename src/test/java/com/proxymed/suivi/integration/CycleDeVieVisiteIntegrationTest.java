package com.proxymed.suivi.integration;

import com.proxymed.suivi.entity.Medecin;
import com.proxymed.suivi.entity.Patient;
import com.proxymed.suivi.enums.RoleMedecin;
import com.proxymed.suivi.enums.Sexe;
import com.proxymed.suivi.repository.MedecinRepository;
import com.proxymed.suivi.repository.PatientRepository;
import jakarta.persistence.EntityManager;
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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cycle de vie de la fiche (BROUILLON -> TERMINEE -> VISEE), signature du patient et liste
 * des fiches en attente de visa.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CycleDeVieVisiteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private MedecinRepository medecinRepository;
    @Autowired
    private EntityManager entityManager;

    private String visiteId;

    @BeforeEach
    void setUp() throws Exception {
        visiteId = creerVisite();
    }

    @Test
    void cycleComplet_terminer_viser_signaturePatient() throws Exception {
        completer(visiteId);

        mockMvc.perform(post("/api/visites/" + visiteId + "/terminer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("TERMINEE")))
                .andExpect(jsonPath("$.dateSignatureJunior", notNullValue()))
                .andExpect(jsonPath("$.echeanceVisa", notNullValue()))
                .andExpect(jsonPath("$.visaEnRetard", is(false)))
                .andExpect(jsonPath("$.dateVisaSenior", nullValue()));

        // Fiche terminee : immuable
        mockMvc.perform(put("/api/visites/" + visiteId).contentType("application/json")
                        .content("""
                                {"resumeClinique": "Modifie apres coup"}
                                """))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/visites/" + visiteId + "/terminer"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/visites/" + visiteId + "/viser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("VISEE")))
                .andExpect(jsonPath("$.dateVisaSenior", notNullValue()));
        mockMvc.perform(post("/api/visites/" + visiteId + "/viser"))
                .andExpect(status().isConflict());

        signer(visiteId, "AIDANT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("VISEE")))
                .andExpect(jsonPath("$.signatairePatient", is("AIDANT")))
                .andExpect(jsonPath("$.dateSignaturePatient", notNullValue()));
        signer(visiteId, "PATIENT")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("deja")));

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/visites/" + visiteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("VISEE")))
                .andExpect(jsonPath("$.dateSignatureJunior", notNullValue()))
                .andExpect(jsonPath("$.dateVisaSenior", notNullValue()))
                .andExpect(jsonPath("$.signatairePatient", is("AIDANT")))
                .andExpect(jsonPath("$.resumeClinique", is("Patient stable")));
    }

    @Test
    void terminer_rejetteUneFicheIncomplete_etListeCeQuiManque() throws Exception {
        mockMvc.perform(post("/api/visites/" + visiteId + "/terminer"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur", is("Regle de gestion non respectee")))
                .andExpect(jsonPath("$.message", containsString("ne peut pas etre terminee")))
                .andExpect(jsonPath("$.details", hasSize(9)))
                .andExpect(jsonPath("$.details", hasItem(startsWith("heureDepart"))))
                .andExpect(jsonPath("$.details", hasItem(
                        "constantes : mesures du creneau H0 manquantes (TA, FC, FR, SPO2, TEMPERATURE, GLYCEMIE, EVA, GLASGOW)")));

        mockMvc.perform(get("/api/visites/" + visiteId))
                .andExpect(jsonPath("$.statut", is("BROUILLON")))
                .andExpect(jsonPath("$.dateSignatureJunior", nullValue()));
    }

    @Test
    void terminer_appliqueLesTroisControlesConditionnels() throws Exception {
        completer(visiteId);
        // Alerte (FC a 135), etat general altere sans precision, signalement DASS sans heure de contact.
        constante(visiteId, "FC", "H0", "135", null);
        mettreAJour(visiteId, """
                {"etatGeneral": "ALTERE", "signalementDass": true}
                """);

        mockMvc.perform(post("/api/visites/" + visiteId + "/terminer"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasSize(4)))
                .andExpect(jsonPath("$.details[0]", startsWith("etatGeneralPrecision")))
                .andExpect(jsonPath("$.details[1]", startsWith("heureAppelSenior")))
                .andExpect(jsonPath("$.details[2]", startsWith("decisionAppelSenior")))
                .andExpect(jsonPath("$.details[3]", startsWith("heureContactDass")));

        mettreAJour(visiteId, """
                {"etatGeneralPrecision": "Asthénie", "heureAppelSenior": "09:40:00",
                 "decisionAppelSenior": "Surveillance rapprochée", "heureContactDass": "10:30:00"}
                """);
        mockMvc.perform(post("/api/visites/" + visiteId + "/terminer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("TERMINEE")))
                .andExpect(jsonPath("$.alerteDeclenchee", is(true)));
    }

    @Test
    void viserEtSignaturePatient_sontRefusesSurUnBrouillon() throws Exception {
        completer(visiteId);
        mockMvc.perform(post("/api/visites/" + visiteId + "/viser"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erreur", is("Action incompatible avec le statut de la fiche")));
        signer(visiteId, "PATIENT").andExpect(status().isConflict());
    }

    @Test
    void signaturePatient_possibleDesQueLaFicheEstTerminee_etSignataireObligatoire() throws Exception {
        completer(visiteId);
        mockMvc.perform(post("/api/visites/" + visiteId + "/terminer")).andExpect(status().isOk());

        mockMvc.perform(post("/api/visites/" + visiteId + "/signature-patient")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasSize(1)));
        signer(visiteId, "VOISIN").andExpect(status().isBadRequest());

        signer(visiteId, "PATIENT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("TERMINEE")))
                .andExpect(jsonPath("$.signatairePatient", is("PATIENT")));
        // La signature du patient n'empeche pas le visa du senior.
        mockMvc.perform(post("/api/visites/" + visiteId + "/viser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signatairePatient", is("PATIENT")));
    }

    @Test
    void enAttenteDeVisa_neListeQueLesFichesTerminees_dansLOrdreDeSignature() throws Exception {
        String brouillon = visiteId;
        String premiere = creerVisite();
        String seconde = creerVisite();
        String visee = creerVisite();
        for (String id : new String[]{premiere, seconde, visee}) {
            completer(id);
            mockMvc.perform(post("/api/visites/" + id + "/terminer")).andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/visites/" + visee + "/viser")).andExpect(status().isOk());

        mockMvc.perform(get("/api/visites/en-attente-visa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(premiere, seconde)))
                .andExpect(jsonPath("$[0].patientNomComplet", is("Abdoulaye Fall")))
                .andExpect(jsonPath("$[0].numeroDossierProxymed", startsWith("PXM-")))
                .andExpect(jsonPath("$[0].medecinSeniorNomComplet", is("Aminata Diop")))
                .andExpect(jsonPath("$[0].decision", is("PATIENT_STABLE_SUIVI_PROGRAMME")))
                .andExpect(jsonPath("$[0].alerteDeclenchee", is(false)))
                .andExpect(jsonPath("$[0].dateSignatureJunior", notNullValue()))
                .andExpect(jsonPath("$[0].echeanceVisa", notNullValue()))
                .andExpect(jsonPath("$[0].visaEnRetard", is(false)));

        mockMvc.perform(get("/api/visites/" + brouillon)).andExpect(jsonPath("$.statut", is("BROUILLON")));
    }

    @Test
    void delaiDuCompteRendu_etSaisieDmi_sontRenvoyesDansLeDetail() throws Exception {
        mettreAJour(visiteId, """
                {"dateVisite": "2026-10-05", "heureDepart": "11:00:00", "dateEnvoiCompteRendu": "2026-10-06T08:00:00",
                 "dateSaisieDmi": "2026-10-06", "saisieDmiPar": "Dr Ndiaye"}
                """)
                .andExpect(jsonPath("$.echeanceCompteRendu", is("2026-10-05T23:00:00")))
                .andExpect(jsonPath("$.compteRenduEnRetard", is(true)))
                .andExpect(jsonPath("$.dateSaisieDmi", is("2026-10-06")))
                .andExpect(jsonPath("$.saisieDmiPar", is("Dr Ndiaye")))
                .andExpect(jsonPath("$.echeanceVisa", nullValue()))
                .andExpect(jsonPath("$.visaEnRetard", nullValue()));

        mettreAJour(visiteId, """
                {"dateEnvoiCompteRendu": "2026-10-05T18:30:00"}
                """).andExpect(jsonPath("$.compteRenduEnRetard", is(false)));
    }

    @Test
    void lesSignaturesNePassentPasParLePut_et404SiVisiteIntrouvable() throws Exception {
        for (String champ : new String[]{"statut", "dateSignatureJunior", "dateVisaSenior", "signatairePatient"}) {
            mockMvc.perform(put("/api/visites/" + visiteId).contentType("application/json")
                            .content("{\"" + champ + "\": null}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("Champ inconnu dans la requete : " + champ)));
        }

        String inconnue = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/visites/" + inconnue + "/terminer")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/visites/" + inconnue + "/viser")).andExpect(status().isNotFound());
        signer(inconnue, "PATIENT").andExpect(status().isNotFound());
    }

    /**
     * Remplit tout ce que la regle 8 exige pour terminer la fiche, sans alerte.
     */
    private void completer(String id) throws Exception {
        mettreAJour(id, """
                {
                  "heureDepart": "11:00:00",
                  "typeVisite": "PROGRAMMEE",
                  "contactSenior": "+221 77 640 12 30",
                  "etatGeneral": "BON",
                  "observanceTraitement": "BONNE",
                  "situationSociale": "STABLE",
                  "decision": "PATIENT_STABLE_SUIVI_PROGRAMME",
                  "resumeClinique": "Patient stable"
                }
                """).andExpect(status().isOk());
        constante(id, "TA", "H0", "130", "80");
        constante(id, "FC", "H0", "78", null);
        constante(id, "FR", "H0", "16", null);
        constante(id, "SPO2", "H0", "98", null);
        constante(id, "TEMPERATURE", "H0", "37", null);
        constante(id, "GLYCEMIE", "H0", "1", null);
        constante(id, "EVA", "H0", "2", null);
        constante(id, "GLASGOW", "H0", "15", null);
    }

    private ResultActions mettreAJour(String id, String corps) throws Exception {
        return mockMvc.perform(put("/api/visites/" + id).contentType("application/json").content(corps));
    }

    private void constante(String id, String type, String creneau, String valeur, String diastolique) throws Exception {
        String dia = diastolique != null ? ", \"valeurDiastolique\": " + diastolique : "";
        mockMvc.perform(post("/api/visites/" + id + "/constantes").contentType("application/json")
                        .content("{\"type\": \"" + type + "\", \"creneau\": \"" + creneau + "\", \"valeur\": " + valeur + dia + "}"))
                .andExpect(status().isCreated());
    }

    private ResultActions signer(String id, String signataire) throws Exception {
        return mockMvc.perform(post("/api/visites/" + id + "/signature-patient")
                .contentType("application/json").content("{\"signataire\": \"" + signataire + "\"}"));
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
