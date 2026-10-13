package com.proxymed.suivi.exception;

import lombok.Getter;

import java.util.List;

/**
 * Violation d'une regle de gestion du cahier des charges (section 6) :
 * role du medecin incorrect, champ conditionnellement obligatoire manquant, etc.
 * details liste, le cas echeant, chaque manquement (ex. champs requis pour terminer la fiche).
 */
@Getter
public class RegleGestionException extends RuntimeException {

    private final List<String> details;

    public RegleGestionException(String message) {
        this(message, List.of());
    }

    public RegleGestionException(String message, List<String> details) {
        super(message);
        this.details = List.copyOf(details);
    }
}
