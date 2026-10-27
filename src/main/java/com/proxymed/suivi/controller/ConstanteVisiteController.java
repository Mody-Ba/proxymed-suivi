package com.proxymed.suivi.controller;

import com.proxymed.suivi.mappers.ConstanteVisiteMapper;
import com.proxymed.suivi.service.ConstanteVisiteService;
import com.proxymed.suivi.service.model.ConstanteVisiteRequest;
import com.proxymed.suivi.service.model.ConstanteVisiteResponse;
import com.proxymed.suivi.service.model.LigneGrilleConstantesResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/visites/{visiteId}/constantes")
@RequiredArgsConstructor
public class ConstanteVisiteController {

    private final ConstanteVisiteService constanteVisiteService;
    private final ConstanteVisiteMapper constanteVisiteMapper;

    /**
     * Enregistre une mesure ; remplace celle du meme type et du meme creneau si elle existe.
     */
    @PreAuthorize("hasAnyRole('MEDECIN_JUNIOR', 'MEDECIN_SENIOR')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConstanteVisiteResponse enregistrer(@PathVariable UUID visiteId,
                                               @Valid @RequestBody ConstanteVisiteRequest request) {
        return constanteVisiteMapper.toResponse(
                constanteVisiteService.enregistrer(visiteId, constanteVisiteMapper.toModel(request)));
    }

    @PreAuthorize("hasAnyRole('MEDECIN_JUNIOR', 'MEDECIN_SENIOR')")
    @GetMapping
    public List<LigneGrilleConstantesResponse> lireGrille(@PathVariable UUID visiteId) {
        return constanteVisiteMapper.toGrilleResponse(constanteVisiteService.lireGrille(visiteId));
    }

    @PreAuthorize("hasAnyRole('MEDECIN_JUNIOR', 'MEDECIN_SENIOR')")
    @DeleteMapping("/{constanteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable UUID visiteId, @PathVariable Long constanteId) {
        constanteVisiteService.supprimer(visiteId, constanteId);
    }
}
