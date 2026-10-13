package com.proxymed.suivi.service;

import com.proxymed.suivi.entity.ConstanteVisite;
import com.proxymed.suivi.entity.Medecin;
import com.proxymed.suivi.entity.Patient;
import com.proxymed.suivi.entity.TraitementVisite;
import com.proxymed.suivi.entity.VisiteSuivi;
import com.proxymed.suivi.enums.ComprehensionPatient;
import com.proxymed.suivi.enums.CreneauMesure;
import com.proxymed.suivi.enums.DecisionVisite;
import com.proxymed.suivi.enums.EtatGeneral;
import com.proxymed.suivi.enums.EvenementIntercurrent;
import com.proxymed.suivi.enums.MoyenTransmission;
import com.proxymed.suivi.enums.ObservanceTraitement;
import com.proxymed.suivi.enums.RoleMedecin;
import com.proxymed.suivi.enums.Sexe;
import com.proxymed.suivi.enums.SignatairePatient;
import com.proxymed.suivi.enums.SigneAlerteClinique;
import com.proxymed.suivi.enums.SituationSociale;
import com.proxymed.suivi.enums.StatutPlanPrevention;
import com.proxymed.suivi.enums.StatutVisite;
import com.proxymed.suivi.enums.ThemeEducation;
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
import com.proxymed.suivi.service.model.TraitementVisiteModel;
import com.proxymed.suivi.service.model.VisiteSuiviModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @Mock
    private PdfGenerationService pdfGenerationService;

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
                        new com.proxymed.suivi.service.mappers.ConstanteVisiteMapper(visiteSuiviRepository),
                        new com.proxymed.suivi.service.mappers.TraitementVisiteMapper(visiteSuiviRepository));

        visiteSuiviService = new VisiteSuiviServiceImpl(
                visiteSuiviRepository, patientService, medecinService, visiteDbMapper, pdfGenerationService);

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
    void mettreAJour_enregistreLesSections2A8_sansToucherAuxChampsNonFournis() {
        VisiteSuivi visite = visiteBrouillon();
        visite.setPlaintePrincipale("Cephalees depuis 3 jours");
        visite.setSituationSociale(SituationSociale.STABLE);
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        VisiteSuiviModel resultat = visiteSuiviService.mettreAJour(visite.getId(), VisiteSuiviModel.builder()
                .etatGeneral(EtatGeneral.ALTERE)
                .etatGeneralPrecision("Asthenie marquee")
                .examenCardiovasculaire("Pouls regulier, pas d'OMI")
                .observanceTraitement(ObservanceTraitement.PARTIELLE)
                .raisonsNonObservance("Rupture de stock")
                .comprehensionPatient(ComprehensionPatient.A_REEXPLIQUER)
                .statutPlanPrevention(StatutPlanPrevention.A_REVISER_PAR_SENIOR)
                .signalementDass(true)
                .heureContactDass(LocalTime.of(10, 30))
                .decision(DecisionVisite.SUIVI_RENFORCE)
                .resumeClinique("HTA mal equilibree")
                .dateEnvoiCompteRendu(LocalDateTime.of(2026, 10, 5, 14, 0))
                .moyenTransmission(MoyenTransmission.WHATSAPP_PRO)
                .prochaineVisiteDate(LocalDate.of(2026, 10, 19))
                .build());

        assertThat(resultat.etatGeneral()).isEqualTo(EtatGeneral.ALTERE);
        assertThat(resultat.etatGeneralPrecision()).isEqualTo("Asthenie marquee");
        assertThat(resultat.examenCardiovasculaire()).isEqualTo("Pouls regulier, pas d'OMI");
        assertThat(resultat.observanceTraitement()).isEqualTo(ObservanceTraitement.PARTIELLE);
        assertThat(resultat.raisonsNonObservance()).isEqualTo("Rupture de stock");
        assertThat(resultat.comprehensionPatient()).isEqualTo(ComprehensionPatient.A_REEXPLIQUER);
        assertThat(resultat.statutPlanPrevention()).isEqualTo(StatutPlanPrevention.A_REVISER_PAR_SENIOR);
        assertThat(resultat.signalementDass()).isTrue();
        assertThat(resultat.heureContactDass()).isEqualTo(LocalTime.of(10, 30));
        assertThat(resultat.decision()).isEqualTo(DecisionVisite.SUIVI_RENFORCE);
        assertThat(resultat.resumeClinique()).isEqualTo("HTA mal equilibree");
        assertThat(resultat.dateEnvoiCompteRendu()).isEqualTo(LocalDateTime.of(2026, 10, 5, 14, 0));
        assertThat(resultat.moyenTransmission()).isEqualTo(MoyenTransmission.WHATSAPP_PRO);
        assertThat(resultat.prochaineVisiteDate()).isEqualTo(LocalDate.of(2026, 10, 19));
        // Champs non fournis : inchanges
        assertThat(resultat.plaintePrincipale()).isEqualTo("Cephalees depuis 3 jours");
        assertThat(resultat.situationSociale()).isEqualTo(SituationSociale.STABLE);
        assertThat(resultat.examenRespiratoire()).isNull();
    }

    @Test
    void mettreAJour_remplaceLesEvenementsEtLesThemes_etLesConserveSIlsSontAbsents() {
        VisiteSuivi visite = visiteBrouillon();
        visite.getEvenementsIntercurrents().add(EvenementIntercurrent.AUCUN);
        visite.getThemesAbordes().add(ThemeEducation.HYGIENE_PREVENTION);
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        VisiteSuiviModel resultat = visiteSuiviService.mettreAJour(visite.getId(), VisiteSuiviModel.builder()
                .evenementsIntercurrents(List.of(EvenementIntercurrent.NON_OBSERVANCE, EvenementIntercurrent.CHUTE_ACCIDENT))
                .evenementsPrecision("Chute dans la cour")
                .build());

        // Listes renvoyees dans l'ordre de la fiche, quel que soit l'ordre d'envoi.
        assertThat(resultat.evenementsIntercurrents())
                .containsExactly(EvenementIntercurrent.CHUTE_ACCIDENT, EvenementIntercurrent.NON_OBSERVANCE);
        assertThat(resultat.evenementsPrecision()).isEqualTo("Chute dans la cour");
        assertThat(resultat.themesAbordes()).containsExactly(ThemeEducation.HYGIENE_PREVENTION);

        VisiteSuiviModel decoche = visiteSuiviService.mettreAJour(visite.getId(), VisiteSuiviModel.builder()
                .themesAbordes(List.of())
                .build());
        assertThat(decoche.themesAbordes()).isEmpty();
        assertThat(decoche.evenementsIntercurrents()).hasSize(2);
    }

    @Test
    void mettreAJour_rejette_siAucunEstCocheAvecUnAutreEvenement() {
        VisiteSuivi visite = visiteBrouillon();
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        assertThatThrownBy(() -> visiteSuiviService.mettreAJour(visite.getId(), VisiteSuiviModel.builder()
                .evenementsIntercurrents(List.of(EvenementIntercurrent.AUCUN, EvenementIntercurrent.HOSPITALISATION))
                .build()))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("AUCUN");
        assertThat(visite.getEvenementsIntercurrents()).isEmpty();
        verify(visiteSuiviRepository, never()).save(any());

        VisiteSuiviModel seul = visiteSuiviService.mettreAJour(visite.getId(), VisiteSuiviModel.builder()
                .evenementsIntercurrents(List.of(EvenementIntercurrent.AUCUN))
                .build());
        assertThat(seul.evenementsIntercurrents()).containsExactly(EvenementIntercurrent.AUCUN);
    }

    @Test
    void etatGeneralUrgenceOuUrgenceSociale_neDeclenchePasDAlerte() {
        VisiteSuivi visite = visiteBrouillon();
        visite.setEtatGeneral(EtatGeneral.URGENCE_APPEL_SENIOR);
        visite.setSituationSociale(SituationSociale.URGENCE_SOCIALE);
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        assertThat(visiteSuiviService.findById(visite.getId()).alerteDeclenchee()).isFalse();
    }

    @Test
    void findById_renvoieLesTraitementsDansLOrdreDeSaisie() {
        VisiteSuivi visite = visiteBrouillon();
        visite.getTraitements().add(TraitementVisite.builder().id(12L).visite(visite).medicament("Metformine").build());
        visite.getTraitements().add(TraitementVisite.builder().id(5L).visite(visite).medicament("Amlodipine").build());
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        VisiteSuiviModel resultat = visiteSuiviService.findById(visite.getId());

        assertThat(resultat.traitements()).extracting(TraitementVisiteModel::medicament)
                .containsExactly("Amlodipine", "Metformine");
    }

    @Test
    void terminer_passeLaFicheCompleteEnTerminee_etEnregistreLaSignatureDuJunior() {
        VisiteSuivi visite = visiteComplete();
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        VisiteSuiviModel resultat = visiteSuiviService.terminer(visite.getId());

        assertThat(resultat.statut()).isEqualTo(StatutVisite.TERMINEE);
        assertThat(resultat.dateSignatureJunior()).isNotNull();
        assertThat(resultat.dateVisaSenior()).isNull();
        assertThat(resultat.echeanceVisa()).isEqualTo(resultat.dateSignatureJunior().plus(VisiteSuiviServiceImpl.DELAI_VISA));
        assertThat(resultat.visaEnRetard()).isFalse();
        assertThat(visite.getStatut()).isEqualTo(StatutVisite.TERMINEE);
    }

    @Test
    void terminer_rejette_etListeTousLesManquements() {
        VisiteSuivi visite = visiteBrouillon();
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        assertThatThrownBy(() -> visiteSuiviService.terminer(visite.getId()))
                .isInstanceOfSatisfying(RegleGestionException.class, ex -> {
                    assertThat(ex.getMessage()).contains("9 element(s)");
                    assertThat(ex.getDetails()).hasSize(9)
                            .anyMatch(d -> d.startsWith("heureDepart"))
                            .anyMatch(d -> d.startsWith("constantes"))
                            .anyMatch(d -> d.startsWith("resumeClinique"));
                });
        assertThat(visite.getStatut()).isEqualTo(StatutVisite.BROUILLON);
        verify(visiteSuiviRepository, never()).save(any());
    }

    @Test
    void terminer_avecAlerte_exigeLAppelDuSenior() {
        VisiteSuivi visite = visiteComplete();
        visite.getSignesAlerteCliniques().add(SigneAlerteClinique.DOULEUR_THORACIQUE);
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        assertThatThrownBy(() -> visiteSuiviService.terminer(visite.getId()))
                .isInstanceOfSatisfying(RegleGestionException.class, ex -> assertThat(ex.getDetails())
                        .extracting(d -> d.substring(0, d.indexOf(" :")))
                        .containsExactly("heureAppelSenior", "decisionAppelSenior"));

        visite.setHeureAppelSenior(LocalTime.of(9, 40));
        visite.setDecisionAppelSenior("Transfert EPS");
        assertThat(visiteSuiviService.terminer(visite.getId()).statut()).isEqualTo(StatutVisite.TERMINEE);
    }

    @Test
    void terminer_rejette_siLaFicheNEstPlusEnBrouillon() {
        for (StatutVisite statut : List.of(StatutVisite.TERMINEE, StatutVisite.VISEE)) {
            VisiteSuivi visite = visiteComplete();
            visite.setStatut(statut);
            when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

            assertThatThrownBy(() -> visiteSuiviService.terminer(visite.getId()))
                    .isInstanceOf(ConflitEtatException.class);
        }
        verify(visiteSuiviRepository, never()).save(any());
    }

    @Test
    void viser_passeUneFicheTermineeEnVisee_etEnregistreLaDateDuVisa() {
        VisiteSuivi visite = visiteComplete();
        visite.setStatut(StatutVisite.TERMINEE);
        visite.setDateSignatureJunior(Instant.now().minusSeconds(3600));
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        VisiteSuiviModel resultat = visiteSuiviService.viser(visite.getId());

        assertThat(resultat.statut()).isEqualTo(StatutVisite.VISEE);
        assertThat(resultat.dateVisaSenior()).isNotNull();
        assertThat(resultat.visaEnRetard()).isFalse();
    }

    @Test
    void viser_rejette_siLaFicheNEstPasTerminee() {
        for (StatutVisite statut : List.of(StatutVisite.BROUILLON, StatutVisite.VISEE)) {
            VisiteSuivi visite = visiteComplete();
            visite.setStatut(statut);
            when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

            assertThatThrownBy(() -> visiteSuiviService.viser(visite.getId()))
                    .isInstanceOf(ConflitEtatException.class);
        }
        verify(visiteSuiviRepository, never()).save(any());
    }

    @Test
    void signerPatient_enregistreLaDateEtLeSignataire_surUneFicheTermineeOuVisee() {
        for (StatutVisite statut : List.of(StatutVisite.TERMINEE, StatutVisite.VISEE)) {
            VisiteSuivi visite = visiteComplete();
            visite.setStatut(statut);
            when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

            VisiteSuiviModel resultat = visiteSuiviService.signerPatient(visite.getId(), SignatairePatient.AIDANT);

            assertThat(resultat.signatairePatient()).isEqualTo(SignatairePatient.AIDANT);
            assertThat(resultat.dateSignaturePatient()).isNotNull();
            assertThat(resultat.statut()).isEqualTo(statut);
        }
    }

    @Test
    void signerPatient_rejette_surUnBrouillon_ouSiDejaSignee() {
        VisiteSuivi brouillon = visiteComplete();
        when(visiteSuiviRepository.findById(brouillon.getId())).thenReturn(Optional.of(brouillon));
        assertThatThrownBy(() -> visiteSuiviService.signerPatient(brouillon.getId(), SignatairePatient.PATIENT))
                .isInstanceOf(ConflitEtatException.class);

        VisiteSuivi dejaSignee = visiteComplete();
        dejaSignee.setStatut(StatutVisite.TERMINEE);
        dejaSignee.setDateSignaturePatient(Instant.now());
        dejaSignee.setSignatairePatient(SignatairePatient.PATIENT);
        when(visiteSuiviRepository.findById(dejaSignee.getId())).thenReturn(Optional.of(dejaSignee));
        assertThatThrownBy(() -> visiteSuiviService.signerPatient(dejaSignee.getId(), SignatairePatient.AIDANT))
                .isInstanceOf(ConflitEtatException.class)
                .hasMessageContaining("deja");
        assertThat(dejaSignee.getSignatairePatient()).isEqualTo(SignatairePatient.PATIENT);
        verify(visiteSuiviRepository, never()).save(any());
    }

    @Test
    void findEnAttenteVisa_listeLesFichesTerminees_etSignaleLeRetardDeVisa() {
        VisiteSuivi enRetard = visiteComplete();
        enRetard.setStatut(StatutVisite.TERMINEE);
        enRetard.setDateSignatureJunior(Instant.now().minus(Duration.ofHours(30)));
        VisiteSuivi recente = visiteComplete();
        recente.setStatut(StatutVisite.TERMINEE);
        recente.setDateSignatureJunior(Instant.now().minus(Duration.ofHours(2)));
        when(visiteSuiviRepository.findByStatutOrderByDateSignatureJuniorAsc(StatutVisite.TERMINEE))
                .thenReturn(List.of(enRetard, recente));

        List<VisiteSuiviModel> resultat = visiteSuiviService.findEnAttenteVisa();

        assertThat(resultat).extracting(VisiteSuiviModel::id).containsExactly(enRetard.getId(), recente.getId());
        assertThat(resultat).extracting(VisiteSuiviModel::visaEnRetard).containsExactly(true, false);
    }

    @Test
    void delais_visaAppose26hApresLaSignature_estEnRetard_etSansSignatureLeDelaiEstInconnu() {
        VisiteSuivi visee = visiteComplete();
        visee.setStatut(StatutVisite.VISEE);
        Instant signature = Instant.now().minus(Duration.ofDays(3));
        visee.setDateSignatureJunior(signature);
        visee.setDateVisaSenior(signature.plus(Duration.ofHours(26)));
        when(visiteSuiviRepository.findById(visee.getId())).thenReturn(Optional.of(visee));
        assertThat(visiteSuiviService.findById(visee.getId()).visaEnRetard()).isTrue();

        VisiteSuivi brouillon = visiteBrouillon();
        when(visiteSuiviRepository.findById(brouillon.getId())).thenReturn(Optional.of(brouillon));
        VisiteSuiviModel sansDelais = visiteSuiviService.findById(brouillon.getId());
        assertThat(sansDelais.echeanceVisa()).isNull();
        assertThat(sansDelais.visaEnRetard()).isNull();
        assertThat(sansDelais.echeanceCompteRendu()).isNull();
        assertThat(sansDelais.compteRenduEnRetard()).isNull();
    }

    @Test
    void delais_compteRenduAttenduDansLes12hApresLaFinDeLaVisite() {
        VisiteSuivi visite = visiteComplete();
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        // Visite du 05/10/2026, depart 11 h : echeance a 23 h. Sans envoi, la date du jour sert de reference.
        VisiteSuiviModel sansEnvoi = visiteSuiviService.findById(visite.getId());
        assertThat(sansEnvoi.echeanceCompteRendu()).isEqualTo(LocalDateTime.of(2026, 10, 5, 23, 0));
        assertThat(sansEnvoi.compteRenduEnRetard()).isTrue();

        visite.setDateEnvoiCompteRendu(LocalDateTime.of(2026, 10, 5, 18, 0));
        assertThat(visiteSuiviService.findById(visite.getId()).compteRenduEnRetard()).isFalse();

        visite.setDateEnvoiCompteRendu(LocalDateTime.of(2026, 10, 6, 8, 0));
        assertThat(visiteSuiviService.findById(visite.getId()).compteRenduEnRetard()).isTrue();
    }

    @Test
    void mettreAJour_enregistreLaSaisieDmi() {
        VisiteSuivi visite = visiteBrouillon();
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));

        VisiteSuiviModel resultat = visiteSuiviService.mettreAJour(visite.getId(), VisiteSuiviModel.builder()
                .dateSaisieDmi(LocalDate.of(2026, 10, 6)).saisieDmiPar("Dr Ndiaye").build());

        assertThat(resultat.dateSaisieDmi()).isEqualTo(LocalDate.of(2026, 10, 6));
        assertThat(resultat.saisieDmiPar()).isEqualTo("Dr Ndiaye");
    }

    @Test
    void genererPdf_transmetLaFicheCompleteAuGenerateur() {
        VisiteSuivi visite = visiteComplete();
        visite.getConstantes().add(constante(visite, TypeConstante.FC, CreneauMesure.H_PLUS_1, "130", null));
        when(visiteSuiviRepository.findById(visite.getId())).thenReturn(Optional.of(visite));
        Resource pdf = new ByteArrayResource("%PDF-".getBytes());
        when(pdfGenerationService.genererFicheVisite(any())).thenReturn(pdf);

        assertThat(visiteSuiviService.genererPdf(visite.getId())).isSameAs(pdf);

        ArgumentCaptor<VisiteSuiviModel> fiche = ArgumentCaptor.forClass(VisiteSuiviModel.class);
        verify(pdfGenerationService).genererFicheVisite(fiche.capture());
        assertThat(fiche.getValue().id()).isEqualTo(visite.getId());
        assertThat(fiche.getValue().alerteDeclenchee()).isTrue();
        assertThat(fiche.getValue().constantes()).hasSize(9);
    }

    @Test
    void genererPdf_rejette_siVisiteIntrouvable() {
        UUID id = UUID.randomUUID();
        when(visiteSuiviRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> visiteSuiviService.genererPdf(id)).isInstanceOf(ResourceNotFoundException.class);
        verify(pdfGenerationService, never()).genererFicheVisite(any());
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

    /**
     * Brouillon qui satisfait tous les controles de la regle 8 (pret a etre termine).
     */
    private VisiteSuivi visiteComplete() {
        VisiteSuivi visite = visiteBrouillon();
        visite.setHeureDepart(LocalTime.of(11, 0));
        visite.setTypeVisite(TypeVisite.PROGRAMMEE);
        visite.setContactSenior("+221 77 640 12 30");
        visite.setEtatGeneral(EtatGeneral.BON);
        visite.setObservanceTraitement(ObservanceTraitement.BONNE);
        visite.setSituationSociale(SituationSociale.STABLE);
        visite.setDecision(DecisionVisite.PATIENT_STABLE_SUIVI_PROGRAMME);
        visite.setResumeClinique("Patient stable");
        visite.getConstantes().add(constante(visite, TypeConstante.TA, CreneauMesure.H0, "130", "80"));
        visite.getConstantes().add(constante(visite, TypeConstante.FC, CreneauMesure.H0, "78", null));
        visite.getConstantes().add(constante(visite, TypeConstante.FR, CreneauMesure.H0, "16", null));
        visite.getConstantes().add(constante(visite, TypeConstante.SPO2, CreneauMesure.H0, "98", null));
        visite.getConstantes().add(constante(visite, TypeConstante.TEMPERATURE, CreneauMesure.H0, "37", null));
        visite.getConstantes().add(constante(visite, TypeConstante.GLYCEMIE, CreneauMesure.H0, "1", null));
        visite.getConstantes().add(constante(visite, TypeConstante.EVA, CreneauMesure.H0, "2", null));
        visite.getConstantes().add(constante(visite, TypeConstante.GLASGOW, CreneauMesure.H0, "15", null));
        return visite;
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
