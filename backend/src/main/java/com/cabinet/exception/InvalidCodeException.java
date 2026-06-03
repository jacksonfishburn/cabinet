package com.cabinet.exception;

public class InvalidCodeException extends CabinetException {
    public InvalidCodeException(){
        super("Invalid Invite Code");
    }
    public InvalidCodeException(String message) {
        super(message);
    }
}
