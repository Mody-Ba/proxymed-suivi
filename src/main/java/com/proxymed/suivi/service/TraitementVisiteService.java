package com.proxymed.suivi.service;

import com.proxymed.suivi.service.model.TraitementVisiteModel;

import java.util.List;
import java.util.UUID;

public interface TraitementVisiteService {

    /**
     * Traitements de la visite, dans l'ordre de saisie.
     */
    List<TraitementVisiteModel> lister(UUID visiteId);

    TraitementVisiteModel ajouter(UUID visiteId, TraitementVisiteModel model);

    /**
     * Mise a jour partielle : seuls les champs non nuls de l'intention sont appliques.
     */
    TraitementVisiteModel modifier(UUID visiteId, Long traitementId, TraitementVisiteModel intention);

    void supprimer(UUID visiteId, Long traitementId);
}
