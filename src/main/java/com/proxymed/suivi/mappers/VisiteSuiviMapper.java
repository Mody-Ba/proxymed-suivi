package com.proxymed.suivi.mappers;

import com.proxymed.suivi.service.model.MedecinModel;
import com.proxymed.suivi.service.model.PatientModel;
import com.proxymed.suivi.service.model.VisiteEnAttenteVisaResponse;
import com.proxymed.suivi.service.model.VisiteSuiviCreateRequest;
import com.proxymed.suivi.service.model.VisiteSuiviModel;
import com.proxymed.suivi.service.model.VisiteSuiviResponse;
import com.proxymed.suivi.service.model.VisiteSuiviSummaryResponse;
import com.proxymed.suivi.service.model.VisiteSuiviUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper API : convertit uniquement VisiteSuiviCreateRequest/UpdateRequest -> VisiteSuiviModel
 * et VisiteSuiviModel -> VisiteSuiviResponse/VisiteSuiviSummaryResponse.
 * Ne doit jamais connaitre l'entite JPA VisiteSuivi.
 *
 * Pour une intention de creation ou de mise a jour, le patient et les medecins ne portent
 * que l'id dans le modele produit ici : c'est au service de les resoudre et de les valider.
 */
@Component("apiVisiteSuiviMapper")
@RequiredArgsConstructor
public class VisiteSuiviMapper {

    private final PatientMapper patientMapper;
    private final MedecinMapper medecinMapper;
    private final ConstanteVisiteMapper constanteVisiteMapper;
    private final TraitementVisiteMapper traitementVisiteMapper;

    public VisiteSuiviModel toModel(UUID patientId, VisiteSuiviCreateRequest req) {
        return VisiteSuiviModel.builder()
                .patient(PatientModel.builder().id(patientId).build())
                .medecinJunior(medecinParId(req.medecinJuniorId()))
                .medecinSenior(medecinParId(req.medecinSeniorId()))
                .dateVisite(req.dateVisite())
                .heureArrivee(req.heureArrivee())
                .heureDepart(req.heureDepart())
                .adresseVisite(req.adresseVisite())
                .contactSenior(req.contactSenior())
                .contactSeniorJoignable(req.contactSeniorJoignable())
                .typeVisite(req.typeVisite())
                .build();
    }

    public VisiteSuiviModel toModel(VisiteSuiviUpdateRequest req) {
        return VisiteSuiviModel.builder()
                .medecinJunior(medecinParId(req.medecinJuniorId()))
                .medecinSenior(medecinParId(req.medecinSeniorId()))
                .dateVisite(req.dateVisite())
                .heureArrivee(req.heureArrivee())
                .heureDepart(req.heureDepart())
                .adresseVisite(req.adresseVisite())
                .contactSenior(req.contactSenior())
                .contactSeniorJoignable(req.contactSeniorJoignable())
                .typeVisite(req.typeVisite())
                .signesAlerteCliniques(req.signesAlerteCliniques())
                .heureAppelSenior(req.heureAppelSenior())
                .heureReponseSenior(req.heureReponseSenior())
                .decisionAppelSenior(req.decisionAppelSenior())
                .etatGeneral(req.etatGeneral())
                .etatGeneralPrecision(req.etatGeneralPrecision())
                .plaintePrincipale(req.plaintePrincipale())
                .evenementsPrecision(req.evenementsPrecision())
                .examenCardiovasculaire(req.examenCardiovasculaire())
                .examenRespiratoire(req.examenRespiratoire())
                .examenNeurologique(req.examenNeurologique())
                .examenDigestifAutre(req.examenDigestifAutre())
                .examenCutane(req.examenCutane())
                .observanceTraitement(req.observanceTraitement())
                .raisonsNonObservance(req.raisonsNonObservance())
                .themesAutresPrecision(req.themesAutresPrecision())
                .comprehensionPatient(req.comprehensionPatient())
                .statutPlanPrevention(req.statutPlanPrevention())
                .situationSociale(req.situationSociale())
                .problemesSociaux(req.problemesSociaux())
                .signalementDass(req.signalementDass())
                .heureContactDass(req.heureContactDass())
                .decision(req.decision())
                .resumeClinique(req.resumeClinique())
                .dateEnvoiCompteRendu(req.dateEnvoiCompteRendu())
                .moyenTransmission(req.moyenTransmission())
                .prochaineVisiteDate(req.prochaineVisiteDate())
                .evenementsIntercurrents(req.evenementsIntercurrents())
                .themesAbordes(req.themesAbordes())
                .dateSaisieDmi(req.dateSaisieDmi())
                .saisieDmiPar(req.saisieDmiPar())
                .build();
    }

