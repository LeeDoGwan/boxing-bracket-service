package com.boxing.bracket.user.domain;

import com.boxing.bracket.common.entity.BaseTimeEntity;
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

@Getter
@Entity
@Table(name = "accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Builder
    private Account(String loginId, String passwordHash, String name, UserRole role, AccountStatus status) {
        validate(loginId, passwordHash, name, role);
        this.loginId = loginId.trim();
        this.passwordHash = passwordHash.trim();
        this.name = name.trim();
        this.role = role;
        this.status = status == null ? AccountStatus.ACTIVE : status;
    }

    public void updateInfo(
            String loginId,
            String passwordHash,
            String name,
            UserRole role,
            AccountStatus status
    ) {
        validate(loginId, passwordHash, name, role);
        this.loginId = loginId.trim();
        this.passwordHash = passwordHash.trim();
        this.name = name.trim();
        this.role = role;
        if (status != null) {
            this.status = status;
        }
    }

    private void validate(String loginId, String passwordHash, String name, UserRole role) {
        if (loginId == null || loginId.trim().isEmpty()) {
            throw new IllegalArgumentException("loginId is required");
        }
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("passwordHash is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
    }
}
