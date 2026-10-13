package com.proxymed.suivi.service;

import com.proxymed.suivi.entity.TraitementVisite;
import com.proxymed.suivi.entity.VisiteSuivi;
import com.proxymed.suivi.exception.ConflitEtatException;
import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.exception.ResourceNotFoundException;
import com.proxymed.suivi.repository.TraitementVisiteRepository;
import com.proxymed.suivi.repository.VisiteSuiviRepository;
import com.proxymed.suivi.service.mappers.TraitementVisiteMapper;
import com.proxymed.suivi.service.model.TraitementVisiteModel;
import com.proxymed.suivi.service.model.VisiteSuiviModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * Teste TraitementVisiteService en isolation : repositories et VisiteSuiviService mockes,
 * mapper DB reel (simple convertisseur + getReferenceById).
 */
@ExtendWith(MockitoExtension.class)
class TraitementVisiteServiceTest {

    @Mock
    private TraitementVisiteRepository traitementVisiteRepository;
    @Mock
    private VisiteSuiviRepository visiteSuiviRepository;
    @Mock
    private VisiteSuiviService visiteSuiviService;

    private TraitementVisiteService traitementVisiteService;

    private VisiteSuivi visite;

    @BeforeEach
    void setUp() {
        traitementVisiteService = new TraitementVisiteServiceImpl(
                traitementVisiteRepository, visiteSuiviService, new TraitementVisiteMapper(visiteSuiviRepository));

        visite = VisiteSuivi.builder().id(UUID.randomUUID()).numeroVisite(1).build();
        lenient().when(visiteSuiviRepository.getReferenceById(visite.getId())).thenReturn(visite);
        lenient().when(traitementVisiteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void ajouter_creeLaLigne_etLaRattacheALaVisite() {
        TraitementVisiteModel resultat = traitementVisiteService.ajouter(visite.getId(), TraitementVisiteModel.builder()
                .medicament("Amlodipine 5 mg").posologie("1 cp le matin").voie("Orale")
                .observant(true).probleme(false).build());

        assertThat(resultat.visiteId()).isEqualTo(visite.getId());
        assertThat(resultat.medicament()).isEqualTo("Amlodipine 5 mg");
        assertThat(resultat.observant()).isTrue();
        assertThat(visite.getTraitements()).hasSize(1);
    }

    @Test
    void ajouter_rejette_siMedicamentAbsentOuVide() {
        for (String medicament : new String[]{null, "", "   "}) {
            assertThatThrownBy(() -> traitementVisiteService.ajouter(visite.getId(),
                    TraitementVisiteModel.builder().medicament(medicament).posologie("1 cp").build()))
                    .isInstanceOf(RegleGestionException.class);
        }
        verify(traitementVisiteRepository, never()).save(any());
    }

    @Test
    void ajouter_rejette_siLaFicheNEstPlusModifiable() {
        doThrow(new ConflitEtatException("La fiche n'est plus modifiable"))
                .when(visiteSuiviService).verifierModifiable(visite.getId());

        assertThatThrownBy(() -> traitementVisiteService.ajouter(visite.getId(),
                TraitementVisiteModel.builder().medicament("Metformine").build()))
                .isInstanceOf(ConflitEtatException.class);
        verify(traitementVisiteRepository, never()).save(any());
    }

    @Test
    void modifier_neChangeQueLesChampsFournis() {
        TraitementVisite existant = traitement(7L, visite);
        when(traitementVisiteRepository.findById(7L)).thenReturn(Optional.of(existant));

        TraitementVisiteModel resultat = traitementVisiteService.modifier(visite.getId(), 7L,
                TraitementVisiteModel.builder().observant(false).probleme(true).problemePrecision("Oublis le soir").build());

        assertThat(resultat.id()).isEqualTo(7L);
        assertThat(resultat.medicament()).isEqualTo("Metformine 850 mg");
        assertThat(resultat.posologie()).isEqualTo("1 cp matin et soir");
        assertThat(resultat.observant()).isFalse();
        assertThat(resultat.probleme()).isTrue();
        assertThat(resultat.problemePrecision()).isEqualTo("Oublis le soir");
    }

    @Test
    void modifier_rejette_siMedicamentVide() {
        TraitementVisite existant = traitement(7L, visite);
        when(traitementVisiteRepository.findById(7L)).thenReturn(Optional.of(existant));

        assertThatThrownBy(() -> traitementVisiteService.modifier(visite.getId(), 7L,
                TraitementVisiteModel.builder().medicament(" ").build()))
                .isInstanceOf(RegleGestionException.class);
        assertThat(existant.getMedicament()).isEqualTo("Metformine 850 mg");
        verify(traitementVisiteRepository, never()).save(any());
    }

    @Test
    void modifierEtSupprimer_rejettent_siTraitementIntrouvable_ouRattacheAUneAutreVisite() {
        when(traitementVisiteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> traitementVisiteService.modifier(visite.getId(), 99L,
                TraitementVisiteModel.builder().posologie("2 cp").build()))
                .isInstanceOf(ResourceNotFoundException.class);

        VisiteSuivi autreVisite = VisiteSuivi.builder().id(UUID.randomUUID()).build();
        TraitementVisite ailleurs = traitement(8L, autreVisite);
        when(traitementVisiteRepository.findById(8L)).thenReturn(Optional.of(ailleurs));
        assertThatThrownBy(() -> traitementVisiteService.modifier(visite.getId(), 8L,
                TraitementVisiteModel.builder().posologie("2 cp").build()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> traitementVisiteService.supprimer(visite.getId(), 8L))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(ailleurs.getPosologie()).isEqualTo("1 cp matin et soir");
        verify(traitementVisiteRepository, never()).save(any());
        verify(traitementVisiteRepository, never()).delete(any());
    }

    @Test
    void supprimer_retireLaLigneDeLaVisite() {
        TraitementVisite existant = traitement(7L, visite);
        visite.getTraitements().add(existant);
        when(traitementVisiteRepository.findById(7L)).thenReturn(Optional.of(existant));

        traitementVisiteService.supprimer(visite.getId(), 7L);

        verify(traitementVisiteRepository).delete(existant);
        assertThat(visite.getTraitements()).isEmpty();
    }

    @Test
    void supprimer_rejette_siLaFicheNEstPlusModifiable() {
        doThrow(new ConflitEtatException("La fiche n'est plus modifiable"))
                .when(visiteSuiviService).verifierModifiable(visite.getId());

        assertThatThrownBy(() -> traitementVisiteService.supprimer(visite.getId(), 7L))
                .isInstanceOf(ConflitEtatException.class);
        verify(traitementVisiteRepository, never()).delete(any());
    }

    @Test
    void lister_renvoieLesTraitementsLusParLeServiceDesVisites() {
        List<TraitementVisiteModel> traitements = List.of(
                TraitementVisiteModel.builder().id(1L).medicament("Amlodipine 5 mg").build());
        when(visiteSuiviService.findById(visite.getId()))
                .thenReturn(VisiteSuiviModel.builder().id(visite.getId()).traitements(traitements).build());

        assertThat(traitementVisiteService.lister(visite.getId())).isEqualTo(traitements);
    }

    private TraitementVisite traitement(Long id, VisiteSuivi visiteParente) {
        return TraitementVisite.builder().id(id).visite(visiteParente)
                .medicament("Metformine 850 mg").posologie("1 cp matin et soir").voie("Orale")
                .observant(true).probleme(false).build();
    }
}
