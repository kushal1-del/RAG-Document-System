package com.kushal.rag.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import com.kushal.rag.exception.RateLimitExceededException;

/**
 * Central place that catches exceptions thrown ANYWHERE in the app
 * (any @RestController) and converts them into consistent JSON
 * error responses, instead of Spring's default HTML/generic error page.
 *
 * @RestControllerAdvice automatically applies to every controller —
 * you never need to add try/catch blocks in controllers for these cases.
 *
 * Handlers are ordered from MOST specific to LEAST specific.
 * Spring always picks the closest matching handler for a given
 * exception type, regardless of the order they're written in this
 * file — but we still order them top-to-bottom this way for
 * readability, so it's obvious which cases are handled explicitly
 * before falling through to the generic catch-all at the bottom.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================
    // 400 — BAD REQUEST (client sent something invalid)
    // =========================================================

    /**
     * Our own business-rule errors: invalid login, duplicate email,
     * disabled account, invalid role, missing required field logic, etc.
     * We throw these ourselves throughout the services.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /**
     * Thrown when @RequestBody validation fails (e.g. if we later add
     * @Valid + @NotBlank annotations on DTOs like RegisterRequest).
     * We're not using @Valid yet, but this makes the handler ready
     * for when we add proper input validation later.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {

        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("Validation failed.");

        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Thrown when the request body is malformed JSON, missing entirely,
     * or doesn't match the expected DTO shape (e.g. wrong field types).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed or missing request body.");
    }

    /**
     * Thrown by Spring's file upload handling — e.g. a multipart
     * request that's malformed, or missing the expected "file" part.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipart(MultipartException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid file upload request.");
    }

    /**
     * Thrown when an uploaded file exceeds the configured max size
     * (Spring Boot's spring.servlet.multipart.max-file-size).
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleFileTooLarge(MaxUploadSizeExceededException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Uploaded file is too large.");
    }

    // =========================================================
    // 401 — UNAUTHORIZED (not logged in / bad credentials)
    // =========================================================

    /**
     * Covers Spring Security authentication failures in general
     * (bad/missing/expired token reaching a point where Spring
     * itself rejects it, wrong credentials during authentication, etc.)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException e) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Authentication failed.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException e) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials.");
    }

    // =========================================================
    // 403 — FORBIDDEN (logged in, but not allowed to do this)
    // =========================================================

    /**
     * Thrown when a logged-in user tries to access something their
     * role doesn't permit — e.g. a USER hitting /api/admin/**.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException e) {
        return buildResponse(HttpStatus.FORBIDDEN, "You do not have permission to perform this action.");
    }

    // =========================================================
    // 404 — NOT FOUND
    // =========================================================

    /**
     * Custom exception we control ourselves (created below) —
     * use this instead of a generic RuntimeException whenever
     * something (a user, document, etc.) isn't found by ID.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException e) {
        return buildResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // =========================================================
    // 409 — CONFLICT (e.g. database constraint violations)
    // =========================================================

    /**
     * Thrown by the database layer when a unique constraint is
     * violated (e.g. duplicate email slipping past our own check
     * due to a race condition), or a foreign key constraint fails.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        return buildResponse(HttpStatus.CONFLICT, "This operation conflicts with existing data.");
    }

    // =========================================================
    // 500 — CATCH-ALL (anything we didn't explicitly handle above)
    // =========================================================

    /**
     * The final safety net. Any exception type not specifically
     * handled above lands here: Qdrant failures, Gemini API errors,
     * unexpected null pointers, etc.
     *
     * We deliberately do NOT expose e.getMessage() raw here for
     * unknown exceptions in a real production system (it can leak
     * internal details like stack traces, SQL, file paths). For now,
     * during development, showing the real message is more useful
     * for debugging — we'll tighten this before going fully to
     * production if needed.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    // =========================================================
    // Shared response builder
    // =========================================================

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        return ResponseEntity.status(status).body(body);
    }
    
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException e) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }
}