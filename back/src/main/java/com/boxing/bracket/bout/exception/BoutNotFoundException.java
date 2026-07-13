package com.boxing.bracket.bout.exception;

public class BoutNotFoundException extends RuntimeException {

    public BoutNotFoundException() {
        super("Bout not found");
    }
}
