package com.proxymed.suivi.controller;

import com.proxymed.suivi.enums.RoleMedecin;
import com.proxymed.suivi.mappers.MedecinMapper;
import com.proxymed.suivi.service.MedecinService;
import com.proxymed.suivi.service.model.MedecinResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/medecins")
@RequiredArgsConstructor
public class MedecinController {

    private final MedecinService medecinService;
    private final MedecinMapper medecinMapper;

    @GetMapping
    public List<MedecinResponse> findAll(@RequestParam(required = false) RoleMedecin role) {
        return medecinService.findAll(role).stream().map(medecinMapper::toResponse).toList();
    }
}
