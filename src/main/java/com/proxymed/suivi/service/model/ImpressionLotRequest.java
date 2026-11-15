package com.proxymed.suivi.service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

/**
 * Impression en lot de fiches vierges par l'admin : un numero de dossier PROXYMED par fiche.
 */
@Builder(toBuilder = true)
public record ImpressionLotRequest(
        @NotEmpty(message = "la liste des numeros de dossier ne doit pas etre vide")
        @Size(max = ImpressionLotRequest.MAX_FICHES,
                message = "au plus " + ImpressionLotRequest.MAX_FICHES + " fiches par impression")
        List<@NotBlank(message = "un numero de dossier ne peut pas etre vide") String> numerosDossierProxymed
) {

    public static final int MAX_FICHES = 50;
}
