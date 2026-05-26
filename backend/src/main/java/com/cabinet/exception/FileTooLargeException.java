package com.cabinet.exception;

public class FileTooLargeException extends RuntimeException {
    public FileTooLargeException(long maxSize) {
        super("File exceeds maximum upload size of " + maxSize + " bytes");
    }
}
