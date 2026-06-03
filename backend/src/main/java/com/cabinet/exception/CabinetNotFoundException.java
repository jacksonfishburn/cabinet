package com.cabinet.exception;

public class CabinetNotFoundException extends CabinetException {
    public CabinetNotFoundException(String cabinetName) {
        super("No Cabinet named '" + cabinetName + " found");
    }
}
