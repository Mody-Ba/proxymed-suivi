package com.proxymed.suivi.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.proxymed.suivi.service.model.AlerteModel;
import com.proxymed.suivi.service.model.ConstanteVisiteModel;
import com.proxymed.suivi.service.model.VisiteSuiviModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Genere le PDF de la Fiche 2 : template Thymeleaf -> XHTML -> openhtmltopdf, avec un QR code
 * qui encode uniquement l'UUID de la visite. Meme approche que le PdfGenerationService de
 * PROXYMED 1. Simple mise en forme : aucune regle metier ici (les alertes et la grille
 * arrivent deja calculees dans le VisiteSuiviModel).
 */
@Component
public class PdfGenerationService {

    static final String TEMPLATE_FICHE_VISITE = "fiche-visite";
    private static final int TAILLE_QR_CODE_PX = 300;

    private final ITemplateEngine templateEngine;
    private final ZoneId fuseauHoraire;

    public PdfGenerationService(ITemplateEngine templateEngine,
                                @Value("${proxymed.pdf.fuseau-horaire:Africa/Dakar}") ZoneId fuseauHoraire) {
        this.templateEngine = templateEngine;
        this.fuseauHoraire = fuseauHoraire;
    }

    /**
     * Le PDF est ecrit dans un fichier temporaire, renvoye sous forme de Resource : il est
     * supprime des que son contenu a ete lu (cf. {@link FichePdfTemporaire}).
     */
    public Resource genererFicheVisite(VisiteSuiviModel visite) {
        return convertirEnPdf(remplirTemplate(visite));
    }

    String remplirTemplate(VisiteSuiviModel visite) {
        Context context = new Context(Locale.FRENCH, Map.of(
                "v", visite,
                "qrCode", genererQrCodeBase64(visite.id()),
                "fuseauHoraire", fuseauHoraire,
                "grille", cellulesDeLaGrille(visite.constantes()),
                "constantesEnAlerte", constantesEnAlerte(visite.alertes()),
                "signes", noms(visite.signesAlerteCliniques()),
                "evenements", noms(visite.evenementsIntercurrents()),
                "themes", noms(visite.themesAbordes())));
        return templateEngine.process(TEMPLATE_FICHE_VISITE, context);
    }

    String genererQrCodeBase64(UUID visiteId) {
        try {
            BitMatrix matrice = new QRCodeWriter().encode(
                    visiteId.toString(), BarcodeFormat.QR_CODE, TAILLE_QR_CODE_PX, TAILLE_QR_CODE_PX,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1));
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrice, "PNG", png);
            return Base64.getEncoder().encodeToString(png.toByteArray());
        } catch (WriterException e) {
            throw new IllegalStateException("Impossible de generer le QR code de la visite " + visiteId, e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Une case du tableau des constantes, deja mise en forme (ex. "130/80", "37,2").
     */
    public record CelluleGrille(String texte, boolean alerte) {
    }

    /**
     * Cases remplies du tableau des constantes, indexees par "TYPE:CRENEAU" (ex. "TA:H0").
     */
    private Map<String, CelluleGrille> cellulesDeLaGrille(List<ConstanteVisiteModel> constantes) {
        Map<String, CelluleGrille> cellules = new HashMap<>();
        if (constantes != null) {
            constantes.forEach(c -> cellules.put(c.type().name() + ":" + c.creneau().name(),
                    new CelluleGrille(formater(c), Boolean.TRUE.equals(c.alerte()))));
        }
        return cellules;
    }

    private String formater(ConstanteVisiteModel constante) {
        String valeur = formater(constante.valeur());
        return constante.valeurDiastolique() != null ? valeur + "/" + formater(constante.valeurDiastolique()) : valeur;
    }

    /**
     * Notation francaise sans zeros inutiles : 130.00 -> "130", 37.20 -> "37,2".
     */
    private String formater(BigDecimal valeur) {
        return valeur.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private Set<String> constantesEnAlerte(List<AlerteModel> alertes) {
        if (alertes == null) {
            return Set.of();
        }
        return alertes.stream().map(AlerteModel::constante).filter(Objects::nonNull)
                .map(Enum::name).collect(Collectors.toSet());
    }

    /**
     * Noms des valeurs cochees d'une liste a choix multiples, pour cocher les cases du PDF.
     */
    private Set<String> noms(Collection<? extends Enum<?>> valeurs) {
        return valeurs == null ? Set.of() : valeurs.stream().map(Enum::name).collect(Collectors.toSet());
    }

    private Resource convertirEnPdf(String html) {
        try {
            var file = File.createTempFile("fiche-visite-", ".pdf");
            try (var pdf = new FileOutputStream(file)) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(html, null);
                builder.toStream(pdf);
                builder.run();
            } catch (IOException | RuntimeException e) {
                file.delete();
                throw e;
            }
            return new FichePdfTemporaire(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Echec de la conversion HTML -> PDF de la fiche", e);
        }
    }

    /**
     * Fichier PDF a usage unique : supprime du disque a la fermeture du flux de lecture,
     * pour ne pas accumuler un fichier temporaire par telechargement.
     */
    private static final class FichePdfTemporaire extends FileSystemResource {

        private FichePdfTemporaire(File file) {
            super(file);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(getFile().toPath(), StandardOpenOption.DELETE_ON_CLOSE);
        }
    }
}
