package com.proxymed.suivi.service;

import com.proxymed.suivi.service.model.PatientModel;

import java.util.List;
import java.util.UUID;

public interface PatientService {

    List<PatientModel> findAll();

    PatientModel findById(UUID id);

    PatientModel create(PatientModel model);

    /**
     * Patients portant ces numeros de dossier PROXYMED, dans l'ordre de la liste. Tout ou rien :
     * si un seul numero est inconnu, aucune donnee n'est renvoyee.
     *
     * @throws com.proxymed.suivi.exception.ResourceNotFoundException avec, en details, chaque
     *                                                               numero introuvable
     */
    List<PatientModel> findByNumerosDossierProxymed(List<String> numerosDossierProxymed);

    /**
     * Renseigne l'age (derive de dateNaissance) sur un PatientModel produit hors de ce service,
     * par exemple imbrique dans une visite. Point unique de calcul de l'age.
     */
    PatientModel completerAge(PatientModel model);
}
