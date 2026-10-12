package com.proxymed.suivi.service.model;

import com.proxymed.suivi.enums.TypeConstante;
import lombok.Builder;

import java.util.List;

/**
 * Une ligne de la grille des constantes : un type et ses mesures, triees par creneau.
 */
@Builder(toBuilder = true)
public record LigneGrilleConstantesModel(
        TypeConstante type,
        List<ConstanteVisiteModel> mesures
) {
}
