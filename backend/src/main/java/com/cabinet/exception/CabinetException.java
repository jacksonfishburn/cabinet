package com.cabinet.exception;

public class CabinetException extends RuntimeException {
    public CabinetException(String message) {
        super(message);
    }

    public CabinetException(String message, Throwable cause) {
        super(message, cause);
    }
}
