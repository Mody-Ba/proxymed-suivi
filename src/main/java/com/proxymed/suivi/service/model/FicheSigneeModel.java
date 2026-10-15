package com.proxymed.suivi.service.model;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Couche interne neutre : ni le contrat API (fichier multipart / FicheSigneeResponse), ni l'entite JPA.
 * id, visiteId et dateImport ne sont renseignes que sur le chemin de lecture (Entity -> Model).
 */
@Builder(toBuilder = true)
public record FicheSigneeModel(
        UUID id,
        UUID visiteId,
        String nomFichier,
        String typeContenu,
        long tailleOctets,
        Instant dateImport,
        byte[] contenu
) {
}
