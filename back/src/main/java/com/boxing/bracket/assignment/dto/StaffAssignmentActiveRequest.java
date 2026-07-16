package com.boxing.bracket.assignment.dto;

import javax.validation.constraints.NotNull;

public class StaffAssignmentActiveRequest {

    @NotNull(message = "active is required")
    private Boolean active;

    protected StaffAssignmentActiveRequest() {
    }

    public StaffAssignmentActiveRequest(Boolean active) {
        this.active = active;
    }

    public Boolean getActive() { return active; }
}
