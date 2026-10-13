package com.proxymed.suivi.service;

import com.proxymed.suivi.enums.SignatairePatient;
import com.proxymed.suivi.service.model.VisiteSuiviModel;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.UUID;

public interface VisiteSuiviService {

    VisiteSuiviModel findById(UUID id);

    /**
     * Visites d'un patient, de la plus recente a la plus ancienne.
     */
    List<VisiteSuiviModel> findByPatient(UUID patientId);

    /**
     * Cree une visite en BROUILLON. Le numero de visite est calcule (cumul par patient).
     */
    VisiteSuiviModel creerBrouillon(VisiteSuiviModel intention);

    /**
     * Mise a jour partielle : seuls les champs non nuls de l'intention sont appliques.
     */
    VisiteSuiviModel mettreAJour(UUID id, VisiteSuiviModel intention);

    /**
     * Le junior termine et signe : BROUILLON -> TERMINEE, apres verification des champs requis.
     */
    VisiteSuiviModel terminer(UUID id);

    /**
     * Visa du senior : TERMINEE -> VISEE.
     */
    VisiteSuiviModel viser(UUID id);

    /**
     * Signature du patient ou de l'aidant, sur une fiche TERMINEE ou VISEE.
     */
    VisiteSuiviModel signerPatient(UUID id, SignatairePatient signataire);

    /**
     * Fiches terminees en attente du visa du senior, de la plus ancienne a la plus recente.
     */
    List<VisiteSuiviModel> findEnAttenteVisa();

    /**
     * PDF de la fiche (quel que soit son statut), avec le QR code de la visite.
     */
    Resource genererPdf(UUID id);

    /**
     * Verifie que la visite existe (404) et est encore en BROUILLON (409). Sert aux services
     * des elements rattaches a la visite (constantes, traitements) avant toute modification.
     */
    void verifierModifiable(UUID id);
}
