package com.proxymed.suivi.service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Ajout d'une ligne au tableau des traitements : seul le medicament est obligatoire.
 */
public record TraitementVisiteRequest(
        @NotBlank(message = "le medicament est obligatoire")
        @Size(max = 255, message = "255 caracteres au maximum")
        String medicament,

        @Size(max = 255, message = "255 caracteres au maximum")
        String posologie,

        @Size(max = 100, message = "100 caracteres au maximum")
        String voie,

        Boolean observant,
        Boolean probleme,
        String problemePrecision
) {
}
