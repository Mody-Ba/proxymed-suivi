package com.proxymed.suivi.mappers;

import com.proxymed.suivi.service.model.AlerteModel;
import com.proxymed.suivi.service.model.AlerteResponse;
import com.proxymed.suivi.service.model.ConstanteVisiteModel;
import com.proxymed.suivi.service.model.ConstanteVisiteRequest;
import com.proxymed.suivi.service.model.ConstanteVisiteResponse;
import com.proxymed.suivi.service.model.LigneGrilleConstantesModel;
import com.proxymed.suivi.service.model.LigneGrilleConstantesResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper API : convertit uniquement ConstanteVisiteRequest -> ConstanteVisiteModel et les
 * modeles de la grille et des alertes vers leurs reponses.
 * Ne doit jamais connaitre l'entite JPA ConstanteVisite.
 */
@Component("apiConstanteVisiteMapper")
public class ConstanteVisiteMapper {

    public ConstanteVisiteModel toModel(ConstanteVisiteRequest req) {
        return ConstanteVisiteModel.builder()
                .type(req.type())
                .creneau(req.creneau())
                .valeur(req.valeur())
                .valeurDiastolique(req.valeurDiastolique())
                .heureMesure(req.heureMesure())
                .build();
    }

    public ConstanteVisiteResponse toResponse(ConstanteVisiteModel model) {
        return ConstanteVisiteResponse.builder()
                .id(model.id())
                .type(model.type())
                .creneau(model.creneau())
                .valeur(model.valeur())
                .valeurDiastolique(model.valeurDiastolique())
                .heureMesure(model.heureMesure())
                .alerte(model.alerte())
                .seuilFranchi(model.seuilFranchi())
                .build();
    }

    public List<LigneGrilleConstantesResponse> toGrilleResponse(List<LigneGrilleConstantesModel> grille) {
        return grille.stream().map(this::toLigneResponse).toList();
    }

    public List<AlerteResponse> toAlerteResponses(List<AlerteModel> alertes) {
        return alertes.stream().map(this::toAlerteResponse).toList();
    }

    private LigneGrilleConstantesResponse toLigneResponse(LigneGrilleConstantesModel ligne) {
        return LigneGrilleConstantesResponse.builder()
                .type(ligne.type())
                .libelle(ligne.type().getLibelle())
                .unite(ligne.type().getUnite())
                .valeursNormales(ligne.type().getValeursNormales())
                .mesures(ligne.mesures().stream().map(this::toResponse).toList())
                .build();
    }

    private AlerteResponse toAlerteResponse(AlerteModel alerte) {
        return AlerteResponse.builder()
                .constante(alerte.constante())
                .creneau(alerte.creneau())
                .valeur(alerte.valeur())
                .valeurDiastolique(alerte.valeurDiastolique())
                .signeClinique(alerte.signeClinique())
                .seuilFranchi(alerte.seuilFranchi())
                .build();
    }
}
