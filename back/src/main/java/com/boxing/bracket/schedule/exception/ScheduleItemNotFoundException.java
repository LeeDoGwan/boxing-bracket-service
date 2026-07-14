package com.boxing.bracket.schedule.exception;

public class ScheduleItemNotFoundException extends RuntimeException {

    public ScheduleItemNotFoundException() {
        super("Schedule item not found");
    }
}
