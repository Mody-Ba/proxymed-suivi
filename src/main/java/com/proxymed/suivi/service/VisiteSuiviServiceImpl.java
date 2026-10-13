package com.proxymed.suivi.service;

import com.proxymed.suivi.entity.VisiteSuivi;
import com.proxymed.suivi.enums.EvenementIntercurrent;
import com.proxymed.suivi.enums.RoleMedecin;
import com.proxymed.suivi.enums.SignatairePatient;
import com.proxymed.suivi.enums.SigneAlerteClinique;
import com.proxymed.suivi.enums.StatutVisite;
import com.proxymed.suivi.enums.TypeConstante;
import com.proxymed.suivi.exception.ConflitEtatException;
import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.exception.ResourceNotFoundException;
import com.proxymed.suivi.repository.VisiteSuiviRepository;
import com.proxymed.suivi.service.mappers.VisiteSuiviMapper;
import com.proxymed.suivi.service.model.AlerteModel;
import com.proxymed.suivi.service.model.ConstanteVisiteModel;
import com.proxymed.suivi.service.model.LigneGrilleConstantesModel;
import com.proxymed.suivi.service.model.MedecinModel;
import com.proxymed.suivi.service.model.TraitementVisiteModel;
import com.proxymed.suivi.service.model.VisiteSuiviModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Travaille avec VisiteSuiviModel comme type de travail. La seule entite JPA visible dans
 * cette classe est VisiteSuivi (l'aggregat propre a ce service) : chargee par
 * VisiteSuiviRepository, convertie en Model immediatement, et reconvertie en Entity
 * (via visiteSuiviMapper) juste avant repository.save(...).
 * Les entites des autres aggregats (Patient, Medecin) ne sont JAMAIS manipulees ici : leur
 * existence et les regles metier associees (role du medecin) sont verifiees via les Model
 * renvoyes par PatientService/MedecinService, et le rattachement JPA (traduction d'un id en
 * reference) est delegue au mapper DB.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class VisiteSuiviServiceImpl implements VisiteSuiviService {

    /**
     * Regle 11 (delais informatifs, non bloquants).
     */
    static final Duration DELAI_COMPTE_RENDU = Duration.ofHours(12);
    static final Duration DELAI_VISA = Duration.ofHours(24);

    private final VisiteSuiviRepository visiteSuiviRepository;
    private final PatientService patientService;
    private final MedecinService medecinService;
    private final VisiteSuiviMapper visiteSuiviMapper;

    @Override
    public VisiteSuiviModel findById(UUID id) {
        return toModel(getEntityById(id));
    }

    @Override
    public List<VisiteSuiviModel> findByPatient(UUID patientId) {
        patientService.findById(patientId);
        return visiteSuiviRepository.findByPatientIdOrderByNumeroVisiteDesc(patientId)
                .stream().map(this::toModel).toList();
    }

    @Override
    public VisiteSuiviModel creerBrouillon(VisiteSuiviModel intention) {
        UUID patientId = intention.patient().id();
        patientService.findById(patientId);
        verifierRole(intention.medecinJunior().id(), RoleMedecin.JUNIOR);
        verifierRole(intention.medecinSenior().id(), RoleMedecin.SENIOR);

        Instant maintenant = Instant.now();
        VisiteSuiviModel avecDefauts = intention.toBuilder()
                .numeroVisite(prochainNumeroVisite(patientId))
                .statut(StatutVisite.BROUILLON)
                .dateVisite(intention.dateVisite() != null ? intention.dateVisite() : LocalDate.now())
                .heureArrivee(intention.heureArrivee() != null
                        ? intention.heureArrivee() : LocalTime.now().truncatedTo(ChronoUnit.MINUTES))
                .dateCreation(maintenant)
                .dateMiseAJour(maintenant)
                .build();
        VisiteSuivi entity = visiteSuiviMapper.toNewEntity(avecDefauts);
        return toModel(visiteSuiviRepository.save(entity));
    }

    @Override
    public VisiteSuiviModel mettreAJour(UUID id, VisiteSuiviModel intention) {
        VisiteSuivi entity = getEntityById(id);
        VisiteSuiviModel courant = toModel(entity);
        verifierModifiable(courant);
        verifierEvenementsIntercurrents(intention.evenementsIntercurrents());

        if (intention.medecinJunior() != null) {
            MedecinModel junior = verifierRole(intention.medecinJunior().id(), RoleMedecin.JUNIOR);
            visiteSuiviMapper.appliquerMedecinJunior(entity, junior.id());
        }
        if (intention.medecinSenior() != null) {
            MedecinModel senior = verifierRole(intention.medecinSenior().id(), RoleMedecin.SENIOR);
            visiteSuiviMapper.appliquerMedecinSenior(entity, senior.id());
        }
        if (intention.signesAlerteCliniques() != null) {
            visiteSuiviMapper.appliquerSignesAlerteCliniques(entity, intention.signesAlerteCliniques());
        }
        if (intention.evenementsIntercurrents() != null) {
            visiteSuiviMapper.appliquerEvenementsIntercurrents(entity, intention.evenementsIntercurrents());
        }
        if (intention.themesAbordes() != null) {
            visiteSuiviMapper.appliquerThemesAbordes(entity, intention.themesAbordes());
        }

        VisiteSuiviModel fusionne = fusionnerChampsScalaires(courant, intention).toBuilder()
                .dateMiseAJour(Instant.now())
                .build();
        visiteSuiviMapper.applyScalarFieldsToEntity(entity, fusionne);
        return toModel(visiteSuiviRepository.save(entity));
    }

    @Override
    public VisiteSuiviModel terminer(UUID id) {
        VisiteSuivi entity = getEntityById(id);
        VisiteSuiviModel courant = toModel(entity);
        if (courant.statut() != StatutVisite.BROUILLON) {
            throw new ConflitEtatException(
                    "Seule une fiche en BROUILLON peut etre terminee (statut actuel : " + courant.statut() + ")");
        }
        List<String> manquements = ControleFicheTerminee.manquements(courant);
        if (!manquements.isEmpty()) {
            throw new RegleGestionException(
                    "La fiche ne peut pas etre terminee : " + manquements.size() + " element(s) requis manquant(s)",
                    manquements);
        }

        Instant maintenant = Instant.now();
        return enregistrer(entity, courant.toBuilder()
                .statut(StatutVisite.TERMINEE)
                .dateSignatureJunior(maintenant)
                .dateMiseAJour(maintenant)
                .build());
    }

    @Override
    public VisiteSuiviModel viser(UUID id) {
        VisiteSuivi entity = getEntityById(id);
        VisiteSuiviModel courant = toModel(entity);
        if (courant.statut() != StatutVisite.TERMINEE) {
            throw new ConflitEtatException(
                    "Seule une fiche TERMINEE peut recevoir le visa du senior (statut actuel : " + courant.statut() + ")");
        }

        Instant maintenant = Instant.now();
        return enregistrer(entity, courant.toBuilder()
                .statut(StatutVisite.VISEE)
                .dateVisaSenior(maintenant)
                .dateMiseAJour(maintenant)
                .build());
    }

    @Override
    public VisiteSuiviModel signerPatient(UUID id, SignatairePatient signataire) {
        VisiteSuivi entity = getEntityById(id);
        VisiteSuiviModel courant = toModel(entity);
        if (courant.statut() == StatutVisite.BROUILLON) {
            throw new ConflitEtatException(
                    "La signature du patient s'enregistre sur une fiche TERMINEE ou VISEE (statut actuel : BROUILLON)");
        }
        if (courant.dateSignaturePatient() != null) {
            throw new ConflitEtatException("La signature du patient est deja enregistree");
        }

        Instant maintenant = Instant.now();
        return enregistrer(entity, courant.toBuilder()
                .dateSignaturePatient(maintenant)
                .signatairePatient(signataire)
                .dateMiseAJour(maintenant)
                .build());
    }

    @Override
    public List<VisiteSuiviModel> findEnAttenteVisa() {
        return visiteSuiviRepository.findByStatutOrderByDateSignatureJuniorAsc(StatutVisite.TERMINEE)
                .stream().map(this::toModel).toList();
    }

    @Override
    public void verifierModifiable(UUID id) {
        verifierModifiable(findById(id));
    }

    /**
     * Seul point d'acces a l'entite JPA dans ce service : prive, jamais expose aux
     * controllers ni aux autres services.
     */
    private VisiteSuivi getEntityById(UUID id) {
        return visiteSuiviRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Visite introuvable : " + id));
    }

    private VisiteSuiviModel enregistrer(VisiteSuivi entity, VisiteSuiviModel model) {
        visiteSuiviMapper.applyScalarFieldsToEntity(entity, model);
        return toModel(visiteSuiviRepository.save(entity));
    }

    private VisiteSuiviModel toModel(VisiteSuivi entity) {
        VisiteSuiviModel model = visiteSuiviMapper.toModel(entity);
        List<ConstanteVisiteModel> constantes = model.constantes().stream().map(SeuilsAlerte::appliquer).toList();
        List<AlerteModel> alertes = calculerAlertes(constantes, model.signesAlerteCliniques());
        return model.toBuilder()
                .patient(patientService.completerAge(model.patient()))
                .constantes(constantes)
                .grilleConstantes(construireGrille(constantes))
                .alerteDeclenchee(!alertes.isEmpty())
                .alertes(alertes)
                .traitements(model.traitements().stream()
                        .sorted(Comparator.comparing(TraitementVisiteModel::id))
                        .toList())
                .echeanceCompteRendu(echeanceCompteRendu(model))
                .compteRenduEnRetard(compteRenduEnRetard(model))
                .echeanceVisa(echeanceVisa(model))
                .visaEnRetard(visaEnRetard(model))
                .build();
    }

    /**
     * Regle 11 : le resume clinique doit partir au senior dans les 12 h qui suivent la fin de
     * la visite (date de la visite + heure de depart). Null tant que l'heure de depart manque.
     */
    private LocalDateTime echeanceCompteRendu(VisiteSuiviModel model) {
        if (model.dateVisite() == null || model.heureDepart() == null) {
            return null;
        }
        return LocalDateTime.of(model.dateVisite(), model.heureDepart()).plus(DELAI_COMPTE_RENDU);
    }

    private Boolean compteRenduEnRetard(VisiteSuiviModel model) {
        LocalDateTime echeance = echeanceCompteRendu(model);
        if (echeance == null) {
            return null;
        }
        LocalDateTime reference = model.dateEnvoiCompteRendu() != null ? model.dateEnvoiCompteRendu() : LocalDateTime.now();
        return reference.isAfter(echeance);
    }

    /**
     * Regle 11 : visa du senior dans les 24 h qui suivent la signature du junior.
     * Null tant que la fiche n'est pas terminee.
     */
    private Instant echeanceVisa(VisiteSuiviModel model) {
        return model.dateSignatureJunior() != null ? model.dateSignatureJunior().plus(DELAI_VISA) : null;
    }

    private Boolean visaEnRetard(VisiteSuiviModel model) {
        Instant echeance = echeanceVisa(model);
        if (echeance == null) {
            return null;
        }
        Instant reference = model.dateVisaSenior() != null ? model.dateVisaSenior() : Instant.now();
        return reference.isAfter(echeance);
    }

    /**
     * Regle 5 : alertes recalculees a chaque lecture, jamais saisies ni stockees. Une
     * constante hors seuil ou un signe d'alerte clinique coche declenche une alerte.
     */
    private List<AlerteModel> calculerAlertes(List<ConstanteVisiteModel> constantes, List<SigneAlerteClinique> signes) {
        List<AlerteModel> alertes = new ArrayList<>();
        constantes.stream()
                .filter(c -> Boolean.TRUE.equals(c.alerte()))
                .sorted(Comparator.comparing(ConstanteVisiteModel::creneau).thenComparing(ConstanteVisiteModel::type))
                .forEach(c -> alertes.add(AlerteModel.builder()
                        .constante(c.type())
                        .creneau(c.creneau())
                        .valeur(c.valeur())
                        .valeurDiastolique(c.valeurDiastolique())
                        .seuilFranchi(c.seuilFranchi())
                        .build()));
        signes.forEach(signe -> alertes.add(AlerteModel.builder()
                .signeClinique(signe)
                .seuilFranchi(signe.getLibelle())
                .build()));
        return alertes;
    }

    /**
     * Grille de la section 3.1 : les 8 constantes dans l'ordre de la fiche (meme sans mesure),
     * chacune avec ses mesures triees par creneau (H0 a H+4).
     */
    private List<LigneGrilleConstantesModel> construireGrille(List<ConstanteVisiteModel> constantes) {
        return Arrays.stream(TypeConstante.values())
                .map(type -> LigneGrilleConstantesModel.builder()
                        .type(type)
                        .mesures(constantes.stream()
                                .filter(c -> c.type() == type)
                                .sorted(Comparator.comparing(ConstanteVisiteModel::creneau))
                                .toList())
                        .build())
                .toList();
    }

    /**
     * Regle 1 : numero de visite = nombre de visites deja enregistrees pour ce patient + 1.
     */
    private int prochainNumeroVisite(UUID patientId) {
        return Math.toIntExact(visiteSuiviRepository.countByPatientId(patientId)) + 1;
    }

    /**
     * Regle 3 : le medecin doit exister (404 via MedecinService) et avoir le role attendu.
     */
    private MedecinModel verifierRole(Long medecinId, RoleMedecin roleAttendu) {
        MedecinModel medecin = medecinService.findById(medecinId);
        if (medecin.role() != roleAttendu) {
            throw new RegleGestionException("Le medecin " + roleAttendu.name().toLowerCase()
                    + " doit avoir le role " + roleAttendu + " (medecin " + medecin.id() + ")");
        }
        return medecin;
    }

    /**
     * Regle 4 : modification possible seulement en BROUILLON.
     */
    private void verifierModifiable(VisiteSuiviModel model) {
        if (model.statut() != StatutVisite.BROUILLON) {
            throw new ConflitEtatException(
                    "La fiche n'est plus modifiable (statut actuel : " + model.statut() + ")");
        }
    }

    /**
     * "Aucun" ne peut pas etre coche en meme temps qu'un evenement intercurrent.
     */
    private void verifierEvenementsIntercurrents(List<EvenementIntercurrent> evenements) {
        if (evenements != null && evenements.contains(EvenementIntercurrent.AUCUN)
                && evenements.stream().distinct().count() > 1) {
            throw new RegleGestionException(
                    "L'evenement intercurrent AUCUN ne peut pas etre coche avec d'autres evenements");
        }
    }

    private VisiteSuiviModel fusionnerChampsScalaires(VisiteSuiviModel courant, VisiteSuiviModel maj) {
        return courant.toBuilder()
                .dateVisite(coalesce(maj.dateVisite(), courant.dateVisite()))
                .heureArrivee(coalesce(maj.heureArrivee(), courant.heureArrivee()))
                .heureDepart(coalesce(maj.heureDepart(), courant.heureDepart()))
                .adresseVisite(coalesce(maj.adresseVisite(), courant.adresseVisite()))
                .contactSenior(coalesce(maj.contactSenior(), courant.contactSenior()))
                .contactSeniorJoignable(coalesce(maj.contactSeniorJoignable(), courant.contactSeniorJoignable()))
                .typeVisite(coalesce(maj.typeVisite(), courant.typeVisite()))
                .heureAppelSenior(coalesce(maj.heureAppelSenior(), courant.heureAppelSenior()))
                .heureReponseSenior(coalesce(maj.heureReponseSenior(), courant.heureReponseSenior()))
                .decisionAppelSenior(coalesce(maj.decisionAppelSenior(), courant.decisionAppelSenior()))
                .etatGeneral(coalesce(maj.etatGeneral(), courant.etatGeneral()))
                .etatGeneralPrecision(coalesce(maj.etatGeneralPrecision(), courant.etatGeneralPrecision()))
                .plaintePrincipale(coalesce(maj.plaintePrincipale(), courant.plaintePrincipale()))
                .evenementsPrecision(coalesce(maj.evenementsPrecision(), courant.evenementsPrecision()))
                .examenCardiovasculaire(coalesce(maj.examenCardiovasculaire(), courant.examenCardiovasculaire()))
                .examenRespiratoire(coalesce(maj.examenRespiratoire(), courant.examenRespiratoire()))
                .examenNeurologique(coalesce(maj.examenNeurologique(), courant.examenNeurologique()))
                .examenDigestifAutre(coalesce(maj.examenDigestifAutre(), courant.examenDigestifAutre()))
                .examenCutane(coalesce(maj.examenCutane(), courant.examenCutane()))
                .observanceTraitement(coalesce(maj.observanceTraitement(), courant.observanceTraitement()))
                .raisonsNonObservance(coalesce(maj.raisonsNonObservance(), courant.raisonsNonObservance()))
                .themesAutresPrecision(coalesce(maj.themesAutresPrecision(), courant.themesAutresPrecision()))
                .comprehensionPatient(coalesce(maj.comprehensionPatient(), courant.comprehensionPatient()))
                .statutPlanPrevention(coalesce(maj.statutPlanPrevention(), courant.statutPlanPrevention()))
                .situationSociale(coalesce(maj.situationSociale(), courant.situationSociale()))
                .problemesSociaux(coalesce(maj.problemesSociaux(), courant.problemesSociaux()))
                .signalementDass(coalesce(maj.signalementDass(), courant.signalementDass()))
                .heureContactDass(coalesce(maj.heureContactDass(), courant.heureContactDass()))
                .decision(coalesce(maj.decision(), courant.decision()))
                .resumeClinique(coalesce(maj.resumeClinique(), courant.resumeClinique()))
                .dateEnvoiCompteRendu(coalesce(maj.dateEnvoiCompteRendu(), courant.dateEnvoiCompteRendu()))
                .moyenTransmission(coalesce(maj.moyenTransmission(), courant.moyenTransmission()))
                .prochaineVisiteDate(coalesce(maj.prochaineVisiteDate(), courant.prochaineVisiteDate()))
                .dateSaisieDmi(coalesce(maj.dateSaisieDmi(), courant.dateSaisieDmi()))
                .saisieDmiPar(coalesce(maj.saisieDmiPar(), courant.saisieDmiPar()))
                .build();
    }

    private static <T> T coalesce(T nouvelle, T actuelle) {
        return nouvelle != null ? nouvelle : actuelle;
    }
}
