package com.proxymed.suivi.service;

import com.proxymed.suivi.entity.ConstanteVisite;
import com.proxymed.suivi.entity.VisiteSuivi;
import com.proxymed.suivi.enums.CreneauMesure;
import com.proxymed.suivi.enums.TypeConstante;
import com.proxymed.suivi.exception.ConflitEtatException;
import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.exception.ResourceNotFoundException;
import com.proxymed.suivi.repository.ConstanteVisiteRepository;
import com.proxymed.suivi.repository.VisiteSuiviRepository;
import com.proxymed.suivi.service.mappers.ConstanteVisiteMapper;
import com.proxymed.suivi.service.model.ConstanteVisiteModel;
import com.proxymed.suivi.service.model.LigneGrilleConstantesModel;
import com.proxymed.suivi.service.model.VisiteSuiviModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Teste ConstanteVisiteService en isolation : repositories et VisiteSuiviService mockes,
 * mapper DB reel (simple convertisseur + getReferenceById).
 */
@ExtendWith(MockitoExtension.class)
class ConstanteVisiteServiceTest {

    @Mock
    private ConstanteVisiteRepository constanteVisiteRepository;
    @Mock
    private VisiteSuiviRepository visiteSuiviRepository;
    @Mock
    private VisiteSuiviService visiteSuiviService;

    private ConstanteVisiteService constanteVisiteService;

    private VisiteSuivi visite;

    @BeforeEach
    void setUp() {
        constanteVisiteService = new ConstanteVisiteServiceImpl(
                constanteVisiteRepository, visiteSuiviService, new ConstanteVisiteMapper(visiteSuiviRepository));

        visite = VisiteSuivi.builder().id(UUID.randomUUID()).numeroVisite(1).build();
        lenient().when(visiteSuiviRepository.getReferenceById(visite.getId())).thenReturn(visite);
        lenient().when(constanteVisiteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void enregistrer_creeUneNouvelleMesure_etCalculeLAlerte() {
        ConstanteVisiteModel resultat = constanteVisiteService.enregistrer(visite.getId(),
                mesure(TypeConstante.FC, CreneauMesure.H0, "130", null));

        assertThat(resultat.visiteId()).isEqualTo(visite.getId());
        assertThat(resultat.valeur()).isEqualByComparingTo("130");
        assertThat(resultat.alerte()).isTrue();
        assertThat(resultat.seuilFranchi()).isEqualTo("FC > 120 batt/min");
        assertThat(visite.getConstantes()).hasSize(1);
    }

    @Test
    void enregistrer_remplaceLaMesureDuMemeTypeEtDuMemeCreneau() {
        ConstanteVisite existante = ConstanteVisite.builder().id(7L).visite(visite)
                .type(TypeConstante.TA).creneau(CreneauMesure.H0)
                .valeur(new BigDecimal("190")).valeurDiastolique(new BigDecimal("100")).build();
        when(constanteVisiteRepository.findByVisiteIdAndTypeAndCreneau(visite.getId(), TypeConstante.TA, CreneauMesure.H0))
                .thenReturn(Optional.of(existante));

        ConstanteVisiteModel resultat = constanteVisiteService.enregistrer(visite.getId(),
                mesure(TypeConstante.TA, CreneauMesure.H0, "135", "85").toBuilder().heureMesure(LocalTime.of(9, 35)).build());

        assertThat(resultat.id()).isEqualTo(7L);
        assertThat(resultat.valeur()).isEqualByComparingTo("135");
        assertThat(resultat.valeurDiastolique()).isEqualByComparingTo("85");
        assertThat(resultat.heureMesure()).isEqualTo(LocalTime.of(9, 35));
        assertThat(resultat.alerte()).isFalse();
        assertThat(visite.getConstantes()).isEmpty();
    }

    @Test
    void enregistrer_rejette_siTensionSansDiastolique() {
        assertThatThrownBy(() -> constanteVisiteService.enregistrer(visite.getId(),
                mesure(TypeConstante.TA, CreneauMesure.H0, "130", null)))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("obligatoire");
        verify(constanteVisiteRepository, never()).save(any());
    }

    @Test
    void enregistrer_rejette_siDiastoliqueFourniePourUnAutreType() {
        assertThatThrownBy(() -> constanteVisiteService.enregistrer(visite.getId(),
                mesure(TypeConstante.FC, CreneauMesure.H0, "80", "60")))
                .isInstanceOf(RegleGestionException.class);
        verify(constanteVisiteRepository, never()).save(any());
    }

    @Test
    void enregistrer_rejette_siValeurHorsBornesDeValidite() {
        assertThatThrownBy(() -> constanteVisiteService.enregistrer(visite.getId(),
                mesure(TypeConstante.SPO2, CreneauMesure.H0, "140", null)))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("impossible");
        verify(constanteVisiteRepository, never()).save(any());
    }

    @Test
    void enregistrer_rejette_siLaFicheNEstPlusModifiable() {
        doThrow(new ConflitEtatException("La fiche n'est plus modifiable"))
                .when(visiteSuiviService).verifierModifiable(visite.getId());

        assertThatThrownBy(() -> constanteVisiteService.enregistrer(visite.getId(),
                mesure(TypeConstante.FC, CreneauMesure.H0, "80", null)))
                .isInstanceOf(ConflitEtatException.class);
        verify(constanteVisiteRepository, never()).save(any());
    }

    @Test
    void lireGrille_renvoieLaGrilleCalculeeParLeServiceDesVisites() {
        List<LigneGrilleConstantesModel> grille = List.of(
                LigneGrilleConstantesModel.builder().type(TypeConstante.TA).mesures(List.of()).build());
        when(visiteSuiviService.findById(visite.getId()))
                .thenReturn(VisiteSuiviModel.builder().id(visite.getId()).grilleConstantes(grille).build());

        assertThat(constanteVisiteService.lireGrille(visite.getId())).isEqualTo(grille);
    }

    @Test
    void supprimer_retireLaMesureDeLaVisite() {
        ConstanteVisite existante = ConstanteVisite.builder().id(7L).visite(visite)
                .type(TypeConstante.FC).creneau(CreneauMesure.H0).valeur(new BigDecimal("80")).build();
        visite.getConstantes().add(existante);
        when(constanteVisiteRepository.findById(7L)).thenReturn(Optional.of(existante));

        constanteVisiteService.supprimer(visite.getId(), 7L);

        verify(constanteVisiteRepository).delete(existante);
        assertThat(visite.getConstantes()).isEmpty();
    }

    @Test
    void supprimer_rejette_siConstanteIntrouvable_ouRattacheeAUneAutreVisite() {
        when(constanteVisiteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> constanteVisiteService.supprimer(visite.getId(), 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        VisiteSuivi autreVisite = VisiteSuivi.builder().id(UUID.randomUUID()).build();
        ConstanteVisite ailleurs = ConstanteVisite.builder().id(8L).visite(autreVisite)
                .type(TypeConstante.FC).creneau(CreneauMesure.H0).valeur(new BigDecimal("80")).build();
        when(constanteVisiteRepository.findById(8L)).thenReturn(Optional.of(ailleurs));
        assertThatThrownBy(() -> constanteVisiteService.supprimer(visite.getId(), 8L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(constanteVisiteRepository, never()).delete(any());
    }

    private ConstanteVisiteModel mesure(TypeConstante type, CreneauMesure creneau, String valeur, String diastolique) {
        return ConstanteVisiteModel.builder()
                .type(type)
                .creneau(creneau)
                .valeur(new BigDecimal(valeur))
                .valeurDiastolique(diastolique != null ? new BigDecimal(diastolique) : null)
                .build();
    }
}
