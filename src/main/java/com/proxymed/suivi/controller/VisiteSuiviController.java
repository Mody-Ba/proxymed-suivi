package com.proxymed.suivi.controller;

import com.proxymed.suivi.mappers.VisiteSuiviMapper;
import com.proxymed.suivi.service.VisiteSuiviService;
import com.proxymed.suivi.service.model.SignaturePatientRequest;
import com.proxymed.suivi.service.model.VisiteEnAttenteVisaResponse;
import com.proxymed.suivi.service.model.VisiteSuiviCreateRequest;
import com.proxymed.suivi.service.model.VisiteSuiviResponse;
import com.proxymed.suivi.service.model.VisiteSuiviSummaryResponse;
import com.proxymed.suivi.service.model.VisiteSuiviUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VisiteSuiviController {

    private final VisiteSuiviService visiteSuiviService;
    private final VisiteSuiviMapper visiteSuiviMapper;

    @PostMapping("/patients/{patientId}/visites")
    @ResponseStatus(HttpStatus.CREATED)
    public VisiteSuiviResponse creer(@PathVariable UUID patientId, @Valid @RequestBody VisiteSuiviCreateRequest request) {
        return visiteSuiviMapper.toResponse(
                visiteSuiviService.creerBrouillon(visiteSuiviMapper.toModel(patientId, request)));
    }

    /**
     * Visites d'un patient, de la plus recente a la plus ancienne.
     */
    @GetMapping("/patients/{patientId}/visites")
    public List<VisiteSuiviSummaryResponse> findByPatient(@PathVariable UUID patientId) {
        return visiteSuiviService.findByPatient(patientId)
                .stream().map(visiteSuiviMapper::toSummaryResponse).toList();
    }

    @GetMapping("/visites/{id}")
    public VisiteSuiviResponse findById(@PathVariable UUID id) {
        return visiteSuiviMapper.toResponse(visiteSuiviService.findById(id));
    }

    /**
     * Fiches terminees en attente du visa du senior, de la plus ancienne a la plus recente.
     */
    @GetMapping("/visites/en-attente-visa")
    public List<VisiteEnAttenteVisaResponse> findEnAttenteVisa() {
        return visiteSuiviService.findEnAttenteVisa()
                .stream().map(visiteSuiviMapper::toEnAttenteVisaResponse).toList();
    }

    /**
     * Le medecin junior termine et signe la fiche (BROUILLON -> TERMINEE).
     */
    @PostMapping("/visites/{id}/terminer")
    public VisiteSuiviResponse terminer(@PathVariable UUID id) {
        return visiteSuiviMapper.toResponse(visiteSuiviService.terminer(id));
    }

    /**
     * Visa du medecin senior (TERMINEE -> VISEE).
     */
    @PostMapping("/visites/{id}/viser")
    public VisiteSuiviResponse viser(@PathVariable UUID id) {
        return visiteSuiviMapper.toResponse(visiteSuiviService.viser(id));
    }

    @PostMapping("/visites/{id}/signature-patient")
    public VisiteSuiviResponse signerPatient(@PathVariable UUID id, @Valid @RequestBody SignaturePatientRequest request) {
        return visiteSuiviMapper.toResponse(visiteSuiviService.signerPatient(id, request.signataire()));
    }

    @PutMapping("/visites/{id}")
    public VisiteSuiviResponse mettreAJour(@PathVariable UUID id, @Valid @RequestBody VisiteSuiviUpdateRequest request) {
        return visiteSuiviMapper.toResponse(visiteSuiviService.mettreAJour(id, visiteSuiviMapper.toModel(request)));
    }
}
