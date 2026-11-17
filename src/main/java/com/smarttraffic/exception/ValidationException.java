package com.smarttraffic.exception;

/**
 * Thrown when form input validation fails.
 */
public class ValidationException extends SmartTrafficException {
    public ValidationException(String message) {
        super(message);
    }
}
