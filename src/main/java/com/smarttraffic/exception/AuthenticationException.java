package com.smarttraffic.exception;

/**
 * Thrown when user credentials or permissions fail verification.
 */
public class AuthenticationException extends SmartTrafficException {
    public AuthenticationException(String message) {
        super(message);
    }
}