    public VisiteSuiviResponse toResponse(VisiteSuiviModel model) {
        return VisiteSuiviResponse.builder()
                .id(model.id())
                .numeroVisite(model.numeroVisite())
                .statut(model.statut())
                .patient(patientMapper.toResponse(model.patient()))
                .dateVisite(model.dateVisite())
                .heureArrivee(model.heureArrivee())
                .heureDepart(model.heureDepart())
                .adresseVisite(model.adresseVisite())
                .medecinJunior(medecinMapper.toResponse(model.medecinJunior()))
                .medecinSenior(medecinMapper.toResponse(model.medecinSenior()))
                .contactSenior(model.contactSenior())
                .contactSeniorJoignable(model.contactSeniorJoignable())
                .typeVisite(model.typeVisite())
                .alerteDeclenchee(Boolean.TRUE.equals(model.alerteDeclenchee()))
                .alertes(constanteVisiteMapper.toAlerteResponses(model.alertes()))
                .grilleConstantes(constanteVisiteMapper.toGrilleResponse(model.grilleConstantes()))
                .signesAlerteCliniques(model.signesAlerteCliniques())
                .heureAppelSenior(model.heureAppelSenior())
                .heureReponseSenior(model.heureReponseSenior())
                .decisionAppelSenior(model.decisionAppelSenior())
                .etatGeneral(model.etatGeneral())
                .etatGeneralPrecision(model.etatGeneralPrecision())
                .plaintePrincipale(model.plaintePrincipale())
                .evenementsPrecision(model.evenementsPrecision())
                .examenCardiovasculaire(model.examenCardiovasculaire())
                .examenRespiratoire(model.examenRespiratoire())
                .examenNeurologique(model.examenNeurologique())
                .examenDigestifAutre(model.examenDigestifAutre())
                .examenCutane(model.examenCutane())
                .observanceTraitement(model.observanceTraitement())
                .raisonsNonObservance(model.raisonsNonObservance())
                .themesAutresPrecision(model.themesAutresPrecision())
                .comprehensionPatient(model.comprehensionPatient())
                .statutPlanPrevention(model.statutPlanPrevention())
                .situationSociale(model.situationSociale())
                .problemesSociaux(model.problemesSociaux())
                .signalementDass(model.signalementDass())
                .heureContactDass(model.heureContactDass())
                .decision(model.decision())
                .resumeClinique(model.resumeClinique())
                .dateEnvoiCompteRendu(model.dateEnvoiCompteRendu())
                .moyenTransmission(model.moyenTransmission())
                .prochaineVisiteDate(model.prochaineVisiteDate())
                .evenementsIntercurrents(model.evenementsIntercurrents())
                .themesAbordes(model.themesAbordes())
                .traitements(traitementVisiteMapper.toResponseList(model.traitements()))
                .dateSignatureJunior(model.dateSignatureJunior())
                .dateVisaSenior(model.dateVisaSenior())
                .dateSignaturePatient(model.dateSignaturePatient())
                .signatairePatient(model.signatairePatient())
                .dateSaisieDmi(model.dateSaisieDmi())
                .saisieDmiPar(model.saisieDmiPar())
                .echeanceCompteRendu(model.echeanceCompteRendu())
                .compteRenduEnRetard(model.compteRenduEnRetard())
                .echeanceVisa(model.echeanceVisa())
                .visaEnRetard(model.visaEnRetard())
                .dateCreation(model.dateCreation())
                .dateMiseAJour(model.dateMiseAJour())
                .build();
    }

    public VisiteSuiviSummaryResponse toSummaryResponse(VisiteSuiviModel model) {
        return VisiteSuiviSummaryResponse.builder()
                .id(model.id())
                .numeroVisite(model.numeroVisite())
                .dateVisite(model.dateVisite())
                .typeVisite(model.typeVisite())
                .statut(model.statut())
                .medecinJuniorId(model.medecinJunior().id())
                .medecinJuniorNomComplet(nomComplet(model.medecinJunior()))
                .medecinSeniorId(model.medecinSenior().id())
                .medecinSeniorNomComplet(nomComplet(model.medecinSenior()))
                .build();
    }

    public VisiteEnAttenteVisaResponse toEnAttenteVisaResponse(VisiteSuiviModel model) {
        return VisiteEnAttenteVisaResponse.builder()
                .id(model.id())
                .numeroVisite(model.numeroVisite())
                .dateVisite(model.dateVisite())
                .patientId(model.patient().id())
                .patientNomComplet(model.patient().nomComplet())
                .numeroDossierProxymed(model.patient().numeroDossierProxymed())
                .medecinJuniorId(model.medecinJunior().id())
                .medecinJuniorNomComplet(nomComplet(model.medecinJunior()))
                .medecinSeniorId(model.medecinSenior().id())
                .medecinSeniorNomComplet(nomComplet(model.medecinSenior()))
                .decision(model.decision())
                .alerteDeclenchee(Boolean.TRUE.equals(model.alerteDeclenchee()))
                .dateSignatureJunior(model.dateSignatureJunior())
                .echeanceVisa(model.echeanceVisa())
                .visaEnRetard(Boolean.TRUE.equals(model.visaEnRetard()))
                .build();
    }

    private MedecinModel medecinParId(Long id) {
        return id != null ? MedecinModel.builder().id(id).build() : null;
    }

    private String nomComplet(MedecinModel medecin) {
        return medecin.prenom() + " " + medecin.nom();
    }
}
