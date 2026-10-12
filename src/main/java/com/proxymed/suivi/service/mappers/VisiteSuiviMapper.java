package com.proxymed.suivi.service.mappers;

import com.proxymed.suivi.entity.VisiteSuivi;
import com.proxymed.suivi.repository.MedecinRepository;
import com.proxymed.suivi.repository.PatientRepository;
import com.proxymed.suivi.service.model.VisiteSuiviModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
     * Les medecins se changent via appliquerMedecinJunior/appliquerMedecinSenior, pour ne
     * les toucher que quand le service decide explicitement de les modifier.
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
        entity.setStatut(model.statut());
        entity.setDateMiseAJour(model.dateMiseAJour());
    }

    public void appliquerMedecinJunior(VisiteSuivi entity, Long medecinId) {
        entity.setMedecinJunior(medecinRepository.getReferenceById(medecinId));
    }

    public void appliquerMedecinSenior(VisiteSuivi entity, Long medecinId) {
        entity.setMedecinSenior(medecinRepository.getReferenceById(medecinId));
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
                .dateCreation(entity.getDateCreation())
                .dateMiseAJour(entity.getDateMiseAJour())
                .build();
    }
}
