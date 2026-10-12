package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.TypeConstante;
import lombok.Builder;

import java.util.List;

/**
 * Une ligne de la grille telle qu'imprimee sur la fiche : la constante, son unite, la colonne
 * "Valeurs normales" et les mesures saisies (H0 a H+4).
 */
@Builder(toBuilder = true)
public record LigneGrilleConstantesResponse(
        TypeConstante type,
        String libelle,
        String unite,
        String valeursNormales,
        List<ConstanteVisiteResponse> mesures
) {
}
