package com.proxymed.suivi.service;

import com.proxymed.suivi.service.model.FichesImprimeesModel;

import java.util.List;

public interface ImpressionFichesService {

    /**
     * Cree une visite vierge par numero de dossier PROXYMED puis genere un seul PDF combine, une
     * fiche par patient, dans l'ordre. Tout ou rien : si un numero est introuvable ou si la
     * generation du PDF echoue, aucune visite n'est enregistree. Chaque fiche ne porte que
     * l'identite du patient et le QR code de la visite.
     *
     * @throws com.proxymed.suivi.exception.RegleGestionException si un numero apparait plusieurs fois
     * @throws com.proxymed.suivi.exception.ResourceNotFoundException si un ou plusieurs numeros
     *                                                               sont inconnus (listes en details)
     */
    FichesImprimeesModel imprimer(List<String> numerosDossierProxymed);
}
