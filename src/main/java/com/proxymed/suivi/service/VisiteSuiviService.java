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
}
