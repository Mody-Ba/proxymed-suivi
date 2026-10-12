package com.proxymed.suivi.service.mappers;

import com.proxymed.suivi.entity.ConstanteVisite;
import com.proxymed.suivi.repository.VisiteSuiviRepository;
import com.proxymed.suivi.service.model.ConstanteVisiteModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Mapper DB : convertit uniquement entre ConstanteVisiteModel et l'entite JPA ConstanteVisite.
 * Ne doit jamais connaitre ConstanteVisiteRequest/ConstanteVisiteResponse.
 * Resout lui-meme la reference JPA vers la visite parente (getReferenceById, simple
 * traduction d'un id en relation) : le service qui l'appelle ne manipule ainsi jamais
 * l'entite VisiteSuivi, seulement son id.
 */
@Component("dbConstanteVisiteMapper")
@RequiredArgsConstructor
public class ConstanteVisiteMapper {

    private final VisiteSuiviRepository visiteSuiviRepository;

    public ConstanteVisite toEntity(ConstanteVisiteModel model, UUID visiteId) {
        return ConstanteVisite.builder()
                .visite(visiteSuiviRepository.getReferenceById(visiteId))
                .type(model.type())
                .creneau(model.creneau())
                .valeur(model.valeur())
                .valeurDiastolique(model.valeurDiastolique())
                .heureMesure(model.heureMesure())
                .build();
    }

    /**
     * Remplace la mesure d'une case existante (meme type et meme creneau) : mutation en place.
     */
    public void applyToEntity(ConstanteVisite entity, ConstanteVisiteModel model) {
        entity.setValeur(model.valeur());
        entity.setValeurDiastolique(model.valeurDiastolique());
        entity.setHeureMesure(model.heureMesure());
    }

    public ConstanteVisiteModel toModel(ConstanteVisite entity) {
        return ConstanteVisiteModel.builder()
                .id(entity.getId())
                .visiteId(entity.getVisite().getId())
                .type(entity.getType())
                .creneau(entity.getCreneau())
                .valeur(entity.getValeur())
                .valeurDiastolique(entity.getValeurDiastolique())
                .heureMesure(entity.getHeureMesure())
                .build();
    }

    public List<ConstanteVisiteModel> toModelList(List<ConstanteVisite> entities) {
        return entities.stream().map(this::toModel).toList();
    }

    /**
     * Rattache la constante a la collection en memoire de la visite parente.
     * Indispensable si la meme entite/session est relue plus tard dans la meme transaction
     * (ex. tests d'integration @Transactional) : sans cela, la collection lazy deja chargee
     * resterait perimee. Pure bascule de persistance JPA, pas une regle metier : c'est
     * pourquoi elle vit dans ce mapper plutot que dans le service, qui ne doit jamais
     * toucher l'entite VisiteSuivi.
     */
    public void synchroniserAvecVisite(UUID visiteId, ConstanteVisite constante) {
        visiteSuiviRepository.getReferenceById(visiteId).getConstantes().add(constante);
    }

    /**
     * Retire la constante de la collection en memoire de la visite parente, avant sa
     * suppression (sinon la collection deja chargee garderait une mesure supprimee).
     */
    public void desynchroniserDeVisite(UUID visiteId, ConstanteVisite constante) {
        visiteSuiviRepository.getReferenceById(visiteId).getConstantes().remove(constante);
    }
}
