package com.proxymed.suivi.service.mappers;

import com.proxymed.suivi.entity.FicheSignee;
import com.proxymed.suivi.entity.VisiteSuivi;
import com.proxymed.suivi.repository.FicheSigneeResume;
import com.proxymed.suivi.repository.VisiteSuiviRepository;
import com.proxymed.suivi.service.model.FicheSigneeModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper DB : convertit uniquement entre FicheSigneeModel et l'entite JPA FicheSignee.
 * Ne doit jamais connaitre le fichier multipart ni FicheSigneeResponse.
 * Resout lui-meme la reference JPA vers la visite parente (getReferenceById, simple
 * traduction d'un id en relation) : le service qui l'appelle ne manipule ainsi jamais
 * l'entite VisiteSuivi, seulement son id.
 */
@Component("dbFicheSigneeMapper")
@RequiredArgsConstructor
public class FicheSigneeMapper {

    private final VisiteSuiviRepository visiteSuiviRepository;

    public FicheSignee toEntity(FicheSigneeModel model, UUID visiteId) {
        return FicheSignee.builder()
                .visite(visiteSuiviRepository.getReferenceById(visiteId))
                .nomFichier(model.nomFichier())
                .typeContenu(model.typeContenu())
                .tailleOctets(model.tailleOctets())
                .dateImport(model.dateImport())
                .contenu(model.contenu())
                .build();
    }

    public FicheSigneeModel toModel(FicheSignee entity) {
        VisiteSuivi visite = entity.getVisite();
        return FicheSigneeModel.builder()
                .id(entity.getId())
                .visiteId(visite.getId())
                .patientId(visite.getPatient().getId())
                .numeroVisite(visite.getNumeroVisite())
                .nomFichier(entity.getNomFichier())
                .typeContenu(entity.getTypeContenu())
                .tailleOctets(entity.getTailleOctets())
                .dateImport(entity.getDateImport())
                .contenu(entity.getContenu())
                .build();
    }

    public FicheSigneeModel toModel(FicheSigneeResume resume) {
        return FicheSigneeModel.builder()
                .id(resume.getId())
                .visiteId(resume.getVisiteId())
                .patientId(resume.getPatientId())
                .numeroVisite(resume.getNumeroVisite())
                .nomFichier(resume.getNomFichier())
                .typeContenu(resume.getTypeContenu())
                .tailleOctets(resume.getTailleOctets())
                .dateImport(resume.getDateImport())
                .build();
    }
}
