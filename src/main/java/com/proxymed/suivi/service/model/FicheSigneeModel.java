package com.proxymed.suivi.service.model;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Couche interne neutre : ni le contrat API (fichier multipart / FicheSigneeResponse), ni l'entite JPA.
 * id, visiteId, patientId, numeroVisite et dateImport ne sont renseignes que sur le chemin de
 * lecture (Entity -> Model) : a l'import, la visite est deduite du QR code du PDF.
 * contenu est null dans les listes du dossier patient (metadonnees seulement).
 * Une seule fiche par visite (contrainte uk_fiche_signee_visite).
 */
@Builder(toBuilder = true)
public record FicheSigneeModel(
        UUID id,
        UUID visiteId,
        UUID patientId,
        Integer numeroVisite,
        String nomFichier,
        String typeContenu,
        long tailleOctets,
        Instant dateImport,
        byte[] contenu
) {
}
