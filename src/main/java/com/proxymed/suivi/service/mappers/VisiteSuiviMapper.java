package com.proxymed.suivi.service.mappers;

import com.proxymed.suivi.entity.VisiteSuivi;
import com.proxymed.suivi.enums.EvenementIntercurrent;
import com.proxymed.suivi.enums.SigneAlerteClinique;
import com.proxymed.suivi.enums.ThemeEducation;
import com.proxymed.suivi.repository.MedecinRepository;
import com.proxymed.suivi.repository.PatientRepository;
import com.proxymed.suivi.service.model.VisiteSuiviModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper DB : convertit uniquement entre VisiteSuiviModel et l'entite JPA VisiteSuivi.
 * Ne doit jamais connaitre VisiteSuiviCreateRequest/UpdateRequest/Response.
 *
 * Resout lui-meme les references JPA vers les autres aggregats (patient, medecins) via
 * getReferenceById sur leurs repositories : simple traduction d'un id en relation, jamais
 * un appel metier. Le service appelant ne manipule ainsi jamais ces entites, seulement
 * leurs id (ou les Model renvoyes par PatientService/MedecinService, deja valides en amont).
 */
@Component("dbVisiteSuiviMapper")
@RequiredArgsConstructor
public class VisiteSuiviMapper {

    private final PatientRepository patientRepository;
    private final MedecinRepository medecinRepository;
    private final PatientMapper patientMapper;
    private final MedecinMapper medecinMapper;
    private final ConstanteVisiteMapper constanteVisiteMapper;
    private final TraitementVisiteMapper traitementVisiteMapper;

    public VisiteSuivi toNewEntity(VisiteSuiviModel model) {
        return VisiteSuivi.builder()
                .patient(patientRepository.getReferenceById(model.patient().id()))
                .numeroVisite(model.numeroVisite())
                .dateVisite(model.dateVisite())
                .heureArrivee(model.heureArrivee())
                .heureDepart(model.heureDepart())
                .adresseVisite(model.adresseVisite())
                .medecinJunior(medecinRepository.getReferenceById(model.medecinJunior().id()))
                .medecinSenior(medecinRepository.getReferenceById(model.medecinSenior().id()))
                .contactSenior(model.contactSenior())
                .contactSeniorJoignable(model.contactSeniorJoignable())
                .typeVisite(model.typeVisite())
                .statut(model.statut())
                .dateCreation(model.dateCreation())
                .dateMiseAJour(model.dateMiseAJour())
                .build();
    }

    /**
     * Applique les champs scalaires (hors relations) du modele sur une entite managee
     * existante (mutation en place, necessaire pour qu'Hibernate suive les changements).
     * Les medecins et les listes a choix multiples se changent via appliquerMedecinJunior/
     * appliquerMedecinSenior/appliquerSignesAlerteCliniques/appliquerEvenementsIntercurrents/
     * appliquerThemesAbordes, pour ne les toucher que quand le service decide explicitement
     * de les modifier.
     * Le patient, le numero de visite et la date de creation ne changent jamais.
     */
    public void applyScalarFieldsToEntity(VisiteSuivi entity, VisiteSuiviModel model) {
        entity.setDateVisite(model.dateVisite());
        entity.setHeureArrivee(model.heureArrivee());
        entity.setHeureDepart(model.heureDepart());
        entity.setAdresseVisite(model.adresseVisite());
        entity.setContactSenior(model.contactSenior());
        entity.setContactSeniorJoignable(model.contactSeniorJoignable());
        entity.setTypeVisite(model.typeVisite());
        entity.setHeureAppelSenior(model.heureAppelSenior());
        entity.setHeureReponseSenior(model.heureReponseSenior());
        entity.setDecisionAppelSenior(model.decisionAppelSenior());
        entity.setEtatGeneral(model.etatGeneral());
        entity.setEtatGeneralPrecision(model.etatGeneralPrecision());
        entity.setPlaintePrincipale(model.plaintePrincipale());
        entity.setEvenementsPrecision(model.evenementsPrecision());
        entity.setExamenCardiovasculaire(model.examenCardiovasculaire());
        entity.setExamenRespiratoire(model.examenRespiratoire());
        entity.setExamenNeurologique(model.examenNeurologique());
        entity.setExamenDigestifAutre(model.examenDigestifAutre());
        entity.setExamenCutane(model.examenCutane());
        entity.setObservanceTraitement(model.observanceTraitement());
        entity.setRaisonsNonObservance(model.raisonsNonObservance());
        entity.setThemesAutresPrecision(model.themesAutresPrecision());
        entity.setComprehensionPatient(model.comprehensionPatient());
        entity.setStatutPlanPrevention(model.statutPlanPrevention());
        entity.setSituationSociale(model.situationSociale());
        entity.setProblemesSociaux(model.problemesSociaux());
        entity.setSignalementDass(model.signalementDass());
        entity.setHeureContactDass(model.heureContactDass());
        entity.setDecision(model.decision());
        entity.setResumeClinique(model.resumeClinique());
        entity.setDateEnvoiCompteRendu(model.dateEnvoiCompteRendu());
        entity.setMoyenTransmission(model.moyenTransmission());
        entity.setProchaineVisiteDate(model.prochaineVisiteDate());
        entity.setDateSignatureJunior(model.dateSignatureJunior());
        entity.setDateVisaSenior(model.dateVisaSenior());
        entity.setDateSignaturePatient(model.dateSignaturePatient());
        entity.setSignatairePatient(model.signatairePatient());
        entity.setDateSaisieDmi(model.dateSaisieDmi());
        entity.setSaisieDmiPar(model.saisieDmiPar());
        entity.setStatut(model.statut());
        entity.setDateMiseAJour(model.dateMiseAJour());
    }

