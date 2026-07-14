package com.boxing.bracket.athlete.exception;

public class AthleteNotFoundException extends RuntimeException {

    public AthleteNotFoundException() {
        super("Athlete not found");
    }
}
