package com.proxymed.suivi.service;

import com.proxymed.suivi.entity.ConstanteVisite;
import com.proxymed.suivi.entity.Medecin;
import com.proxymed.suivi.entity.Patient;
import com.proxymed.suivi.entity.VisiteSuivi;
import com.proxymed.suivi.enums.CreneauMesure;
import com.proxymed.suivi.enums.RoleMedecin;
import com.proxymed.suivi.enums.Sexe;
import com.proxymed.suivi.enums.SigneAlerteClinique;
import com.proxymed.suivi.enums.StatutVisite;
import com.proxymed.suivi.enums.TypeConstante;
import com.proxymed.suivi.enums.TypeVisite;
import com.proxymed.suivi.exception.ConflitEtatException;
import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.exception.ResourceNotFoundException;
import com.proxymed.suivi.repository.MedecinRepository;
import com.proxymed.suivi.repository.PatientRepository;
import com.proxymed.suivi.repository.VisiteSuiviRepository;
import com.proxymed.suivi.service.model.AlerteModel;
import com.proxymed.suivi.service.model.LigneGrilleConstantesModel;
import com.proxymed.suivi.service.model.MedecinModel;
import com.proxymed.suivi.service.model.PatientModel;
import com.proxymed.suivi.service.model.VisiteSuiviModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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

/**
 * Teste VisiteSuiviService en isolation : les collaborateurs externes (repositories,
 * autres services) sont mockes, mais le mapper DB (service.mappers.VisiteSuiviMapper) est
 * une vraie instance (simple convertisseur + getReferenceById, sans logique metier).
 */
@ExtendWith(MockitoExtension.class)
class VisiteSuiviServiceTest {

    @Mock
    private VisiteSuiviRepository visiteSuiviRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private MedecinRepository medecinRepository;
    @Mock
    private PatientService patientService;
    @Mock
    private MedecinService medecinService;

    private VisiteSuiviService visiteSuiviService;

    private Patient patient;
    private Medecin medecinSenior;
    private Medecin medecinJunior;

