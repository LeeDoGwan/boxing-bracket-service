package com.boxing.bracket.assignment.dto;

import com.boxing.bracket.assignment.domain.StaffAssignment;
import com.boxing.bracket.user.domain.UserRole;

import java.time.LocalDateTime;

public class StaffAssignmentResponse {

    private final Long assignmentId;
    private final Long accountId;
    private final Long tournamentId;
    private final Long ringId;
    private final UserRole role;
    private final boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private StaffAssignmentResponse(StaffAssignment assignment) {
        this.assignmentId = assignment.getId();
        this.accountId = assignment.getAccountId();
        this.tournamentId = assignment.getTournamentId();
        this.ringId = assignment.getRingId();
        this.role = assignment.getRole();
        this.active = assignment.isActive();
        this.createdAt = assignment.getCreatedAt();
        this.updatedAt = assignment.getUpdatedAt();
    }

    public static StaffAssignmentResponse from(StaffAssignment assignment) {
        return new StaffAssignmentResponse(assignment);
    }

    public Long getAssignmentId() { return assignmentId; }
    public Long getAccountId() { return accountId; }
    public Long getTournamentId() { return tournamentId; }
    public Long getRingId() { return ringId; }
    public UserRole getRole() { return role; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
