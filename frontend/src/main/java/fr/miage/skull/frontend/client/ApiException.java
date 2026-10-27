package fr.miage.skull.frontend.client;

import java.util.Map;

/** Erreur renvoyée par le back-end : code HTTP, message, et erreurs par champ (400). */
public class ApiException extends RuntimeException {

    private final int status;
    private final Map<String, String> fieldErrors;

    public ApiException(int status, String message, Map<String, String> fieldErrors) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors;
    }

    public int getStatus() { return status; }

    public Map<String, String> getFieldErrors() { return fieldErrors; }
}
