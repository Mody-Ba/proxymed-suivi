package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.RoleMedecin;
import lombok.Builder;

/**
 * Couche interne neutre : ni le contrat API (MedecinResponse), ni l'entite JPA.
 */
@Builder(toBuilder = true)
public record MedecinModel(
        Long id,
        String nom,
        String prenom,
        String numeroOrdre,
        RoleMedecin role,
        String telephone
) {
}
