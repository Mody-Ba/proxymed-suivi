package com.proxymed.suivi.integration;

import com.proxymed.suivi.entity.Medecin;
import com.proxymed.suivi.enums.RoleMedecin;
import com.proxymed.suivi.repository.MedecinRepository;
import com.proxymed.suivi.support.BadgeMedecinParDefaut;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(BadgeMedecinParDefaut.class)
@Transactional
class MedecinIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MedecinRepository medecinRepository;

    @BeforeEach
    void setUp() {
        medecinRepository.save(Medecin.builder()
                .nom("Diop").prenom("Aminata").numeroOrdre("SEN-" + System.nanoTime()).role(RoleMedecin.SENIOR).build());
        medecinRepository.save(Medecin.builder()
                .nom("Ndiaye").prenom("Ousmane").numeroOrdre("JUN-" + System.nanoTime()).role(RoleMedecin.JUNIOR).build());
    }

    @Test
    void findAll_retourneTousLesMedecins() throws Exception {
        mockMvc.perform(get("/api/medecins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void findAll_filtreParRole() throws Exception {
        mockMvc.perform(get("/api/medecins").param("role", "JUNIOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nom", is("Ndiaye")))
                .andExpect(jsonPath("$[0].role", is("JUNIOR")));
    }
}
