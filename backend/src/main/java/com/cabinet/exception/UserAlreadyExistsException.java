package com.cabinet.exception;

public class UserAlreadyExistsException extends CabinetException {
    public UserAlreadyExistsException(String name) {
        super("Username '" + name + "' is already taken.");
    }
}
