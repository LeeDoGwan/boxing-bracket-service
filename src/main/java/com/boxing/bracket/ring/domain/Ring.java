package com.boxing.bracket.ring.domain;

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
import javax.persistence.Version;

@Getter
@Entity
@Table(name = "rings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ring extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false)
    private Long tournamentId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RingStatus status = RingStatus.READY;

    private Long currentBoutId;

    @Builder
    private Ring(Long tournamentId, String name, RingStatus status, Long currentBoutId) {
        validateTournamentId(tournamentId);
        validateName(name);
        this.tournamentId = tournamentId;
        this.name = name.trim();
        this.status = status == null ? RingStatus.READY : status;
        this.currentBoutId = currentBoutId;
    }

    public void assignCurrentBout(Long boutId) {
        if (boutId == null) {
            throw new IllegalArgumentException("boutId is required");
        }
        this.currentBoutId = boutId;
        this.status = RingStatus.IN_PROGRESS;
    }

    public void prepareCurrentBout(Long boutId) {
        if (boutId == null) {
            throw new IllegalArgumentException("boutId is required");
        }
        this.currentBoutId = boutId;
        this.status = RingStatus.READY;
    }

    public void updateInfo(Long tournamentId, String name, RingStatus status) {
        validateTournamentId(tournamentId);
        validateName(name);
        this.tournamentId = tournamentId;
        this.name = name.trim();
        if (status != null) {
            this.status = status;
        }
    }

    private void validateTournamentId(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
    }
}
