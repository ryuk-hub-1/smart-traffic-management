package com.smarttraffic.exception;

/**
 * Thrown when low-level JDBC SQL operations fail.
 */
public class DatabaseException extends SmartTrafficException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
