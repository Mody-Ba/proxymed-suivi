package com.proxymed.suivi.mappers;

import com.proxymed.suivi.service.model.MedecinModel;
import com.proxymed.suivi.service.model.MedecinResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper API : convertit uniquement MedecinModel -> MedecinResponse.
 * Ne doit jamais connaitre l'entite JPA Medecin.
 */
@Component("apiMedecinMapper")
public class MedecinMapper {

    public MedecinResponse toResponse(MedecinModel model) {
        return MedecinResponse.builder()
                .id(model.id())
                .nom(model.nom())
                .prenom(model.prenom())
                .numeroOrdre(model.numeroOrdre())
                .role(model.role())
                .telephone(model.telephone())
                .build();
    }
}
