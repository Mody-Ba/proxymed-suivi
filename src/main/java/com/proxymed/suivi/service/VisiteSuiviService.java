package com.proxymed.suivi.service;

import com.proxymed.suivi.service.model.VisiteSuiviModel;

import java.util.List;
import java.util.UUID;

public interface VisiteSuiviService {

    VisiteSuiviModel findById(UUID id);

    /**
     * Visites d'un patient, de la plus recente a la plus ancienne.
     */
    List<VisiteSuiviModel> findByPatient(UUID patientId);

    /**
     * Cree une visite en BROUILLON. Le numero de visite est calcule (cumul par patient).
     */
    VisiteSuiviModel creerBrouillon(VisiteSuiviModel intention);

    /**
     * Mise a jour partielle : seuls les champs non nuls de l'intention sont appliques.
     */
    VisiteSuiviModel mettreAJour(UUID id, VisiteSuiviModel intention);

    /**
     * Verifie que la visite existe (404) et est encore en BROUILLON (409). Sert aux services
     * des elements rattaches a la visite (constantes, traitements) avant toute modification.
     */
    void verifierModifiable(UUID id);
}
