package com.proxymed.suivi.service;

import com.proxymed.suivi.entity.FicheSignee;
import com.proxymed.suivi.entity.VisiteSuivi;
import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.exception.ResourceNotFoundException;
import com.proxymed.suivi.repository.FicheSigneeRepository;
import com.proxymed.suivi.repository.VisiteSuiviRepository;
import com.proxymed.suivi.service.mappers.FicheSigneeMapper;
import com.proxymed.suivi.service.model.FicheSigneeModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
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
 * Teste FicheSigneeService en isolation : repositories et VisiteSuiviService mockes,
 * mapper DB reel (simple convertisseur + getReferenceById).
 */
@ExtendWith(MockitoExtension.class)
class FicheSigneeServiceTest {

    private static final byte[] PDF = "%PDF-1.7\nfiche signee".getBytes(StandardCharsets.US_ASCII);

    @Mock
    private FicheSigneeRepository ficheSigneeRepository;
    @Mock
    private VisiteSuiviRepository visiteSuiviRepository;
    @Mock
    private VisiteSuiviService visiteSuiviService;

    private FicheSigneeService ficheSigneeService;

    private VisiteSuivi visite;

    @BeforeEach
    void setUp() {
        ficheSigneeService = new FicheSigneeServiceImpl(
                ficheSigneeRepository, visiteSuiviService, new FicheSigneeMapper(visiteSuiviRepository));

        visite = VisiteSuivi.builder().id(UUID.randomUUID()).numeroVisite(1).build();
        lenient().when(visiteSuiviRepository.getReferenceById(visite.getId())).thenReturn(visite);
        lenient().when(ficheSigneeRepository.save(any())).thenAnswer(inv -> {
            FicheSignee fiche = inv.getArgument(0);
            fiche.setId(UUID.randomUUID());
            return fiche;
        });
    }

    @Test
    void importer_enregistreLePdf_avecSaTailleEtSaDateDImport() {
        FicheSigneeModel resultat = ficheSigneeService.importer(visite.getId(), fichier("fiche-signee.pdf", "application/pdf", PDF));

        assertThat(resultat.id()).isNotNull();
        assertThat(resultat.visiteId()).isEqualTo(visite.getId());
        assertThat(resultat.nomFichier()).isEqualTo("fiche-signee.pdf");
        assertThat(resultat.tailleOctets()).isEqualTo(PDF.length);
        assertThat(resultat.dateImport()).isNotNull();
        assertThat(resultat.contenu()).isEqualTo(PDF);
    }

    @Test
    void importer_rejette_siLeTypeNEstPasPdf_ouSiLeContenuNeCommencePasParPdf() {
        assertThatThrownBy(() -> ficheSigneeService.importer(visite.getId(), fichier("fiche.pdf", "image/png", PDF)))
                .isInstanceOf(RegleGestionException.class).hasMessageContaining("PDF");
        assertThatThrownBy(() -> ficheSigneeService.importer(visite.getId(),
                fichier("photo.pdf", "application/pdf", "pas un pdf".getBytes(StandardCharsets.US_ASCII))))
                .isInstanceOf(RegleGestionException.class);
        assertThatThrownBy(() -> ficheSigneeService.importer(visite.getId(), fichier("vide.pdf", "application/pdf", new byte[0])))
                .isInstanceOf(RegleGestionException.class);
        assertThatThrownBy(() -> ficheSigneeService.importer(visite.getId(), fichier("sans-type.pdf", null, PDF)))
                .isInstanceOf(RegleGestionException.class);
        verify(ficheSigneeRepository, never()).save(any());
    }

    @Test
    void importer_rejette_auDelaDe10Mo_etAccepteExactement10Mo() {
        byte[] trop = pdfDeTaille(FicheSigneeServiceImpl.TAILLE_MAX_OCTETS + 1);
        assertThatThrownBy(() -> ficheSigneeService.importer(visite.getId(), fichier("gros.pdf", "application/pdf", trop)))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("10 Mo");
        verify(ficheSigneeRepository, never()).save(any());

        byte[] limite = pdfDeTaille(FicheSigneeServiceImpl.TAILLE_MAX_OCTETS);
        assertThat(ficheSigneeService.importer(visite.getId(), fichier("limite.pdf", "application/pdf", limite)).tailleOctets())
                .isEqualTo(FicheSigneeServiceImpl.TAILLE_MAX_OCTETS);
    }

    @Test
    void importer_rejette_siVisiteIntrouvable() {
        UUID inconnue = UUID.randomUUID();
        when(visiteSuiviService.findById(inconnue)).thenThrow(new ResourceNotFoundException("Visite introuvable : " + inconnue));

        assertThatThrownBy(() -> ficheSigneeService.importer(inconnue, fichier("fiche.pdf", "application/pdf", PDF)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ficheSigneeRepository, never()).save(any());
    }

    @Test
    void importer_gardeLeNomSansChemin_etDonneUnNomParDefaut() {
        ArgumentCaptor<FicheSignee> enregistree = ArgumentCaptor.forClass(FicheSignee.class);

        ficheSigneeService.importer(visite.getId(), fichier("C:\\Users\\junior\\Scans\\fiche 12.pdf", "application/pdf", PDF));
        ficheSigneeService.importer(visite.getId(), fichier("  ", "application/pdf", PDF));
        ficheSigneeService.importer(visite.getId(), fichier(null, "application/pdf", PDF));

        verify(ficheSigneeRepository, org.mockito.Mockito.times(3)).save(enregistree.capture());
        assertThat(enregistree.getAllValues()).extracting(FicheSignee::getNomFichier)
                .containsExactly("fiche 12.pdf", "fiche-signee.pdf", "fiche-signee.pdf");
    }

    @Test
    void telecharger_renvoieLaFicheLaPlusRecente() {
        FicheSignee fiche = FicheSignee.builder().id(UUID.randomUUID()).visite(visite).nomFichier("fiche.pdf")
                .typeContenu("application/pdf").tailleOctets(PDF.length).dateImport(Instant.now()).contenu(PDF).build();
        when(ficheSigneeRepository.findFirstByVisite_IdOrderByDateImportDesc(visite.getId())).thenReturn(Optional.of(fiche));

        FicheSigneeModel resultat = ficheSigneeService.telecharger(visite.getId());

        assertThat(resultat.id()).isEqualTo(fiche.getId());
        assertThat(resultat.contenu()).isEqualTo(PDF);
    }

    @Test
    void telecharger_rejette_siAucuneFicheImportee_ouVisiteIntrouvable() {
        when(ficheSigneeRepository.findFirstByVisite_IdOrderByDateImportDesc(visite.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ficheSigneeService.telecharger(visite.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Aucune fiche signee");

        UUID inconnue = UUID.randomUUID();
        when(visiteSuiviService.findById(inconnue)).thenThrow(new ResourceNotFoundException("Visite introuvable : " + inconnue));
        assertThatThrownBy(() -> ficheSigneeService.telecharger(inconnue))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Visite introuvable");
    }

    private FicheSigneeModel fichier(String nom, String type, byte[] contenu) {
        return FicheSigneeModel.builder().nomFichier(nom).typeContenu(type).tailleOctets(contenu.length).contenu(contenu).build();
    }

    private byte[] pdfDeTaille(long taille) {
        byte[] contenu = new byte[Math.toIntExact(taille)];
        Arrays.fill(contenu, (byte) ' ');
        System.arraycopy(PDF, 0, contenu, 0, PDF.length);
        return contenu;
    }
}
