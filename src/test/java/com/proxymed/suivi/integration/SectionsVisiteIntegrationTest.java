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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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

/**
 * Sections 2, 4, 5, 6, 7 et 8 de la fiche : champs du PUT de la visite, listes a choix
 * multiples et tableau des traitements.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(BadgeMedecinParDefaut.class)
@Transactional
class SectionsVisiteIntegrationTest {

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
    private EntityManager entityManager;

    private String visiteId;

    @BeforeEach
    void setUp() throws Exception {
        visiteId = creerVisite();
    }

    @Test
    void nouvelleVisite_aSesSectionsVides() throws Exception {
        mockMvc.perform(get("/api/visites/" + visiteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etatGeneral", nullValue()))
                .andExpect(jsonPath("$.evenementsIntercurrents", hasSize(0)))
                .andExpect(jsonPath("$.themesAbordes", hasSize(0)))
                .andExpect(jsonPath("$.traitements", hasSize(0)))
                .andExpect(jsonPath("$.decision", nullValue()));
    }

    @Test
    void put_enregistreToutesLesSections_etLaRelectureEnBaseLesRetrouve() throws Exception {
        mockMvc.perform(put("/api/visites/" + visiteId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "etatGeneral": "ALTERE",
                                  "etatGeneralPrecision": "Asthénie marquée",
                                  "plaintePrincipale": "Céphalées depuis 3 jours",
                                  "evenementsIntercurrents": ["NON_OBSERVANCE", "CHUTE_ACCIDENT"],
                                  "evenementsPrecision": "Chute dans la cour",
                                  "examenCardiovasculaire": "Pouls régulier, pas d'OMI",
                                  "examenRespiratoire": "MV symétrique, pas de crépitants",
                                  "examenNeurologique": "Conscient, pas de déficit",
                                  "examenDigestifAutre": "Abdomen souple",
                                  "examenCutane": "Pas d'escarre",
                                  "observanceTraitement": "PARTIELLE",
                                  "raisonsNonObservance": "Rupture de stock",
                                  "themesAbordes": ["AUTRES", "GESTION_MEDICAMENTS"],
                                  "themesAutresPrecision": "Prévention des chutes",
                                  "comprehensionPatient": "A_REEXPLIQUER",
                                  "statutPlanPrevention": "A_REVISER_PAR_SENIOR",
                                  "situationSociale": "DETERIOREE",
                                  "problemesSociaux": "Aidant absent la semaine",
                                  "signalementDass": true,
                                  "heureContactDass": "10:30:00",
                                  "decision": "SUIVI_RENFORCE",
                                  "resumeClinique": "HTA mal équilibrée, observance partielle",
                                  "dateEnvoiCompteRendu": "2026-10-05T14:00:00",
                                  "moyenTransmission": "WHATSAPP_PRO",
                                  "prochaineVisiteDate": "2026-10-19"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etatGeneral", is("ALTERE")));

        // On vide le cache de la session pour relire reellement les colonnes et les tables de listes.
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/visites/" + visiteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etatGeneral", is("ALTERE")))
                .andExpect(jsonPath("$.etatGeneralPrecision", is("Asthénie marquée")))
                .andExpect(jsonPath("$.plaintePrincipale", is("Céphalées depuis 3 jours")))
                .andExpect(jsonPath("$.evenementsIntercurrents", contains("CHUTE_ACCIDENT", "NON_OBSERVANCE")))
                .andExpect(jsonPath("$.evenementsPrecision", is("Chute dans la cour")))
                .andExpect(jsonPath("$.examenCardiovasculaire", is("Pouls régulier, pas d'OMI")))
                .andExpect(jsonPath("$.examenRespiratoire", is("MV symétrique, pas de crépitants")))
                .andExpect(jsonPath("$.examenNeurologique", is("Conscient, pas de déficit")))
                .andExpect(jsonPath("$.examenDigestifAutre", is("Abdomen souple")))
                .andExpect(jsonPath("$.examenCutane", is("Pas d'escarre")))
                .andExpect(jsonPath("$.observanceTraitement", is("PARTIELLE")))
                .andExpect(jsonPath("$.raisonsNonObservance", is("Rupture de stock")))
                .andExpect(jsonPath("$.themesAbordes", contains("GESTION_MEDICAMENTS", "AUTRES")))
                .andExpect(jsonPath("$.themesAutresPrecision", is("Prévention des chutes")))
                .andExpect(jsonPath("$.comprehensionPatient", is("A_REEXPLIQUER")))
                .andExpect(jsonPath("$.statutPlanPrevention", is("A_REVISER_PAR_SENIOR")))
                .andExpect(jsonPath("$.situationSociale", is("DETERIOREE")))
                .andExpect(jsonPath("$.problemesSociaux", is("Aidant absent la semaine")))
                .andExpect(jsonPath("$.signalementDass", is(true)))
                .andExpect(jsonPath("$.heureContactDass", is("10:30:00")))
                .andExpect(jsonPath("$.decision", is("SUIVI_RENFORCE")))
                .andExpect(jsonPath("$.resumeClinique", is("HTA mal équilibrée, observance partielle")))
                .andExpect(jsonPath("$.dateEnvoiCompteRendu", is("2026-10-05T14:00:00")))
                .andExpect(jsonPath("$.moyenTransmission", is("WHATSAPP_PRO")))
                .andExpect(jsonPath("$.prochaineVisiteDate", is("2026-10-19")))
                .andExpect(jsonPath("$.alerteDeclenchee", is(false)));
    }

    @Test
    void put_estPartiel_etUneListeEnvoyeeRemplaceLAncienne() throws Exception {
        mettreAJour("""
                {"etatGeneral": "BON", "evenementsIntercurrents": ["AUCUN"], "themesAbordes": ["HYGIENE_PREVENTION"]}
                """).andExpect(status().isOk());

        mettreAJour("""
                {"decision": "PATIENT_STABLE_SUIVI_PROGRAMME", "evenementsIntercurrents": ["HOSPITALISATION"]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etatGeneral", is("BON")))
                .andExpect(jsonPath("$.decision", is("PATIENT_STABLE_SUIVI_PROGRAMME")))
                .andExpect(jsonPath("$.evenementsIntercurrents", contains("HOSPITALISATION")))
                .andExpect(jsonPath("$.themesAbordes", contains("HYGIENE_PREVENTION")));

        mettreAJour("""
                {"themesAbordes": []}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themesAbordes", hasSize(0)))
                .andExpect(jsonPath("$.evenementsIntercurrents", contains("HOSPITALISATION")));
    }

    @Test
    void put_rejette_aucunCocheAvecUnAutreEvenement() throws Exception {
        mettreAJour("""
                {"evenementsIntercurrents": ["AUCUN", "CHUTE_ACCIDENT"]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur", is("Regle de gestion non respectee")))
                .andExpect(jsonPath("$.message", containsString("AUCUN")));
    }

    @Test
    void put_rejette_lesValeursInconnuesOuNulles() throws Exception {
        mettreAJour("""
                {"etatGeneral": "MOYEN"}
                """).andExpect(status().isBadRequest());
        mettreAJour("""
                {"themesAbordes": ["JARDINAGE"]}
                """).andExpect(status().isBadRequest());
        mettreAJour("""
                {"evenementsIntercurrents": [null]}
                """).andExpect(status().isBadRequest());
        mettreAJour("""
                {"heureContactDass": "25:99"}
                """).andExpect(status().isBadRequest());
        // Les traitements ont leur endpoint dedie : ils ne passent pas par le PUT de la visite.
        mettreAJour("""
                {"traitements": []}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Champ inconnu dans la requete : traitements")));
    }

    @Test
    void put_retourne409_surUneFicheTerminee() throws Exception {
        terminer();
        mettreAJour("""
                {"decision": "SUIVI_RENFORCE"}
                """).andExpect(status().isConflict());
    }

    @Test
    void traitements_ajout_modificationPartielle_suppression() throws Exception {
        long amlodipine = idDe(ajouterTraitement("""
                {"medicament": "Amlodipine 5 mg", "posologie": "1 cp le matin", "voie": "Orale", "observant": true, "probleme": false}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicament", is("Amlodipine 5 mg")))
                .andExpect(jsonPath("$.observant", is(true))));
        long metformine = idDe(ajouterTraitement("""
                {"medicament": "Metformine 850 mg"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.posologie", nullValue())));

        mockMvc.perform(put("/api/visites/" + visiteId + "/traitements/" + metformine)
                        .contentType("application/json")
                        .content("""
                                {"observant": false, "probleme": true, "problemePrecision": "Troubles digestifs"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicament", is("Metformine 850 mg")))
                .andExpect(jsonPath("$.observant", is(false)))
                .andExpect(jsonPath("$.probleme", is(true)))
                .andExpect(jsonPath("$.problemePrecision", is("Troubles digestifs")));

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/visites/" + visiteId + "/traitements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].medicament", contains("Amlodipine 5 mg", "Metformine 850 mg")))
                .andExpect(jsonPath("$[1].problemePrecision", is("Troubles digestifs")));
        mockMvc.perform(get("/api/visites/" + visiteId))
                .andExpect(jsonPath("$.traitements", hasSize(2)))
                .andExpect(jsonPath("$.traitements[0].voie", is("Orale")));

        mockMvc.perform(delete("/api/visites/" + visiteId + "/traitements/" + amlodipine))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/visites/" + visiteId))
                .andExpect(jsonPath("$.traitements[*].medicament", contains("Metformine 850 mg")));
        mockMvc.perform(delete("/api/visites/" + visiteId + "/traitements/" + amlodipine))
                .andExpect(status().isNotFound());
    }

    @Test
    void traitements_medicamentObligatoire_etChampsInconnusRefuses() throws Exception {
        ajouterTraitement("""
                {"posologie": "1 cp le matin"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasSize(1)));
        ajouterTraitement("""
                {"medicament": "   "}
                """).andExpect(status().isBadRequest());
        ajouterTraitement("""
                {"medicament": "Amlodipine", "dose": "5 mg"}
                """).andExpect(status().isBadRequest());

        long id = idDe(ajouterTraitement("""
                {"medicament": "Amlodipine 5 mg"}
                """));
        mockMvc.perform(put("/api/visites/" + visiteId + "/traitements/" + id)
                        .contentType("application/json")
                        .content("""
                                {"medicament": ""}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/visites/" + visiteId + "/traitements"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].medicament", is("Amlodipine 5 mg")));
    }

    @Test
    void traitements_404_siVisiteOuTraitementIntrouvable_ouTraitementDUneAutreVisite() throws Exception {
        mockMvc.perform(get("/api/visites/" + UUID.randomUUID() + "/traitements"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/visites/" + UUID.randomUUID() + "/traitements")
                        .contentType("application/json")
                        .content("""
                                {"medicament": "Amlodipine"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/visites/" + visiteId + "/traitements/999999")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isNotFound());

        long id = idDe(ajouterTraitement("""
                {"medicament": "Amlodipine 5 mg"}
                """));
        String autreVisiteId = creerVisite();
        mockMvc.perform(put("/api/visites/" + autreVisiteId + "/traitements/" + id)
                        .contentType("application/json")
                        .content("""
                                {"posologie": "2 cp"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/visites/" + autreVisiteId + "/traitements/" + id))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/visites/" + visiteId + "/traitements"))
                .andExpect(jsonPath("$[0].posologie", nullValue()));
    }

    @Test
    void traitements_409_surUneFicheTerminee() throws Exception {
        long id = idDe(ajouterTraitement("""
                {"medicament": "Amlodipine 5 mg"}
                """));
        terminer();

        ajouterTraitement("""
                {"medicament": "Metformine"}
                """).andExpect(status().isConflict());
        mockMvc.perform(put("/api/visites/" + visiteId + "/traitements/" + id)
                        .contentType("application/json")
                        .content("""
                                {"posologie": "2 cp"}
                                """))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/visites/" + visiteId + "/traitements/" + id))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/visites/" + visiteId + "/traitements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    private ResultActions mettreAJour(String corps) throws Exception {
        return mockMvc.perform(put("/api/visites/" + visiteId).contentType("application/json").content(corps));
    }

    private ResultActions ajouterTraitement(String corps) throws Exception {
        return mockMvc.perform(post("/api/visites/" + visiteId + "/traitements")
                .contentType("application/json").content(corps));
    }

    private long idDe(ResultActions resultat) throws Exception {
        return objectMapper.readTree(resultat.andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    /**
     * Pas encore d'endpoint "terminer" (etape 4) : on force le statut via le repository.
     */
    private void terminer() {
        visiteSuiviRepository.findById(UUID.fromString(visiteId)).orElseThrow().setStatut(StatutVisite.TERMINEE);
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
