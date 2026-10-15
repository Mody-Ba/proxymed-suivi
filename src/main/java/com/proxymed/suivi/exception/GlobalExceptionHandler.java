package com.proxymed.suivi.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.util.List;

/**
 * Le type de contenu JSON est force sur chaque reponse d'erreur : sans cela, une erreur sur
 * un endpoint binaire appele avec "Accept: application/pdf" (cas de Swagger) echouerait en
 * 406 au lieu de renvoyer le message d'erreur.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Ressource introuvable", ex.getMessage()));
    }

    @ExceptionHandler(RegleGestionException.class)
    public ResponseEntity<ErrorResponse> handleRegleGestion(RegleGestionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Regle de gestion non respectee", ex.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(ConflitEtatException.class)
    public ResponseEntity<ErrorResponse> handleConflitEtat(ConflitEtatException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.CONFLICT.value(), "Action incompatible avec le statut de la fiche", ex.getMessage()));
    }

    /**
     * Fichier refuse par Spring avant meme d'atteindre le service (limites
     * spring.servlet.multipart.*) : 413 avec un message lisible au lieu d'une page d'erreur.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.CONTENT_TOO_LARGE.value(), "Fichier trop volumineux",
                        "Le fichier importe depasse la taille maximale de 10 Mo"));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Requete invalide",
                        "La partie \"" + ex.getRequestPartName() + "\" (fichier PDF) est obligatoire"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " : " + fe.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation echouee", "Un ou plusieurs champs sont invalides", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation echouee", ex.getMessage()));
    }

    /**
     * Corps JSON illisible. Cas le plus courant : un champ inconnu du DTO (rejete grace a
     * spring.jackson.deserialization.fail-on-unknown-properties=true), par exemple un
     * "numeroVisite" envoye a la creation alors qu'il est calcule par le service.
     * On nomme le champ fautif pour que le client sache quoi corriger.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        String message = ex.getMostSpecificCause() instanceof UnrecognizedPropertyException upe
                ? "Champ inconnu dans la requete : " + upe.getPropertyName()
                : "Le corps de la requete est illisible ou mal forme";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Requete invalide", message));
    }
}
