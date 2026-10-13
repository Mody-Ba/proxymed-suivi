package com.proxymed.suivi.service;

import com.proxymed.suivi.enums.CreneauMesure;
import com.proxymed.suivi.enums.EtatGeneral;
import com.proxymed.suivi.enums.TypeConstante;
import com.proxymed.suivi.service.model.VisiteSuiviModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Regle 8 : champs requis pour terminer une fiche. Renvoie tous les manquements d'un coup,
 * pour que le medecin puisse completer la fiche en une seule fois.
 */
final class ControleFicheTerminee {

    private ControleFicheTerminee() {
    }

    /**
     * Liste vide si la fiche peut etre terminee.
     */
    static List<String> manquements(VisiteSuiviModel visite) {
        List<String> manquements = new ArrayList<>();

        // Identification complete (le patient et les deux medecins sont toujours renseignes)
        exiger(manquements, visite.dateVisite(), "dateVisite : la date de la visite est obligatoire");
        exiger(manquements, visite.heureArrivee(), "heureArrivee : l'heure d'arrivee est obligatoire");
        exiger(manquements, visite.typeVisite(), "typeVisite : le type de visite est obligatoire");
        exigerTexte(manquements, visite.contactSenior(), "contactSenior : le contact du medecin senior est obligatoire");
        exiger(manquements, visite.heureDepart(), "heureDepart : l'heure de depart est obligatoire");

        exiger(manquements, visite.etatGeneral(), "etatGeneral : l'etat general est obligatoire");
        if (visite.etatGeneral() == EtatGeneral.ALTERE) {
            exigerTexte(manquements, visite.etatGeneralPrecision(),
                    "etatGeneralPrecision : la precision est obligatoire quand l'etat general est ALTERE");
        }

        List<String> constantesAbsentes = Arrays.stream(TypeConstante.values())
                .filter(type -> visite.constantes().stream()
                        .noneMatch(c -> c.type() == type && c.creneau() == CreneauMesure.H0))
                .map(TypeConstante::name)
                .toList();
        if (!constantesAbsentes.isEmpty()) {
            manquements.add("constantes : mesures du creneau H0 manquantes (" + String.join(", ", constantesAbsentes) + ")");
        }

        if (Boolean.TRUE.equals(visite.alerteDeclenchee())) {
            exiger(manquements, visite.heureAppelSenior(),
                    "heureAppelSenior : l'heure d'appel du senior est obligatoire quand une alerte est declenchee");
            exigerTexte(manquements, visite.decisionAppelSenior(),
                    "decisionAppelSenior : la decision du senior est obligatoire quand une alerte est declenchee");
        }

        exiger(manquements, visite.observanceTraitement(), "observanceTraitement : l'observance est obligatoire");

        exiger(manquements, visite.situationSociale(), "situationSociale : la situation sociale est obligatoire");
        if (Boolean.TRUE.equals(visite.signalementDass())) {
            exiger(manquements, visite.heureContactDass(),
                    "heureContactDass : l'heure de contact de la DASS est obligatoire quand un signalement est requis");
        }

        exiger(manquements, visite.decision(), "decision : la decision est obligatoire");
        exigerTexte(manquements, visite.resumeClinique(), "resumeClinique : le resume clinique est obligatoire");
        return manquements;
    }

    private static void exiger(List<String> manquements, Object valeur, String message) {
        if (valeur == null) {
            manquements.add(message);
        }
    }

    private static void exigerTexte(List<String> manquements, String valeur, String message) {
        if (valeur == null || valeur.isBlank()) {
            manquements.add(message);
        }
    }
}
