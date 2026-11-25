package com.smarttraffic.exception;

/**
 * Base checked exception for Smart Traffic Management System.
 * Demonstrates Custom Exception Handling in Java.
 */
public class SmartTrafficException extends Exception {
    public SmartTrafficException(String message) {
        super(message);
    }

    public SmartTrafficException(String message, Throwable cause) {
        super(message, cause);
    }
}
