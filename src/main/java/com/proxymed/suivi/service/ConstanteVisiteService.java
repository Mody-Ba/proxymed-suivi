package com.proxymed.suivi.service;

import com.proxymed.suivi.service.model.ConstanteVisiteModel;
import com.proxymed.suivi.service.model.LigneGrilleConstantesModel;

import java.util.List;
import java.util.UUID;

public interface ConstanteVisiteService {

    /**
     * Enregistre une mesure de la grille ; remplace celle qui existe deja pour le meme type
     * et le meme creneau.
     */
    ConstanteVisiteModel enregistrer(UUID visiteId, ConstanteVisiteModel model);

    /**
     * Grille complete (les 8 constantes de la fiche, avec ou sans mesure), alertes calculees.
     */
    List<LigneGrilleConstantesModel> lireGrille(UUID visiteId);

    void supprimer(UUID visiteId, Long constanteId);
}
