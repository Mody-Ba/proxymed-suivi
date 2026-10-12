package com.proxymed.suivi.service;

import com.proxymed.suivi.entity.Patient;
import com.proxymed.suivi.enums.Sexe;
import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.exception.ResourceNotFoundException;
import com.proxymed.suivi.repository.PatientRepository;
import com.proxymed.suivi.service.mappers.PatientMapper;
import com.proxymed.suivi.service.model.PatientModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    private PatientService patientService;

    @BeforeEach
    void setUp() {
        patientService = new PatientServiceImpl(patientRepository, new PatientMapper());
        lenient().when(patientRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void findAll_calculeLAgeDeChaquePatient() {
        Patient patient = Patient.builder().id(UUID.randomUUID()).numeroDossierProxymed("PXM-2026-0001")
                .nomComplet("Fatou Sow").dateNaissance(LocalDate.now().minusYears(30)).sexe(Sexe.F).build();
        when(patientRepository.findAll()).thenReturn(List.of(patient));

        var resultat = patientService.findAll();

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).age()).isEqualTo(30);
    }

    @Test
    void findById_rejette_siInexistant() {
        UUID id = UUID.randomUUID();
        when(patientRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_persisteEtRetourneLeModeleAvecAge() {
        PatientModel model = PatientModel.builder()
                .numeroDossierProxymed("PXM-2026-0009")
                .nomComplet("Abdoulaye Fall")
                .dateNaissance(LocalDate.now().minusYears(45))
                .sexe(Sexe.M)
                .build();

        var resultat = patientService.create(model);

        assertThat(resultat.nomComplet()).isEqualTo("Abdoulaye Fall");
        assertThat(resultat.age()).isEqualTo(45);
    }

    @Test
    void create_rejette_siNumeroDossierDejaUtilise() {
        when(patientRepository.existsByNumeroDossierProxymed("PXM-2026-0001")).thenReturn(true);
        PatientModel model = PatientModel.builder().numeroDossierProxymed("PXM-2026-0001")
                .nomComplet("Doublon").dateNaissance(LocalDate.now().minusYears(50)).sexe(Sexe.F).build();

        assertThatThrownBy(() -> patientService.create(model))
                .isInstanceOf(RegleGestionException.class);
        verify(patientRepository, never()).save(any());
    }

    @Test
    void completerAge_calculeLAgeDUnModeleProduitHorsDuService() {
        PatientModel model = PatientModel.builder().id(UUID.randomUUID())
                .dateNaissance(LocalDate.now().minusYears(72)).build();

        PatientModel resultat = patientService.completerAge(model);

        assertThat(resultat.age()).isEqualTo(72);
        assertThat(resultat.id()).isEqualTo(model.id());
    }

    @Test
    void completerAge_laisseLAgeNull_siDateNaissanceAbsente() {
        assertThat(patientService.completerAge(PatientModel.builder().build()).age()).isNull();
        assertThat(patientService.completerAge(null)).isNull();
    }
}
