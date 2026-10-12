package com.proxymed.suivi.mappers;

import com.proxymed.suivi.service.model.MedecinModel;
import com.proxymed.suivi.service.model.PatientModel;
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

    private MedecinModel medecinParId(Long id) {
        return id != null ? MedecinModel.builder().id(id).build() : null;
    }

    private String nomComplet(MedecinModel medecin) {
        return medecin.prenom() + " " + medecin.nom();
    }
}