    @BeforeEach
    void setUp() {
        com.proxymed.suivi.service.mappers.VisiteSuiviMapper visiteDbMapper =
                new com.proxymed.suivi.service.mappers.VisiteSuiviMapper(
                        patientRepository, medecinRepository,
                        new com.proxymed.suivi.service.mappers.PatientMapper(),
                        new com.proxymed.suivi.service.mappers.MedecinMapper(),
                        new com.proxymed.suivi.service.mappers.ConstanteVisiteMapper(visiteSuiviRepository));

        visiteSuiviService = new VisiteSuiviServiceImpl(
                visiteSuiviRepository, patientService, medecinService, visiteDbMapper);

        patient = Patient.builder().id(UUID.randomUUID()).numeroDossierProxymed("PXM-2026-0002")
                .nomComplet("Abdoulaye Fall").dateNaissance(LocalDate.now().minusYears(57)).sexe(Sexe.M).build();
        medecinSenior = Medecin.builder().id(1L).nom("Diop").prenom("Aminata").numeroOrdre("S-1").role(RoleMedecin.SENIOR).build();
        medecinJunior = Medecin.builder().id(2L).nom("Ndiaye").prenom("Ousmane").numeroOrdre("J-1").role(RoleMedecin.JUNIOR).build();

        lenient().when(visiteSuiviRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(patientService.completerAge(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(patientRepository.getReferenceById(patient.getId())).thenReturn(patient);
        lenient().when(medecinRepository.getReferenceById(1L)).thenReturn(medecinSenior);
        lenient().when(medecinRepository.getReferenceById(2L)).thenReturn(medecinJunior);
        lenient().when(medecinService.findById(1L)).thenReturn(
                MedecinModel.builder().id(1L).role(RoleMedecin.SENIOR).build());
        lenient().when(medecinService.findById(2L)).thenReturn(
                MedecinModel.builder().id(2L).role(RoleMedecin.JUNIOR).build());
    }

    @Test
    void findById_renseigneLAgeDuPatientViaPatientService() {
        VisiteSuivi visite = visiteBrouillon();
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));
        when(patientService.completerAge(any())).thenAnswer(inv ->
                ((PatientModel) inv.getArgument(0)).toBuilder().age(57).build());

        VisiteSuiviModel resultat = visiteSuiviService.findById(visite.getId());

        assertThat(resultat.patient().id()).isEqualTo(patient.getId());
        assertThat(resultat.patient().age()).isEqualTo(57);
        assertThat(resultat.patient().sexe()).isEqualTo(Sexe.M);
    }

    @Test
    void findById_rejette_siInexistante() {
        UUID id = UUID.randomUUID();
        when(visiteSuiviRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> visiteSuiviService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByPatient_rejette_siPatientIntrouvable() {
        UUID inconnu = UUID.randomUUID();
        when(patientService.findById(inconnu)).thenThrow(new ResourceNotFoundException("Patient introuvable : " + inconnu));

        assertThatThrownBy(() -> visiteSuiviService.findByPatient(inconnu))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByPatient_retourneLesVisitesDansLOrdreDuRepository() {
        VisiteSuivi visite2 = visiteBrouillon();
        visite2.setNumeroVisite(2);
        VisiteSuivi visite1 = visiteBrouillon();
        when(visiteSuiviRepository.findByPatientIdOrderByNumeroVisiteDesc(patient.getId()))
                .thenReturn(List.of(visite2, visite1));

        var resultat = visiteSuiviService.findByPatient(patient.getId());

        assertThat(resultat).extracting(VisiteSuiviModel::numeroVisite).containsExactly(2, 1);
    }

    @Test
    void creerBrouillon_calculeLeNumeroDeVisite_etAppliqueLesValeursParDefaut() {
        when(visiteSuiviRepository.countByPatientId(patient.getId())).thenReturn(2L);

        VisiteSuiviModel resultat = visiteSuiviService.creerBrouillon(intentionCreation(2L, 1L).build());

        assertThat(resultat.numeroVisite()).isEqualTo(3);
        assertThat(resultat.statut()).isEqualTo(StatutVisite.BROUILLON);
        assertThat(resultat.dateVisite()).isEqualTo(LocalDate.now());
        assertThat(resultat.heureArrivee()).isNotNull();
        assertThat(resultat.dateCreation()).isNotNull();
        assertThat(resultat.dateMiseAJour()).isEqualTo(resultat.dateCreation());
        assertThat(resultat.medecinJunior().id()).isEqualTo(2L);
        assertThat(resultat.medecinSenior().id()).isEqualTo(1L);
    }

    @Test
    void creerBrouillon_premiereVisite_porteLeNumero1_etConserveLesChampsFournis() {
        when(visiteSuiviRepository.countByPatientId(patient.getId())).thenReturn(0L);

        VisiteSuiviModel resultat = visiteSuiviService.creerBrouillon(intentionCreation(2L, 1L)
                .dateVisite(LocalDate.of(2026, 10, 5))
                .heureArrivee(LocalTime.of(9, 30))
                .typeVisite(TypeVisite.URGENTE)
                .contactSenior("+221 77 640 12 30")
                .contactSeniorJoignable(true)
                .build());

        assertThat(resultat.numeroVisite()).isEqualTo(1);
        assertThat(resultat.dateVisite()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(resultat.heureArrivee()).isEqualTo(LocalTime.of(9, 30));
        assertThat(resultat.typeVisite()).isEqualTo(TypeVisite.URGENTE);
        assertThat(resultat.contactSeniorJoignable()).isTrue();
    }

    @Test
    void creerBrouillon_rejette_siPatientIntrouvable() {
        when(patientService.findById(patient.getId()))
                .thenThrow(new ResourceNotFoundException("Patient introuvable : " + patient.getId()));

        assertThatThrownBy(() -> visiteSuiviService.creerBrouillon(intentionCreation(2L, 1L).build()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(visiteSuiviRepository, never()).save(any());
    }

    @Test
    void creerBrouillon_rejette_siMedecinJuniorNAPasLeRoleJunior() {
        assertThatThrownBy(() -> visiteSuiviService.creerBrouillon(intentionCreation(1L, 1L).build()))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("JUNIOR");
        verify(visiteSuiviRepository, never()).save(any());
    }

    @Test
    void creerBrouillon_rejette_siMedecinSeniorNAPasLeRoleSenior() {
        assertThatThrownBy(() -> visiteSuiviService.creerBrouillon(intentionCreation(2L, 2L).build()))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("SENIOR");
        verify(visiteSuiviRepository, never()).save(any());
    }

    @Test
    void mettreAJour_neModifieQueLesChampsFournis() {
        VisiteSuivi visite = visiteBrouillon();
        visite.setAdresseVisite("Chez sa fille, Ouakam");
        Instant creation = visite.getDateCreation();
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        VisiteSuiviModel resultat = visiteSuiviService.mettreAJour(visite.getId(), VisiteSuiviModel.builder()
                .heureDepart(LocalTime.of(11, 15))
                .contactSeniorJoignable(false)
                .build());

        assertThat(resultat.heureDepart()).isEqualTo(LocalTime.of(11, 15));
        assertThat(resultat.contactSeniorJoignable()).isFalse();
        assertThat(resultat.adresseVisite()).isEqualTo("Chez sa fille, Ouakam");
        assertThat(resultat.heureArrivee()).isEqualTo(LocalTime.of(9, 0));
        assertThat(resultat.numeroVisite()).isEqualTo(1);
        assertThat(resultat.dateCreation()).isEqualTo(creation);
        assertThat(resultat.dateMiseAJour()).isAfter(creation);
    }

    @Test
    void mettreAJour_changeLeMedecinJunior_siRoleCorrect() {
        Medecin autreJunior = Medecin.builder().id(3L).nom("Sarr").prenom("Khady").numeroOrdre("J-2").role(RoleMedecin.JUNIOR).build();
        when(medecinService.findById(3L)).thenReturn(MedecinModel.builder().id(3L).role(RoleMedecin.JUNIOR).build());
        when(medecinRepository.getReferenceById(3L)).thenReturn(autreJunior);
        VisiteSuivi visite = visiteBrouillon();
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        VisiteSuiviModel resultat = visiteSuiviService.mettreAJour(visite.getId(), VisiteSuiviModel.builder()
                .medecinJunior(MedecinModel.builder().id(3L).build())
                .build());

        assertThat(resultat.medecinJunior().id()).isEqualTo(3L);
        assertThat(resultat.medecinSenior().id()).isEqualTo(1L);
    }

    @Test
    void mettreAJour_rejette_siNouveauMedecinSeniorNAPasLeRoleSenior() {
        VisiteSuivi visite = visiteBrouillon();
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        assertThatThrownBy(() -> visiteSuiviService.mettreAJour(visite.getId(), VisiteSuiviModel.builder()
                .medecinSenior(MedecinModel.builder().id(2L).build())
                .build()))
                .isInstanceOf(RegleGestionException.class);
        verify(visiteSuiviRepository, never()).save(any());
    }

    @Test
    void mettreAJour_rejette_siLaFicheNEstPlusEnBrouillon() {
        for (StatutVisite statut : List.of(StatutVisite.TERMINEE, StatutVisite.VISEE)) {
            VisiteSuivi visite = visiteBrouillon();
            visite.setStatut(statut);
            when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

            assertThatThrownBy(() -> visiteSuiviService.mettreAJour(visite.getId(),
                    VisiteSuiviModel.builder().heureDepart(LocalTime.of(12, 0)).build()))
                    .isInstanceOf(ConflitEtatException.class);
        }
        verify(visiteSuiviRepository, never()).save(any());
    }

    @Test
    void findById_sansConstante_renvoieLaGrilleVideEtAucuneAlerte() {
        VisiteSuivi visite = visiteBrouillon();
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        VisiteSuiviModel resultat = visiteSuiviService.findById(visite.getId());

        assertThat(resultat.alerteDeclenchee()).isFalse();
        assertThat(resultat.alertes()).isEmpty();
        assertThat(resultat.grilleConstantes()).extracting(LigneGrilleConstantesModel::type)
                .containsExactly(TypeConstante.values());
        assertThat(resultat.grilleConstantes()).allSatisfy(ligne -> assertThat(ligne.mesures()).isEmpty());
    }

    @Test
    void findById_recalculeLesAlertesDesConstantes_etTrieLaGrilleParCreneau() {
        VisiteSuivi visite = visiteBrouillon();
        visite.getConstantes().add(constante(visite, TypeConstante.FC, CreneauMesure.H_PLUS_1, "130", null));
        visite.getConstantes().add(constante(visite, TypeConstante.FC, CreneauMesure.H0, "80", null));
        visite.getConstantes().add(constante(visite, TypeConstante.TA, CreneauMesure.H0, "190", "100"));
        visite.getConstantes().add(constante(visite, TypeConstante.SPO2, CreneauMesure.H0, "97", null));
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        VisiteSuiviModel resultat = visiteSuiviService.findById(visite.getId());

        assertThat(resultat.alerteDeclenchee()).isTrue();
        assertThat(resultat.alertes()).extracting(AlerteModel::constante, AlerteModel::creneau, AlerteModel::seuilFranchi)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(TypeConstante.TA, CreneauMesure.H0, "TA > 180/110 mmHg"),
                        org.assertj.core.groups.Tuple.tuple(TypeConstante.FC, CreneauMesure.H_PLUS_1, "FC > 120 batt/min"));
        assertThat(resultat.alertes().get(0).valeurDiastolique()).isEqualByComparingTo("100");

        LigneGrilleConstantesModel ligneFc = resultat.grilleConstantes().get(1);
        assertThat(ligneFc.type()).isEqualTo(TypeConstante.FC);
        assertThat(ligneFc.mesures()).extracting(m -> m.creneau()).containsExactly(CreneauMesure.H0, CreneauMesure.H_PLUS_1);
        assertThat(ligneFc.mesures()).extracting(m -> m.alerte()).containsExactly(false, true);
    }

    @Test
    void findById_unSigneAlerteCliniqueCoche_declencheUneAlerte() {
        VisiteSuivi visite = visiteBrouillon();
        visite.getSignesAlerteCliniques().add(SigneAlerteClinique.DOULEUR_THORACIQUE);
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        VisiteSuiviModel resultat = visiteSuiviService.findById(visite.getId());

        assertThat(resultat.alerteDeclenchee()).isTrue();
        assertThat(resultat.alertes()).hasSize(1);
        assertThat(resultat.alertes().get(0).signeClinique()).isEqualTo(SigneAlerteClinique.DOULEUR_THORACIQUE);
        assertThat(resultat.alertes().get(0).constante()).isNull();
        assertThat(resultat.alertes().get(0).seuilFranchi()).isEqualTo("Douleur thoracique");
    }

    @Test
    void mettreAJour_remplaceLesSignesAlerteCliniques_etEnregistreLAppelDuSenior() {
        VisiteSuivi visite = visiteBrouillon();
        visite.getSignesAlerteCliniques().add(SigneAlerteClinique.DOULEUR_THORACIQUE);
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        VisiteSuiviModel resultat = visiteSuiviService.mettreAJour(visite.getId(), VisiteSuiviModel.builder()
                .signesAlerteCliniques(List.of(SigneAlerteClinique.DETRESSE_RESPIRATOIRE))
                .heureAppelSenior(LocalTime.of(9, 40))
                .heureReponseSenior(LocalTime.of(9, 43))
                .decisionAppelSenior("Oxygene et transfert EPS")
                .build());

        assertThat(resultat.signesAlerteCliniques()).containsExactly(SigneAlerteClinique.DETRESSE_RESPIRATOIRE);
        assertThat(resultat.alerteDeclenchee()).isTrue();
        assertThat(resultat.heureAppelSenior()).isEqualTo(LocalTime.of(9, 40));
        assertThat(resultat.heureReponseSenior()).isEqualTo(LocalTime.of(9, 43));
        assertThat(resultat.decisionAppelSenior()).isEqualTo("Oxygene et transfert EPS");
    }

    @Test
    void mettreAJour_sansSignesDansLaRequete_conserveLesSignesCoches_etUneListeVideLesDecoche() {
        VisiteSuivi visite = visiteBrouillon();
        visite.getSignesAlerteCliniques().add(SigneAlerteClinique.DOULEUR_THORACIQUE);
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        VisiteSuiviModel inchange = visiteSuiviService.mettreAJour(visite.getId(),
                VisiteSuiviModel.builder().heureDepart(LocalTime.of(11, 0)).build());
        assertThat(inchange.signesAlerteCliniques()).containsExactly(SigneAlerteClinique.DOULEUR_THORACIQUE);

        VisiteSuiviModel decoche = visiteSuiviService.mettreAJour(visite.getId(),
                VisiteSuiviModel.builder().signesAlerteCliniques(List.of()).build());
        assertThat(decoche.signesAlerteCliniques()).isEmpty();
        assertThat(decoche.alerteDeclenchee()).isFalse();
    }

    @Test
    void verifierModifiable_accepteUnBrouillon_etRejetteUneFicheTerminee() {
        VisiteSuivi brouillon = visiteBrouillon();
        when(visiteSuiviRepository.findById(brouillon.getId())).thenReturn(Optional.of(brouillon));
        visiteSuiviService.verifierModifiable(brouillon.getId());

        VisiteSuivi terminee = visiteBrouillon();
        terminee.setStatut(StatutVisite.TERMINEE);
        when(visiteSuiviRepository.findById(terminee.getId())).thenReturn(Optional.of(terminee));
        assertThatThrownBy(() -> visiteSuiviService.verifierModifiable(terminee.getId()))
                .isInstanceOf(ConflitEtatException.class);
    }

    private ConstanteVisite constante(VisiteSuivi visite, TypeConstante type, CreneauMesure creneau,
                                      String valeur, String diastolique) {
        return ConstanteVisite.builder()
                .visite(visite)
                .type(type)
                .creneau(creneau)
                .valeur(new BigDecimal(valeur))
                .valeurDiastolique(diastolique != null ? new BigDecimal(diastolique) : null)
                .build();
    }

    private VisiteSuiviModel.VisiteSuiviModelBuilder intentionCreation(Long medecinJuniorId, Long medecinSeniorId) {
        return VisiteSuiviModel.builder()
                .patient(PatientModel.builder().id(patient.getId()).build())
                .medecinJunior(MedecinModel.builder().id(medecinJuniorId).build())
                .medecinSenior(MedecinModel.builder().id(medecinSeniorId).build());
    }

    private VisiteSuivi visiteBrouillon() {
        Instant creation = Instant.now().minusSeconds(3600);
        return VisiteSuivi.builder()
                .id(UUID.randomUUID())
                .patient(patient)
                .numeroVisite(1)
                .dateVisite(LocalDate.of(2026, 10, 5))
                .heureArrivee(LocalTime.of(9, 0))
                .medecinJunior(medecinJunior)
                .medecinSenior(medecinSenior)
                .statut(StatutVisite.BROUILLON)
                .dateCreation(creation)
                .dateMiseAJour(creation)
                .build();
    }
}
