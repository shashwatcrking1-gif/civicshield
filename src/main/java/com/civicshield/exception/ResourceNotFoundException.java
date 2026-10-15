package com.civicshield.exception;

/** Thrown when an entity id does not exist; rendered as a styled 404 page (or JSON for /api). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String entity, Object id) {
        super(entity + " #" + id + " was not found");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
