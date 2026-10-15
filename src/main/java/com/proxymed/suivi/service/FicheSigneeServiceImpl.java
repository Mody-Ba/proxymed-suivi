package com.proxymed.suivi.service;

import com.proxymed.suivi.entity.FicheSignee;
import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.exception.ResourceNotFoundException;
import com.proxymed.suivi.repository.FicheSigneeRepository;
import com.proxymed.suivi.service.mappers.FicheSigneeMapper;
import com.proxymed.suivi.service.model.FicheSigneeModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/**
 * La seule entite JPA visible dans cette classe est FicheSignee (l'aggregat propre a ce
 * service). VisiteSuivi (aggregat parent) n'est jamais manipulee ici : la verification
 * qu'elle existe passe par VisiteSuiviService.findById, et le rattachement JPA
 * (id -> reference) est delegue au mapper DB.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FicheSigneeServiceImpl implements FicheSigneeService {

    static final long TAILLE_MAX_OCTETS = 10L * 1024 * 1024;
    static final String NOM_FICHIER_PAR_DEFAUT = "fiche-signee.pdf";
    private static final String TYPE_PDF = "application/pdf";
    private static final byte[] SIGNATURE_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final FicheSigneeRepository ficheSigneeRepository;
    private final VisiteSuiviService visiteSuiviService;
    private final FicheSigneeMapper ficheSigneeMapper;

    @Override
    public FicheSigneeModel importer(UUID visiteId, FicheSigneeModel model) {
        visiteSuiviService.findById(visiteId);
        if (model.contenu() != null && model.contenu().length > TAILLE_MAX_OCTETS) {
            throw new RegleGestionException("Le fichier importe depasse la taille maximale de 10 Mo");
        }
        if (!estPdf(model)) {
            throw new RegleGestionException("Le fichier importe doit etre un PDF");
        }
        FicheSigneeModel aEnregistrer = model.toBuilder()
                .nomFichier(nomFichier(model.nomFichier()))
                .tailleOctets(model.contenu().length)
                .dateImport(Instant.now())
                .build();
        FicheSignee entity = ficheSigneeRepository.save(ficheSigneeMapper.toEntity(aEnregistrer, visiteId));
        return ficheSigneeMapper.toModel(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public FicheSigneeModel telecharger(UUID visiteId) {
        visiteSuiviService.findById(visiteId);
        return ficheSigneeRepository.findFirstByVisite_IdOrderByDateImportDesc(visiteId)
                .map(ficheSigneeMapper::toModel)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune fiche signee importee pour la visite : " + visiteId));
    }

    /**
     * Le type de contenu declare par le client ne suffit pas (il est librement falsifiable) :
     * on exige en plus que le fichier commence par la signature d'un PDF.
     */
    private boolean estPdf(FicheSigneeModel model) {
        byte[] contenu = model.contenu();
        return TYPE_PDF.equalsIgnoreCase(model.typeContenu())
                && contenu != null
                && contenu.length >= SIGNATURE_PDF.length
                && Arrays.equals(contenu, 0, SIGNATURE_PDF.length, SIGNATURE_PDF, 0, SIGNATURE_PDF.length);
    }

    /**
     * Nom du fichier sans chemin (certains navigateurs envoient le chemin complet), avec un
     * nom par defaut si le client n'en fournit pas.
     */
    private String nomFichier(String nomRecu) {
        if (nomRecu == null || nomRecu.isBlank()) {
            return NOM_FICHIER_PAR_DEFAUT;
        }
        String nom = nomRecu.substring(Math.max(nomRecu.lastIndexOf('/'), nomRecu.lastIndexOf('\\')) + 1).strip();
        if (nom.isEmpty()) {
            return NOM_FICHIER_PAR_DEFAUT;
        }
        return nom.length() > 255 ? nom.substring(nom.length() - 255) : nom;
    }
}
