package com.proxymed.suivi.exception;

/**
 * Violation d'une regle de gestion du cahier des charges (section 6) :
 * role du medecin incorrect, champ conditionnellement obligatoire manquant, etc.
 */
public class RegleGestionException extends RuntimeException {

    public RegleGestionException(String message) {
        super(message);
    }
}
