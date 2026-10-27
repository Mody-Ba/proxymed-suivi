package com.proxymed.suivi.service;

import com.proxymed.suivi.entity.FicheSignee;
import com.proxymed.suivi.exception.FicheSigneeExistanteException;
import com.proxymed.suivi.exception.QrCodeIntrouvableException;
import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.exception.ResourceNotFoundException;
import com.proxymed.suivi.repository.FicheSigneeRepository;
import com.proxymed.suivi.service.mappers.FicheSigneeMapper;
import com.proxymed.suivi.service.model.ContenuQrModel;
import com.proxymed.suivi.service.model.FicheSigneeModel;
import com.proxymed.suivi.service.model.VisiteSuiviModel;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * La seule entite JPA visible dans cette classe est FicheSignee (l'aggregat propre a ce
 * service). VisiteSuivi (aggregat parent) n'est jamais manipulee ici : la verification
 * qu'elle existe passe par VisiteSuiviService.findById, et le rattachement JPA
 * (id -> reference) est delegue au mapper DB.
 * A l'import, aucun id n'est fourni : la visite est lue dans le QR code du PDF scanne
 * (DetecteurQrPdf puis LecteurContenuQr), puis recoupee avec le patient du QR.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FicheSigneeServiceImpl implements FicheSigneeService {

    static final long TAILLE_MAX_OCTETS = 10L * 1024 * 1024;
    static final String NOM_FICHIER_PAR_DEFAUT = "fiche-signee.pdf";
    static final String MESSAGE_FICHE_EXISTANTE = "Une fiche signee existe deja pour cette visite";
    private static final String TYPE_PDF = "application/pdf";
    private static final byte[] SIGNATURE_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final FicheSigneeRepository ficheSigneeRepository;
    private final VisiteSuiviService visiteSuiviService;
    private final PatientService patientService;
    private final FicheSigneeMapper ficheSigneeMapper;
    private final DetecteurQrPdf detecteurQrPdf;
    private final LecteurContenuQr lecteurContenuQr;

    @Override
    public FicheSigneeModel importer(FicheSigneeModel model) {
        if (model.contenu() != null && model.contenu().length > TAILLE_MAX_OCTETS) {
            throw new RegleGestionException("Le fichier importe depasse la taille maximale de 10 Mo");
        }
        if (!estPdf(model)) {
            throw new RegleGestionException("Le fichier importe doit etre un PDF");
        }
        ContenuQrModel qr = lireQrCode(model.contenu());
        patientService.findById(qr.patientId());
        VisiteSuiviModel visite = visiteSuiviService.findById(qr.visiteId());
        if (!qr.patientId().equals(visite.patient().id())) {
            throw new RegleGestionException("Le patient du code QR (" + qr.patientId()
                    + ") ne correspond pas au patient de la visite " + qr.visiteId());
        }
        UUID visiteId = visite.id();
        if (ficheSigneeRepository.existsByVisite_Id(visiteId)) {
            throw new FicheSigneeExistanteException(MESSAGE_FICHE_EXISTANTE + " : " + visiteId);
        }
        FicheSigneeModel aEnregistrer = model.toBuilder()
                .nomFichier(nomFichier(model.nomFichier()))
                .tailleOctets(model.contenu().length)
                .dateImport(Instant.now())
                .build();
        try {
            // saveAndFlush : la contrainte d'unicite est verifiee ici, et non au commit, pour que
            // l'import simultane perdant recoive lui aussi un 409.
            FicheSignee entity = ficheSigneeRepository.saveAndFlush(ficheSigneeMapper.toEntity(aEnregistrer, visiteId));
            return ficheSigneeMapper.toModel(entity);
        } catch (DataIntegrityViolationException e) {
            // Seule contrainte possible ici : uk_fiche_signee_visite (la visite existe, verifiee plus haut).
            throw new FicheSigneeExistanteException(MESSAGE_FICHE_EXISTANTE + " : " + visiteId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FicheSigneeModel telecharger(UUID visiteId) {
        visiteSuiviService.findById(visiteId);
        return ficheSigneeRepository.findByVisite_Id(visiteId)
                .map(ficheSigneeMapper::toModel)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune fiche signee importee pour la visite : " + visiteId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FicheSigneeModel> findByPatient(UUID patientId) {
        patientService.findById(patientId);
        return ficheSigneeRepository.findResumesByPatientId(patientId)
                .stream().map(ficheSigneeMapper::toModel).toList();
    }

    /**
     * Premier QR PROXYMED valide trouve, dans l'ordre des pages : une autre page peut porter un
     * QR etranger (ex. document joint). Si aucun ne l'est, l'erreur du premier QR est renvoyee.
     */
    private ContenuQrModel lireQrCode(byte[] pdf) {
        List<String> textes = detecteurQrPdf.detecter(pdf);
        if (textes.isEmpty()) {
            throw new QrCodeIntrouvableException("Aucun code QR n'a ete trouve dans le PDF importe : "
                    + "verifiez que la fiche a ete scannee en entier et que le code QR est net");
        }
        RegleGestionException premiereErreur = null;
        for (String texte : textes) {
            try {
                return lecteurContenuQr.lire(texte);
            } catch (RegleGestionException e) {
                premiereErreur = premiereErreur == null ? e : premiereErreur;
            }
        }
        throw premiereErreur;
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
