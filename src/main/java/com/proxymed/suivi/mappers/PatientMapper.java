package com.proxymed.suivi.mappers;

import com.proxymed.suivi.service.model.PatientModel;
import com.proxymed.suivi.service.model.PatientRequest;
import com.proxymed.suivi.service.model.PatientResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper API : convertit uniquement PatientRequest -> PatientModel et PatientModel -> PatientResponse.
 * Ne doit jamais connaitre l'entite JPA Patient.
 * L'age n'est pas calcule ici : PatientService le derive de la date de naissance
 * et le renseigne sur le PatientModel avant tout retour au controller.
 */
@Component("apiPatientMapper")
public class PatientMapper {

    public PatientModel toModel(PatientRequest req) {
        return PatientModel.builder()
                .numeroDossierProxymed(req.numeroDossierProxymed())
                .numeroDmi(req.numeroDmi())
                .nomComplet(req.nomComplet())
                .dateNaissance(req.dateNaissance())
                .sexe(req.sexe())
                .telephone(req.telephone())
                .adresseDomicile(req.adresseDomicile())
                .commune(req.commune())
                .quartier(req.quartier())
                .build();
    }

    public PatientResponse toResponse(PatientModel model) {
        return PatientResponse.builder()
                .id(model.id())
                .numeroDossierProxymed(model.numeroDossierProxymed())
                .numeroDmi(model.numeroDmi())
                .nomComplet(model.nomComplet())
                .dateNaissance(model.dateNaissance())
                .age(model.age())
                .sexe(model.sexe())
                .telephone(model.telephone())
                .adresseDomicile(model.adresseDomicile())
                .commune(model.commune())
                .quartier(model.quartier())
                .build();
    }
}
