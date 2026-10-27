package com.proxymed.suivi.service;

import com.proxymed.suivi.service.model.FicheSigneeModel;

import java.util.List;
import java.util.UUID;

public interface FicheSigneeService {

    /**
     * Importe une fiche signee scannee (PDF) sans id : la visite et le patient sont deduits du
     * QR code imprime sur la fiche, cherche sur toutes les pages. Une visite n'a qu'une seule
     * fiche signee : un second import (meme fichier ou non) est refuse.
     *
     * @throws com.proxymed.suivi.exception.RegleGestionException si le fichier n'est pas un PDF,
     *         depasse 10 Mo ou est illisible, si le QR n'est pas un QR PROXYMED ou pas une
     *         FICHE_2_SUIVI, ou si le patient du QR n'est pas celui de la visite
     * @throws com.proxymed.suivi.exception.QrCodeIntrouvableException si aucun QR code n'est trouve
     * @throws com.proxymed.suivi.exception.ResourceNotFoundException si le patient ou la visite
     *         du QR n'existe pas
     * @throws com.proxymed.suivi.exception.FicheSigneeExistanteException si la visite a deja une
     *         fiche signee
     */
    FicheSigneeModel importer(FicheSigneeModel model);

    /**
     * Renvoie la fiche signee de la visite.
     *
     * @throws com.proxymed.suivi.exception.ResourceNotFoundException si la visite n'existe pas
     *                                                                ou n'a aucune fiche importee
     */
    FicheSigneeModel telecharger(UUID visiteId);

    /**
     * Fiches signees du dossier du patient (metadonnees, sans contenu), de la visite la plus
     * recente a la plus ancienne.
     *
     * @throws com.proxymed.suivi.exception.ResourceNotFoundException si le patient n'existe pas
     */
    List<FicheSigneeModel> findByPatient(UUID patientId);
}
