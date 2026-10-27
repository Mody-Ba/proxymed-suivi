package com.proxymed.suivi.controller;

import com.proxymed.suivi.mappers.FicheSigneeMapper;
import com.proxymed.suivi.service.FicheSigneeService;
import com.proxymed.suivi.service.model.FicheSigneeModel;
import com.proxymed.suivi.service.model.FicheSigneeResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FicheSigneeController {

    private final FicheSigneeService ficheSigneeService;
    private final FicheSigneeMapper ficheSigneeMapper;

    /**
     * Importe la fiche remplie, signee et scannee (PDF, 10 Mo au maximum), sans aucun id : le
     * patient et la visite sont lus dans le QR code de la fiche. La reponse indique ou elle a ete rangee.
     */
    @ApiResponse(responseCode = "201", description = "Fiche rangee dans le dossier du patient")
    @ApiResponse(responseCode = "400", description = "Fichier non PDF, trop volumineux ou illisible ; QR non PROXYMED "
            + "ou d'un autre type de fiche ; patient du QR different de celui de la visite")
    @ApiResponse(responseCode = "404", description = "Patient ou visite du QR code introuvable")
    @ApiResponse(responseCode = "409", description = "La visite a deja une fiche signee")
    @ApiResponse(responseCode = "422", description = "Aucun code QR trouve dans le PDF")
    @PostMapping(value = "/fiches-signees/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FicheSigneeResponse importer(@RequestPart("fichier") MultipartFile fichier) {
        return ficheSigneeMapper.toResponse(ficheSigneeService.importer(ficheSigneeMapper.toModel(fichier)));
    }

    /**
     * Fiches signees du dossier du patient, de la visite la plus recente a la plus ancienne.
     */
    @GetMapping("/patients/{patientId}/fiches-signees")
    public List<FicheSigneeResponse> findByPatient(@PathVariable UUID patientId) {
        return ficheSigneeService.findByPatient(patientId).stream().map(ficheSigneeMapper::toResponse).toList();
    }

    @ApiResponse(responseCode = "200", description = "Fiche signee de la visite, au format PDF",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
    @GetMapping(value = "/visites/{visiteId}/fiche-signee", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> telecharger(@PathVariable UUID visiteId) {
        FicheSigneeModel fiche = ficheSigneeService.telecharger(visiteId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fiche.nomFichier(), StandardCharsets.UTF_8).build().toString())
                .body(new ByteArrayResource(fiche.contenu()));
    }
}