    public void appliquerMedecinJunior(VisiteSuivi entity, Long medecinId) {
        entity.setMedecinJunior(medecinRepository.getReferenceById(medecinId));
    }

    public void appliquerMedecinSenior(VisiteSuivi entity, Long medecinId) {
        entity.setMedecinSenior(medecinRepository.getReferenceById(medecinId));
    }

    /**
     * Remplace toute la liste des signes d'alerte cliniques coches.
     */
    public void appliquerSignesAlerteCliniques(VisiteSuivi entity, List<SigneAlerteClinique> signes) {
        entity.getSignesAlerteCliniques().clear();
        entity.getSignesAlerteCliniques().addAll(signes);
    }

    public void appliquerEvenementsIntercurrents(VisiteSuivi entity, List<EvenementIntercurrent> evenements) {
        entity.getEvenementsIntercurrents().clear();
        entity.getEvenementsIntercurrents().addAll(evenements);
    }

    public void appliquerThemesAbordes(VisiteSuivi entity, List<ThemeEducation> themes) {
        entity.getThemesAbordes().clear();
        entity.getThemesAbordes().addAll(themes);
    }

    public VisiteSuiviModel toModel(VisiteSuivi entity) {
        return VisiteSuiviModel.builder()
                .id(entity.getId())
                .patient(patientMapper.toModel(entity.getPatient()))
                .numeroVisite(entity.getNumeroVisite())
                .dateVisite(entity.getDateVisite())
                .heureArrivee(entity.getHeureArrivee())
                .heureDepart(entity.getHeureDepart())
                .adresseVisite(entity.getAdresseVisite())
                .medecinJunior(medecinMapper.toModel(entity.getMedecinJunior()))
                .medecinSenior(medecinMapper.toModel(entity.getMedecinSenior()))
                .contactSenior(entity.getContactSenior())
                .contactSeniorJoignable(entity.getContactSeniorJoignable())
                .typeVisite(entity.getTypeVisite())
                .statut(entity.getStatut())
                .constantes(constanteVisiteMapper.toModelList(entity.getConstantes()))
                .signesAlerteCliniques(entity.getSignesAlerteCliniques().stream().sorted().toList())
                .heureAppelSenior(entity.getHeureAppelSenior())
                .heureReponseSenior(entity.getHeureReponseSenior())
                .decisionAppelSenior(entity.getDecisionAppelSenior())
                .etatGeneral(entity.getEtatGeneral())
                .etatGeneralPrecision(entity.getEtatGeneralPrecision())
                .plaintePrincipale(entity.getPlaintePrincipale())
                .evenementsPrecision(entity.getEvenementsPrecision())
                .examenCardiovasculaire(entity.getExamenCardiovasculaire())
                .examenRespiratoire(entity.getExamenRespiratoire())
                .examenNeurologique(entity.getExamenNeurologique())
                .examenDigestifAutre(entity.getExamenDigestifAutre())
                .examenCutane(entity.getExamenCutane())
                .observanceTraitement(entity.getObservanceTraitement())
                .raisonsNonObservance(entity.getRaisonsNonObservance())
                .themesAutresPrecision(entity.getThemesAutresPrecision())
                .comprehensionPatient(entity.getComprehensionPatient())
                .statutPlanPrevention(entity.getStatutPlanPrevention())
                .situationSociale(entity.getSituationSociale())
                .problemesSociaux(entity.getProblemesSociaux())
                .signalementDass(entity.getSignalementDass())
                .heureContactDass(entity.getHeureContactDass())
                .decision(entity.getDecision())
                .resumeClinique(entity.getResumeClinique())
                .dateEnvoiCompteRendu(entity.getDateEnvoiCompteRendu())
                .moyenTransmission(entity.getMoyenTransmission())
                .prochaineVisiteDate(entity.getProchaineVisiteDate())
                .dateSignatureJunior(entity.getDateSignatureJunior())
                .dateVisaSenior(entity.getDateVisaSenior())
                .dateSignaturePatient(entity.getDateSignaturePatient())
                .signatairePatient(entity.getSignatairePatient())
                .dateSaisieDmi(entity.getDateSaisieDmi())
                .saisieDmiPar(entity.getSaisieDmiPar())
                .evenementsIntercurrents(entity.getEvenementsIntercurrents().stream().sorted().toList())
                .themesAbordes(entity.getThemesAbordes().stream().sorted().toList())
                .traitements(traitementVisiteMapper.toModelList(entity.getTraitements()))
                .dateCreation(entity.getDateCreation())
                .dateMiseAJour(entity.getDateMiseAJour())
                .build();
    }
}
