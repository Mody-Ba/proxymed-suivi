package com.proxymed.suivi.service.model;

import jakarta.validation.constraints.Size;

/**
 * Mise a jour partielle d'une ligne du tableau des traitements : un champ absent (null)
 * conserve sa valeur actuelle.
 */
public record TraitementVisiteUpdateRequest(
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
