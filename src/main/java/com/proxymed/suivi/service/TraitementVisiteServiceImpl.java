package com.proxymed.suivi.service;

import com.proxymed.suivi.entity.TraitementVisite;
import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.exception.ResourceNotFoundException;
import com.proxymed.suivi.repository.TraitementVisiteRepository;
import com.proxymed.suivi.service.mappers.TraitementVisiteMapper;
import com.proxymed.suivi.service.model.TraitementVisiteModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * La seule entite JPA visible dans cette classe est TraitementVisite (l'aggregat propre a ce
 * service). VisiteSuivi (aggregat parent) n'est jamais manipulee ici : la verification
 * qu'elle existe et est modifiable passe par VisiteSuiviService.verifierModifiable, et le
 * rattachement JPA (id -> reference) est delegue au mapper DB.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TraitementVisiteServiceImpl implements TraitementVisiteService {

    private final TraitementVisiteRepository traitementVisiteRepository;
    private final VisiteSuiviService visiteSuiviService;
    private final TraitementVisiteMapper traitementVisiteMapper;

    @Override
    public List<TraitementVisiteModel> lister(UUID visiteId) {
        return visiteSuiviService.findById(visiteId).traitements();
    }

    @Override
    public TraitementVisiteModel ajouter(UUID visiteId, TraitementVisiteModel model) {
        visiteSuiviService.verifierModifiable(visiteId);
        verifierMedicament(model);

        TraitementVisite entity = traitementVisiteRepository.save(traitementVisiteMapper.toEntity(model, visiteId));
        traitementVisiteMapper.synchroniserAvecVisite(visiteId, entity);
        return traitementVisiteMapper.toModel(entity);
    }

    @Override
    public TraitementVisiteModel modifier(UUID visiteId, Long traitementId, TraitementVisiteModel intention) {
        visiteSuiviService.verifierModifiable(visiteId);
        TraitementVisite entity = getEntity(visiteId, traitementId);

        TraitementVisiteModel fusionne = fusionner(traitementVisiteMapper.toModel(entity), intention);
        verifierMedicament(fusionne);
        traitementVisiteMapper.applyToEntity(entity, fusionne);
        return traitementVisiteMapper.toModel(traitementVisiteRepository.save(entity));
    }

    @Override
    public void supprimer(UUID visiteId, Long traitementId) {
        visiteSuiviService.verifierModifiable(visiteId);
        TraitementVisite entity = getEntity(visiteId, traitementId);
        traitementVisiteMapper.desynchroniserDeVisite(visiteId, entity);
        traitementVisiteRepository.delete(entity);
    }

    /**
     * Le traitement doit exister et appartenir a la visite de l'URL (404 sinon).
     */
    private TraitementVisite getEntity(UUID visiteId, Long traitementId) {
        TraitementVisite entity = traitementVisiteRepository.findById(traitementId)
                .orElseThrow(() -> new ResourceNotFoundException("Traitement introuvable : " + traitementId));
        if (!visiteId.equals(traitementVisiteMapper.toModel(entity).visiteId())) {
            throw new ResourceNotFoundException("Traitement introuvable pour cette visite : " + traitementId);
        }
        return entity;
    }

    private void verifierMedicament(TraitementVisiteModel model) {
        if (model.medicament() == null || model.medicament().isBlank()) {
            throw new RegleGestionException("Le medicament est obligatoire");
        }
    }

    private TraitementVisiteModel fusionner(TraitementVisiteModel courant, TraitementVisiteModel maj) {
        return courant.toBuilder()
                .medicament(coalesce(maj.medicament(), courant.medicament()))
                .posologie(coalesce(maj.posologie(), courant.posologie()))
                .voie(coalesce(maj.voie(), courant.voie()))
                .observant(coalesce(maj.observant(), courant.observant()))
                .probleme(coalesce(maj.probleme(), courant.probleme()))
                .problemePrecision(coalesce(maj.problemePrecision(), courant.problemePrecision()))
                .build();
    }

    private static <T> T coalesce(T nouvelle, T actuelle) {
        return nouvelle != null ? nouvelle : actuelle;
    }
}
