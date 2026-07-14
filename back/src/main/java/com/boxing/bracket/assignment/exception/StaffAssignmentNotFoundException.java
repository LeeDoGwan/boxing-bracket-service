package com.boxing.bracket.assignment.exception;

public class StaffAssignmentNotFoundException extends RuntimeException {

    public StaffAssignmentNotFoundException() {
        super("Assignment not found");
    }
}
