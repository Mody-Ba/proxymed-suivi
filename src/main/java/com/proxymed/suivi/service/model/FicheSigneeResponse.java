package com.proxymed.suivi.service.model;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadonnees uniquement : le contenu du fichier n'est jamais renvoye ici.
 * patientId, visiteId et numeroVisite indiquent ou la fiche a ete rangee (deduits du QR code).
 */
@Builder(toBuilder = true)
public record FicheSigneeResponse(
        UUID id,
        UUID patientId,
        UUID visiteId,
        Integer numeroVisite,
        String nomFichier,
        long tailleOctets,
        Instant dateImport
) {
}
