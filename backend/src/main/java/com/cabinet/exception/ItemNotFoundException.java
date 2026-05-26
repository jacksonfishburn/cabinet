package com.cabinet.exception;

public class ItemNotFoundException extends CabinetException {
    public ItemNotFoundException(String item) {
        super("No Archive named '" + item + " found");
    }
}
