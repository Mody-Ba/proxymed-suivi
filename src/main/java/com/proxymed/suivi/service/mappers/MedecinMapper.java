package com.proxymed.suivi.service.mappers;

import com.proxymed.suivi.entity.Medecin;
import com.proxymed.suivi.service.model.MedecinModel;
import org.springframework.stereotype.Component;

/**
 * Mapper DB : convertit uniquement l'entite JPA Medecin en MedecinModel.
 * Ne doit jamais connaitre MedecinResponse.
 */
@Component("dbMedecinMapper")
public class MedecinMapper {

    public MedecinModel toModel(Medecin entity) {
        return MedecinModel.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .prenom(entity.getPrenom())
                .numeroOrdre(entity.getNumeroOrdre())
                .role(entity.getRole())
                .telephone(entity.getTelephone())
                .build();
    }
}
