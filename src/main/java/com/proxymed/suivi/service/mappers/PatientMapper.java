package com.proxymed.suivi.service.mappers;

import com.proxymed.suivi.entity.Patient;
import com.proxymed.suivi.service.model.PatientModel;
import org.springframework.stereotype.Component;

/**
 * Mapper DB : convertit uniquement entre PatientModel et l'entite JPA Patient.
 * Ne doit jamais connaitre PatientRequest/PatientResponse.
 * L'entite Patient ne porte pas l'age (champ derive) : c'est PatientService qui le
 * calcule a partir de dateNaissance et le renseigne sur le PatientModel.
 */
@Component("dbPatientMapper")
public class PatientMapper {

    public Patient toEntity(PatientModel model) {
        return Patient.builder()
                .numeroDossierProxymed(model.numeroDossierProxymed())
                .numeroDmi(model.numeroDmi())
                .nomComplet(model.nomComplet())
                .dateNaissance(model.dateNaissance())
                .sexe(model.sexe())
                .telephone(model.telephone())
                .adresseDomicile(model.adresseDomicile())
                .commune(model.commune())
                .quartier(model.quartier())
                .build();
    }

    public PatientModel toModel(Patient entity) {
        return PatientModel.builder()
                .id(entity.getId())
                .numeroDossierProxymed(entity.getNumeroDossierProxymed())
                .numeroDmi(entity.getNumeroDmi())
                .nomComplet(entity.getNomComplet())
                .dateNaissance(entity.getDateNaissance())
                .sexe(entity.getSexe())
                .telephone(entity.getTelephone())
                .adresseDomicile(entity.getAdresseDomicile())
                .commune(entity.getCommune())
                .quartier(entity.getQuartier())
                .build();
    }
}
