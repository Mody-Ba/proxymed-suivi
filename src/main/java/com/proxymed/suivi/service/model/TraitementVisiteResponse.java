package com.proxymed.suivi.service.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record TraitementVisiteResponse(
        Long id,
        String medicament,
        String posologie,
        String voie,
        Boolean observant,
        Boolean probleme,
        String problemePrecision
) {
}
