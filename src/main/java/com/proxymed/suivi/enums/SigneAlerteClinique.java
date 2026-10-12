package com.proxymed.suivi.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Signes cliniques de l'encadre "Seuils d'alerte" (section 3) : chacun impose l'appel
 * immediat du medecin senior, au meme titre qu'une constante hors seuil.
 */
@Getter
@RequiredArgsConstructor
public enum SigneAlerteClinique {
    SIGNE_NEUROLOGIQUE_FOCAL_AIGU("Tout signe neurologique focal aigu"),
    DETRESSE_RESPIRATOIRE("Détresse respiratoire"),
    DOULEUR_THORACIQUE("Douleur thoracique"),
    AUTRE_SIGNE_CLINIQUE_GRAVE("Autre signe clinique grave");

    private final String libelle;
}
