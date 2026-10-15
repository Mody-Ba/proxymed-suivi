package com.proxymed.suivi.service;

import com.proxymed.suivi.service.model.FicheSigneeModel;

import java.util.UUID;

public interface FicheSigneeService {

    /**
     * Importe la fiche signee (PDF) et la rattache a la visite.
     *
     * @throws com.proxymed.suivi.exception.ResourceNotFoundException si la visite n'existe pas
     * @throws com.proxymed.suivi.exception.RegleGestionException si le fichier n'est pas un PDF
     *                                                             ou depasse 10 Mo
     */
    FicheSigneeModel importer(UUID visiteId, FicheSigneeModel model);

    /**
     * Renvoie la fiche signee la plus recente (par date d'import) de la visite.
     *
     * @throws com.proxymed.suivi.exception.ResourceNotFoundException si la visite n'existe pas
     *                                                                ou n'a aucune fiche importee
     */
    FicheSigneeModel telecharger(UUID visiteId);
}
