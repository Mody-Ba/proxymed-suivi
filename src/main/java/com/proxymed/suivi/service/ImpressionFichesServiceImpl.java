package com.proxymed.suivi.service;

import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.service.model.FichesImprimeesModel;
import com.proxymed.suivi.service.model.PatientModel;
import com.proxymed.suivi.service.model.VisiteSuiviModel;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestre PatientService (identite), VisiteSuiviService (visites vierges) et
 * PdfGenerationService, dans une seule transaction : le PDF est entierement ecrit avant la fin de
 * la methode, donc un echec de generation annule aussi les visites creees.
 * Ce que l'admin voit sur la fiche est construit ici, champ par champ (identiteSeulement) : rien
 * d'autre que l'identite du patient ne peut arriver dans le template. Meme approche que PROXYMED 1.
 * Aucune journalisation (ni identite, ni donnee medicale).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ImpressionFichesServiceImpl implements ImpressionFichesService {

    private final PatientService patientService;
    private final VisiteSuiviService visiteSuiviService;
    private final PdfGenerationService pdfGenerationService;

    @Override
    public FichesImprimeesModel imprimer(List<String> numerosDossierProxymed) {
        verifierSansDoublon(numerosDossierProxymed);
        List<PatientModel> patients = patientService.findByNumerosDossierProxymed(numerosDossierProxymed);
        List<UUID> visiteIds = new ArrayList<>();
        List<VisiteSuiviModel> fiches = new ArrayList<>();
        for (PatientModel patient : patients) {
            VisiteSuiviModel visite = visiteSuiviService.creerVisiteVierge(patient.id());
            visiteIds.add(visite.id());
            fiches.add(ficheVierge(visite, identiteSeulement(patient)));
        }
        Resource pdf = pdfGenerationService.genererFichesCombinees(fiches);
        return FichesImprimeesModel.builder().visiteIds(List.copyOf(visiteIds)).pdf(pdf).build();
    }

    /** Deux fois le meme numero creerait deux visites pour le meme patient : double clic ou erreur de selection. */
    private static void verifierSansDoublon(List<String> numeros) {
        Map<String, Integer> premiere = new HashMap<>();
        for (int i = 0; i < numeros.size(); i++) {
            Integer deja = premiere.putIfAbsent(numeros.get(i), i);
            if (deja != null) {
                throw new RegleGestionException("Le numero de dossier n° " + (i + 1) + " (" + numeros.get(i)
                        + ") est en double avec le n° " + (deja + 1) + " : chaque patient ne peut etre demande qu'une fois");
            }
        }
    }

    /** Fiche a imprimer : id et statut de la visite, identite du patient, rien d'autre. */
    static VisiteSuiviModel ficheVierge(VisiteSuiviModel visite, PatientModel identite) {
        return VisiteSuiviModel.builder()
                .id(visite.id())
                .statut(visite.statut())
                .patient(identite)
                .constantes(List.of())
                .grilleConstantes(List.of())
                .signesAlerteCliniques(List.of())
                .alertes(List.of())
                .evenementsIntercurrents(List.of())
                .traitements(List.of())
                .themesAbordes(List.of())
                .build();
    }

    /** Champs d'identite du patient imprimes pour l'admin. */
    static PatientModel identiteSeulement(PatientModel patient) {
        return PatientModel.builder()
                .id(patient.id())
                .numeroDossierProxymed(patient.numeroDossierProxymed())
                .numeroDmi(patient.numeroDmi())
                .nomComplet(patient.nomComplet())
                .dateNaissance(patient.dateNaissance())
                .age(patient.age())
                .sexe(patient.sexe())
                .telephone(patient.telephone())
                .adresseDomicile(patient.adresseDomicile())
                .commune(patient.commune())
                .quartier(patient.quartier())
                .build();
    }
}
