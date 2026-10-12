package com.proxymed.suivi.service;

import com.proxymed.suivi.entity.ConstanteVisite;
import com.proxymed.suivi.enums.TypeConstante;
import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.exception.ResourceNotFoundException;
import com.proxymed.suivi.repository.ConstanteVisiteRepository;
import com.proxymed.suivi.service.mappers.ConstanteVisiteMapper;
import com.proxymed.suivi.service.model.ConstanteVisiteModel;
import com.proxymed.suivi.service.model.LigneGrilleConstantesModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * La seule entite JPA visible dans cette classe est ConstanteVisite (l'aggregat propre a ce
 * service). VisiteSuivi (aggregat parent) n'est jamais manipulee ici : la verification
 * qu'elle existe et est modifiable passe par VisiteSuiviService.verifierModifiable, et le
 * rattachement JPA (id -> reference) est delegue au mapper DB.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ConstanteVisiteServiceImpl implements ConstanteVisiteService {

    private final ConstanteVisiteRepository constanteVisiteRepository;
    private final VisiteSuiviService visiteSuiviService;
    private final ConstanteVisiteMapper constanteVisiteMapper;

    @Override
    public ConstanteVisiteModel enregistrer(UUID visiteId, ConstanteVisiteModel model) {
        visiteSuiviService.verifierModifiable(visiteId);
        verifierValeurDiastolique(model);
        BornesConstantesVitales.valider(model);

        Optional<ConstanteVisite> existante = constanteVisiteRepository
                .findByVisiteIdAndTypeAndCreneau(visiteId, model.type(), model.creneau());
        ConstanteVisite entity;
        if (existante.isPresent()) {
            entity = existante.get();
            constanteVisiteMapper.applyToEntity(entity, model);
            entity = constanteVisiteRepository.save(entity);
        } else {
            entity = constanteVisiteRepository.save(constanteVisiteMapper.toEntity(model, visiteId));
            constanteVisiteMapper.synchroniserAvecVisite(visiteId, entity);
        }
        return SeuilsAlerte.appliquer(constanteVisiteMapper.toModel(entity));
    }

    @Override
    public List<LigneGrilleConstantesModel> lireGrille(UUID visiteId) {
        return visiteSuiviService.findById(visiteId).grilleConstantes();
    }

    @Override
    public void supprimer(UUID visiteId, Long constanteId) {
        visiteSuiviService.verifierModifiable(visiteId);
        ConstanteVisite entity = constanteVisiteRepository.findById(constanteId)
                .orElseThrow(() -> new ResourceNotFoundException("Constante introuvable : " + constanteId));
        if (!visiteId.equals(constanteVisiteMapper.toModel(entity).visiteId())) {
            throw new ResourceNotFoundException("Constante introuvable pour cette visite : " + constanteId);
        }
        constanteVisiteMapper.desynchroniserDeVisite(visiteId, entity);
        constanteVisiteRepository.delete(entity);
    }

    /**
     * La diastolique est obligatoire pour la tension arterielle (valeur porte la systolique)
     * et interdite pour toutes les autres constantes.
     */
    private void verifierValeurDiastolique(ConstanteVisiteModel model) {
        if (model.type() == TypeConstante.TA && model.valeurDiastolique() == null) {
            throw new RegleGestionException(
                    "La valeur diastolique est obligatoire pour la tension arterielle (valeur = systolique)");
        }
        if (model.type() != TypeConstante.TA && model.valeurDiastolique() != null) {
            throw new RegleGestionException(
                    "La valeur diastolique n'est acceptee que pour la tension arterielle (TA)");
        }
    }
}
