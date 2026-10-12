package com.proxymed.suivi.service;

import com.proxymed.suivi.service.model.PatientModel;

import java.util.List;
import java.util.UUID;

public interface PatientService {

    List<PatientModel> findAll();

    PatientModel findById(UUID id);

    PatientModel create(PatientModel model);

    /**
     * Renseigne l'age (derive de dateNaissance) sur un PatientModel produit hors de ce service,
     * par exemple imbrique dans une visite. Point unique de calcul de l'age.
     */
    PatientModel completerAge(PatientModel model);
}
