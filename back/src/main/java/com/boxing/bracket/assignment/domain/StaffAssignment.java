package com.boxing.bracket.assignment.domain;

import com.boxing.bracket.common.entity.BaseTimeEntity;
import com.boxing.bracket.user.domain.UserRole;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Getter
@Entity
@Table(name = "staff_assignments", uniqueConstraints = @UniqueConstraint(
        name = "uk_staff_assignments_account_tournament_ring",
        columnNames = {"account_id", "tournament_id", "ring_id"}
))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffAssignment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Column(name = "ring_id", nullable = false)
    private Long ringId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Builder
    private StaffAssignment(Long accountId, Long tournamentId, Long ringId, UserRole role, Boolean active) {
        validate(accountId, tournamentId, ringId, role);
        this.accountId = accountId;
        this.tournamentId = tournamentId;
        this.ringId = ringId;
        this.role = role;
        this.active = active == null || active;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }

    private void validate(Long accountId, Long tournamentId, Long ringId, UserRole role) {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }
        if (ringId == null) {
            throw new IllegalArgumentException("ringId is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
        if (!isRingScopedRole(role)) {
            throw new IllegalArgumentException("role must be JUDGE, SUPERVISOR, or RING_MANAGER");
        }
    }

    public static boolean isRingScopedRole(UserRole role) {
        return role == UserRole.JUDGE
                || role == UserRole.SUPERVISOR
                || role == UserRole.RING_MANAGER;
    }
}
