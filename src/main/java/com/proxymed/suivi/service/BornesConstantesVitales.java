package com.proxymed.suivi.service;

import com.proxymed.suivi.enums.TypeConstante;
import com.proxymed.suivi.exception.RegleGestionException;
import com.proxymed.suivi.service.model.ConstanteVisiteModel;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Bornes de validite des constantes saisies : refuse les valeurs physiologiquement
 * impossibles (erreur de saisie), sans rapport avec les seuils d'alerte (SeuilsAlerte).
 *
 * PROVISOIRES : a valider medicalement (cahier des charges, section 2 point 3 et section 11
 * point 4). Pour les changer, il suffit de modifier les valeurs ci-dessous.
 */
final class BornesConstantesVitales {

    private record Bornes(String min, String max) {
    }

    private static final Map<TypeConstante, Bornes> BORNES = Map.of(
            TypeConstante.TA, new Bornes("40", "300"),
            TypeConstante.FC, new Bornes("20", "250"),
            TypeConstante.FR, new Bornes("4", "70"),
            TypeConstante.SPO2, new Bornes("50", "100"),
            TypeConstante.TEMPERATURE, new Bornes("30", "45"),
            TypeConstante.GLYCEMIE, new Bornes("0.1", "10"),
            TypeConstante.EVA, new Bornes("0", "10"),
            TypeConstante.GLASGOW, new Bornes("3", "15")
    );

    private static final Bornes TA_DIASTOLIQUE = new Bornes("20", "200");

    private BornesConstantesVitales() {
    }

    static void valider(ConstanteVisiteModel constante) {
        TypeConstante type = constante.type();
        verifier(type.getLibelle(), constante.valeur(), BORNES.get(type), type.getUnite());

        if (type == TypeConstante.TA) {
            verifier("Tension artérielle diastolique", constante.valeurDiastolique(), TA_DIASTOLIQUE, type.getUnite());
            if (constante.valeurDiastolique().compareTo(constante.valeur()) >= 0) {
                throw new RegleGestionException(
                        "La tension diastolique doit etre inferieure a la systolique");
            }
        }
        if ((type == TypeConstante.EVA || type == TypeConstante.GLASGOW) && !estEntier(constante.valeur())) {
            throw new RegleGestionException(type.getLibelle() + " : la valeur doit etre un nombre entier");
        }
    }

    private static void verifier(String libelle, BigDecimal valeur, Bornes bornes, String unite) {
        if (valeur.compareTo(new BigDecimal(bornes.min())) < 0 || valeur.compareTo(new BigDecimal(bornes.max())) > 0) {
            throw new RegleGestionException(libelle + " : valeur " + valeur.toPlainString()
                    + " impossible (attendu entre " + bornes.min() + " et " + bornes.max() + " " + unite + ")");
        }
    }

    private static boolean estEntier(BigDecimal valeur) {
        return valeur.stripTrailingZeros().scale() <= 0;
    }
}
