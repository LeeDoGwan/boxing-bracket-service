package com.boxing.bracket.ring.exception;

public class RingNotFoundException extends RuntimeException {

    public RingNotFoundException() {
        super("Ring not found");
    }
}
