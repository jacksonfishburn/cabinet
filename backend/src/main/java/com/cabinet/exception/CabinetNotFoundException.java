package com.cabinet.exception;

public class CabinetNotFoundException extends CabinetException {
    public CabinetNotFoundException() {
        super("Could not find Cabinet");
    }
}
