package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.RoleMedecin;
import lombok.Builder;

@Builder(toBuilder = true)
public record MedecinResponse(
        Long id,
        String nom,
        String prenom,
        String numeroOrdre,
        RoleMedecin role,
        String telephone
) {
}
