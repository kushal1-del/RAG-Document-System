package com.kushal.rag.exception;

/**
 * Thrown when a caller has exceeded their allowed rate limit
 * (e.g. too many registration attempts from one IP).
 * Mapped to HTTP 429 Too Many Requests in GlobalExceptionHandler.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}