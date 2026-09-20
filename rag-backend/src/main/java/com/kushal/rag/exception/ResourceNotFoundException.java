package com.kushal.rag.exception;

/**
 * Throw this specifically whenever something is looked up by ID
 * and doesn't exist — a user, a document, etc.
 *
 * Using this instead of a generic RuntimeException lets
 * GlobalExceptionHandler map it cleanly to 404 Not Found,
 * instead of it falling into the generic 500 catch-all.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}