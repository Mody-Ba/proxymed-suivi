package com.proxymed.suivi.exception;

import lombok.Getter;

import java.util.List;

/**
 * details liste, le cas echeant, chaque element introuvable (ex. numeros de dossier PROXYMED).
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final List<String> details;

    public ResourceNotFoundException(String message) {
        this(message, List.of());
    }

    public ResourceNotFoundException(String message, List<String> details) {
        super(message);
        this.details = List.copyOf(details);
    }
}
