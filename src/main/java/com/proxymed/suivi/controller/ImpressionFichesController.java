package com.proxymed.suivi.controller;

import com.proxymed.suivi.service.ImpressionFichesService;
import com.proxymed.suivi.service.model.FichesImprimeesModel;
import com.proxymed.suivi.service.model.ImpressionLotRequest;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Impression en lot de fiches de suivi vierges par l'admin. L'admin ne lit aucune donnee
 * medicale : la reponse ne contient que le PDF (identite + QR) et les ids des visites creees.
 * Seul endpoint de la Fiche 2 ouvert au role ADMIN.
 */
@RestController
@RequestMapping("/api/visites")
public class ImpressionFichesController {

    /** Ids des visites creees, separes par des virgules, dans l'ordre des fiches du PDF (expose par le CORS). */
    public static final String EN_TETE_VISITES = "X-Visites-Ids";

    private final ImpressionFichesService impressionFichesService;
    private final ZoneId fuseauHoraire;

    public ImpressionFichesController(ImpressionFichesService impressionFichesService,
                                      @Value("${proxymed.pdf.fuseau-horaire:Africa/Dakar}") ZoneId fuseauHoraire) {
        this.impressionFichesService = impressionFichesService;
        this.fuseauHoraire = fuseauHoraire;
    }

    /**
     * Une visite vierge creee par numero de dossier PROXYMED, et un seul PDF combine en retour
     * (une fiche par patient, chacune sur une nouvelle page, dans l'ordre de la liste). Tout ou rien.
     */
    @ApiResponse(responseCode = "200", description = "PDF combine des fiches vierges",
            headers = @Header(name = EN_TETE_VISITES, description = "Ids des visites creees, dans l'ordre des fiches"),
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "400", description = "Liste vide, de plus de " + ImpressionLotRequest.MAX_FICHES
            + " numeros, numero vide ou en double")
    @ApiResponse(responseCode = "403", description = "Reserve a l'admin")
    @ApiResponse(responseCode = "404", description = "Un ou plusieurs numeros de dossier introuvables (listes en details, aucune visite creee)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/impression-lot")
    public ResponseEntity<Resource> imprimerLot(@Valid @RequestBody ImpressionLotRequest request) {
        FichesImprimeesModel resultat = impressionFichesService.imprimer(request.numerosDossierProxymed());
        String nomFichier = "fiches-suivi-" + LocalDate.now(fuseauHoraire) + "-" + resultat.visiteIds().size() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(nomFichier).build().toString())
                .header(EN_TETE_VISITES, resultat.visiteIds().stream().map(UUID::toString).collect(Collectors.joining(",")))
                .body(resultat.pdf());
    }
}
