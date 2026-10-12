package com.proxymed.suivi.service;

import com.proxymed.suivi.entity.Medecin;
import com.proxymed.suivi.enums.RoleMedecin;
import com.proxymed.suivi.exception.ResourceNotFoundException;
import com.proxymed.suivi.repository.MedecinRepository;
import com.proxymed.suivi.service.mappers.MedecinMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedecinServiceTest {

    @Mock
    private MedecinRepository medecinRepository;

    private MedecinService medecinService;

    @BeforeEach
    void setUp() {
        medecinService = new MedecinServiceImpl(medecinRepository, new MedecinMapper());
    }

    @Test
    void findAll_filtreParRole_siRoleFourni() {
        Medecin senior = Medecin.builder().id(1L).nom("Diop").prenom("Aminata").numeroOrdre("SEN01").role(RoleMedecin.SENIOR).build();
        when(medecinRepository.findByRole(RoleMedecin.SENIOR)).thenReturn(List.of(senior));

        var resultat = medecinService.findAll(RoleMedecin.SENIOR);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).nom()).isEqualTo("Diop");
    }

    @Test
    void findAll_retourneTout_siRoleAbsent() {
        Medecin junior = Medecin.builder().id(2L).nom("Ndiaye").prenom("Ousmane").numeroOrdre("JUN01").role(RoleMedecin.JUNIOR).build();
        when(medecinRepository.findAll()).thenReturn(List.of(junior));

        var resultat = medecinService.findAll(null);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).nom()).isEqualTo("Ndiaye");
    }

    @Test
    void findById_retourneLeMedecin() {
        Medecin junior = Medecin.builder().id(2L).nom("Ndiaye").prenom("Ousmane").numeroOrdre("JUN01")
                .role(RoleMedecin.JUNIOR).telephone("770000000").build();
        when(medecinRepository.findById(2L)).thenReturn(Optional.of(junior));

        var resultat = medecinService.findById(2L);

        assertThat(resultat.role()).isEqualTo(RoleMedecin.JUNIOR);
        assertThat(resultat.telephone()).isEqualTo("770000000");
    }

    @Test
    void findById_rejette_siInexistant() {
        when(medecinRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medecinService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
