package com.proxymed.suivi.mappers;

import com.proxymed.suivi.service.model.TraitementVisiteModel;
import com.proxymed.suivi.service.model.TraitementVisiteRequest;
import com.proxymed.suivi.service.model.TraitementVisiteResponse;
import com.proxymed.suivi.service.model.TraitementVisiteUpdateRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper API : convertit uniquement TraitementVisiteRequest/UpdateRequest -> TraitementVisiteModel
 * et TraitementVisiteModel -> TraitementVisiteResponse.
 * Ne doit jamais connaitre l'entite JPA TraitementVisite.
 */
@Component("apiTraitementVisiteMapper")
public class TraitementVisiteMapper {

    public TraitementVisiteModel toModel(TraitementVisiteRequest req) {
        return TraitementVisiteModel.builder()
                .medicament(req.medicament())
                .posologie(req.posologie())
                .voie(req.voie())
                .observant(req.observant())
                .probleme(req.probleme())
                .problemePrecision(req.problemePrecision())
                .build();
    }

    public TraitementVisiteModel toModel(TraitementVisiteUpdateRequest req) {
        return TraitementVisiteModel.builder()
                .medicament(req.medicament())
                .posologie(req.posologie())
                .voie(req.voie())
                .observant(req.observant())
                .probleme(req.probleme())
                .problemePrecision(req.problemePrecision())
                .build();
    }

    public TraitementVisiteResponse toResponse(TraitementVisiteModel model) {
        return TraitementVisiteResponse.builder()
                .id(model.id())
                .medicament(model.medicament())
                .posologie(model.posologie())
                .voie(model.voie())
                .observant(model.observant())
                .probleme(model.probleme())
                .problemePrecision(model.problemePrecision())
                .build();
    }

    public List<TraitementVisiteResponse> toResponseList(List<TraitementVisiteModel> models) {
        return models.stream().map(this::toResponse).toList();
    }
}
