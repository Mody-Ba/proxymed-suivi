package com.proxymed.suivi.exception;

/**
 * Action incompatible avec le statut courant de la fiche
 * (ex. modifier une fiche TERMINEE ou VISEE).
 */
public class ConflitEtatException extends RuntimeException {

    public ConflitEtatException(String message) {
        super(message);
    }
}
