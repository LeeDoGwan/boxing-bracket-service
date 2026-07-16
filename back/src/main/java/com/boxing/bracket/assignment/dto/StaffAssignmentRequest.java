package com.boxing.bracket.assignment.dto;

import com.boxing.bracket.user.domain.UserRole;

import javax.validation.constraints.NotNull;

public class StaffAssignmentRequest {

    @NotNull(message = "accountId is required")
    private Long accountId;

    @NotNull(message = "tournamentId is required")
    private Long tournamentId;

    @NotNull(message = "ringId is required")
    private Long ringId;

    @NotNull(message = "role is required")
    private UserRole role;

    protected StaffAssignmentRequest() {
    }

    public StaffAssignmentRequest(Long accountId, Long tournamentId, Long ringId, UserRole role) {
        this.accountId = accountId;
        this.tournamentId = tournamentId;
        this.ringId = ringId;
        this.role = role;
    }

    public Long getAccountId() { return accountId; }
    public Long getTournamentId() { return tournamentId; }
    public Long getRingId() { return ringId; }
    public UserRole getRole() { return role; }
}
