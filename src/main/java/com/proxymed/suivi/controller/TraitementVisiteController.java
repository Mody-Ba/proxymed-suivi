package com.proxymed.suivi.controller;

import com.proxymed.suivi.mappers.TraitementVisiteMapper;
import com.proxymed.suivi.service.TraitementVisiteService;
import com.proxymed.suivi.service.model.TraitementVisiteRequest;
import com.proxymed.suivi.service.model.TraitementVisiteResponse;
import com.proxymed.suivi.service.model.TraitementVisiteUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/visites/{visiteId}/traitements")
@RequiredArgsConstructor
public class TraitementVisiteController {

    private final TraitementVisiteService traitementVisiteService;
    private final TraitementVisiteMapper traitementVisiteMapper;

    @PreAuthorize("hasAnyRole('MEDECIN_JUNIOR', 'MEDECIN_SENIOR')")
    @GetMapping
    public List<TraitementVisiteResponse> lister(@PathVariable UUID visiteId) {
        return traitementVisiteMapper.toResponseList(traitementVisiteService.lister(visiteId));
    }

    @PreAuthorize("hasAnyRole('MEDECIN_JUNIOR', 'MEDECIN_SENIOR')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TraitementVisiteResponse ajouter(@PathVariable UUID visiteId,
                                            @Valid @RequestBody TraitementVisiteRequest request) {
        return traitementVisiteMapper.toResponse(
                traitementVisiteService.ajouter(visiteId, traitementVisiteMapper.toModel(request)));
    }

    @PreAuthorize("hasAnyRole('MEDECIN_JUNIOR', 'MEDECIN_SENIOR')")
    @PutMapping("/{traitementId}")
    public TraitementVisiteResponse modifier(@PathVariable UUID visiteId, @PathVariable Long traitementId,
                                             @Valid @RequestBody TraitementVisiteUpdateRequest request) {
        return traitementVisiteMapper.toResponse(
                traitementVisiteService.modifier(visiteId, traitementId, traitementVisiteMapper.toModel(request)));
    }

    @PreAuthorize("hasAnyRole('MEDECIN_JUNIOR', 'MEDECIN_SENIOR')")
    @DeleteMapping("/{traitementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable UUID visiteId, @PathVariable Long traitementId) {
        traitementVisiteService.supprimer(visiteId, traitementId);
    }
}
