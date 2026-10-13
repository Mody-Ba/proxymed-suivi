package com.proxymed.suivi.service.mappers;

import com.proxymed.suivi.entity.TraitementVisite;
import com.proxymed.suivi.repository.VisiteSuiviRepository;
import com.proxymed.suivi.service.model.TraitementVisiteModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Mapper DB : convertit uniquement entre TraitementVisiteModel et l'entite JPA TraitementVisite.
 * Ne doit jamais connaitre TraitementVisiteRequest/UpdateRequest/Response.
 * Resout lui-meme la reference JPA vers la visite parente (getReferenceById, simple
 * traduction d'un id en relation) : le service qui l'appelle ne manipule ainsi jamais
 * l'entite VisiteSuivi, seulement son id.
 */
@Component("dbTraitementVisiteMapper")
@RequiredArgsConstructor
public class TraitementVisiteMapper {

    private final VisiteSuiviRepository visiteSuiviRepository;

    public TraitementVisite toEntity(TraitementVisiteModel model, UUID visiteId) {
        return TraitementVisite.builder()
                .visite(visiteSuiviRepository.getReferenceById(visiteId))
                .medicament(model.medicament())
                .posologie(model.posologie())
                .voie(model.voie())
                .observant(model.observant())
                .probleme(model.probleme())
                .problemePrecision(model.problemePrecision())
                .build();
    }

    /**
     * Applique le modele sur une ligne existante (mutation en place).
     */
    public void applyToEntity(TraitementVisite entity, TraitementVisiteModel model) {
        entity.setMedicament(model.medicament());
        entity.setPosologie(model.posologie());
        entity.setVoie(model.voie());
        entity.setObservant(model.observant());
        entity.setProbleme(model.probleme());
        entity.setProblemePrecision(model.problemePrecision());
    }

    public TraitementVisiteModel toModel(TraitementVisite entity) {
        return TraitementVisiteModel.builder()
                .id(entity.getId())
                .visiteId(entity.getVisite().getId())
                .medicament(entity.getMedicament())
                .posologie(entity.getPosologie())
                .voie(entity.getVoie())
                .observant(entity.getObservant())
                .probleme(entity.getProbleme())
                .problemePrecision(entity.getProblemePrecision())
                .build();
    }

    public List<TraitementVisiteModel> toModelList(List<TraitementVisite> entities) {
        return entities.stream().map(this::toModel).toList();
    }

    /**
     * Rattache le traitement a la collection en memoire de la visite parente (meme raison
     * que pour les constantes : la collection deja chargee resterait sinon perimee si la
     * visite est relue dans la meme transaction).
     */
    public void synchroniserAvecVisite(UUID visiteId, TraitementVisite traitement) {
        visiteSuiviRepository.getReferenceById(visiteId).getTraitements().add(traitement);
    }

    /**
     * Retire le traitement de la collection en memoire de la visite parente, avant sa suppression.
     */
    public void desynchroniserDeVisite(UUID visiteId, TraitementVisite traitement) {
        visiteSuiviRepository.getReferenceById(visiteId).getTraitements().remove(traitement);
    }
}
