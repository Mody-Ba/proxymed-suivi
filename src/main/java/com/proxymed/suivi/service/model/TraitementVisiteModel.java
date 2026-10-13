package com.proxymed.suivi.service.model;

import lombok.Builder;

import java.util.UUID;

/**
 * Couche interne neutre : ni le contrat API (TraitementVisiteRequest/UpdateRequest/Response),
 * ni l'entite JPA.
 */
@Builder(toBuilder = true)
public record TraitementVisiteModel(
        Long id,
        UUID visiteId,
        String medicament,
        String posologie,
        String voie,
        Boolean observant,
        Boolean probleme,
        String problemePrecision
) {
}
