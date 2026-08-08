package com.boxing.bracket.tournament.domain;

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
import java.time.LocalDate;

@Getter
@Entity
@Table(name = "tournaments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tournament extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String location;

    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentStatus status = TournamentStatus.READY;

    @Column(nullable = false)
    private Integer judgeCount = 3;

    @Builder
    private Tournament(String name, String location, LocalDate startDate, LocalDate endDate, TournamentStatus status, Integer judgeCount) {
        validateName(name);
        validateDateRange(startDate, endDate);
        validateJudgeCount(judgeCount);
        this.name = name.trim();
        this.location = normalizeText(location);
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status == null ? TournamentStatus.READY : status;
        this.judgeCount = judgeCount == null ? 3 : judgeCount;
    }

    public void updateInfo(
            String name,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            TournamentStatus status
    ) {
        updateInfo(name, location, startDate, endDate, status, this.judgeCount);
    }

    public void updateInfo(
            String name,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            TournamentStatus status,
            Integer judgeCount
    ) {
        validateName(name);
        validateDateRange(startDate, endDate);
        validateJudgeCount(judgeCount);
        this.name = name.trim();
        this.location = normalizeText(location);
        this.startDate = startDate;
        this.endDate = endDate;
        if (status != null) {
            this.status = status;
        }
        this.judgeCount = judgeCount == null ? 3 : judgeCount;
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
    }

    private void validateJudgeCount(Integer judgeCount) {
        if (judgeCount != null && judgeCount != 3 && judgeCount != 5) {
            throw new IllegalArgumentException("judgeCount must be 3 or 5");
        }
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }
}
