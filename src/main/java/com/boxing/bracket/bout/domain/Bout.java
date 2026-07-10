package com.boxing.bracket.bout.domain;

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
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "bouts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bout extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tournamentId;

    @Column(nullable = false)
    private Long ringId;

    @Column(nullable = false)
    private Integer boutNumber;

    private String matchType;

    @Column(nullable = false)
    private Long redAthleteId;

    @Column(nullable = false)
    private Long blueAthleteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoutStatus status = BoutStatus.SCHEDULED;

    @Column(nullable = false)
    private Integer currentRound = 0;

    private Integer totalRounds;

    private Integer scheduledOrder;

    @Enumerated(EnumType.STRING)
    private BoutSide winnerSide;

    @Column(nullable = false)
    private boolean resultConfirmed = false;

    @Column(nullable = false)
    private boolean eventBout = false;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @Builder
    private Bout(
            Long tournamentId,
            Long ringId,
            Integer boutNumber,
            String matchType,
            Long redAthleteId,
            Long blueAthleteId,
            BoutStatus status,
            Integer currentRound,
            Integer totalRounds,
            Integer scheduledOrder,
            BoutSide winnerSide,
            boolean resultConfirmed,
            boolean eventBout,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    ) {
        validateSchedule(
                tournamentId,
                ringId,
                boutNumber,
                redAthleteId,
                blueAthleteId,
                totalRounds,
                scheduledOrder
        );
        this.tournamentId = tournamentId;
        this.ringId = ringId;
        this.boutNumber = boutNumber;
        this.matchType = normalizeText(matchType);
        this.redAthleteId = redAthleteId;
        this.blueAthleteId = blueAthleteId;
        this.status = status == null ? BoutStatus.SCHEDULED : status;
        this.currentRound = currentRound == null ? 0 : currentRound;
        this.totalRounds = totalRounds;
        this.scheduledOrder = scheduledOrder;
        this.winnerSide = winnerSide;
        this.resultConfirmed = resultConfirmed;
        this.eventBout = eventBout;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public void start() {
        this.status = BoutStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void finish(BoutSide winnerSide) {
        this.status = BoutStatus.FINISHED;
        this.winnerSide = winnerSide;
        this.endedAt = LocalDateTime.now();
    }

    public void confirmResult(BoutSide winnerSide) {
        this.winnerSide = winnerSide;
        this.resultConfirmed = true;
    }

    public void changeStatus(BoutStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        this.status = status;
        if (status == BoutStatus.IN_PROGRESS && this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
        if (status == BoutStatus.FINISHED && this.endedAt == null) {
            this.endedAt = LocalDateTime.now();
        }
    }

    public void startRound(Integer roundNo) {
        if (roundNo == null) {
            throw new IllegalArgumentException("roundNo is required");
        }
        if (roundNo < 1) {
            throw new IllegalArgumentException("roundNo must be greater than or equal to 1");
        }
        if (totalRounds != null && roundNo > totalRounds) {
            throw new IllegalArgumentException("roundNo must not exceed totalRounds");
        }
        this.currentRound = roundNo;
        if (this.status != BoutStatus.IN_PROGRESS) {
            changeStatus(BoutStatus.IN_PROGRESS);
        }
    }

    public void updateSchedule(
            Long tournamentId,
            Long ringId,
            Integer boutNumber,
            String matchType,
            Long redAthleteId,
            Long blueAthleteId,
            Integer totalRounds,
            Integer scheduledOrder,
            boolean eventBout
    ) {
        validateSchedule(
                tournamentId,
                ringId,
                boutNumber,
                redAthleteId,
                blueAthleteId,
                totalRounds,
                scheduledOrder
        );
        this.tournamentId = tournamentId;
        this.ringId = ringId;
        this.boutNumber = boutNumber;
        this.matchType = normalizeText(matchType);
        this.redAthleteId = redAthleteId;
        this.blueAthleteId = blueAthleteId;
        this.totalRounds = totalRounds;
        this.scheduledOrder = scheduledOrder;
        this.eventBout = eventBout;
    }

    private static void validateSchedule(
            Long tournamentId,
            Long ringId,
            Integer boutNumber,
            Long redAthleteId,
            Long blueAthleteId,
            Integer totalRounds,
            Integer scheduledOrder
    ) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }
        if (ringId == null) {
            throw new IllegalArgumentException("ringId is required");
        }
        if (boutNumber == null) {
            throw new IllegalArgumentException("boutNumber is required");
        }
        if (boutNumber <= 0) {
            throw new IllegalArgumentException("boutNumber must be positive");
        }
        if (redAthleteId == null) {
            throw new IllegalArgumentException("redAthleteId is required");
        }
        if (blueAthleteId == null) {
            throw new IllegalArgumentException("blueAthleteId is required");
        }
        if (redAthleteId.equals(blueAthleteId)) {
            throw new IllegalArgumentException("redAthleteId and blueAthleteId must be different");
        }
        if (totalRounds != null && totalRounds <= 0) {
            throw new IllegalArgumentException("totalRounds must be positive");
        }
        if (scheduledOrder != null && scheduledOrder <= 0) {
            throw new IllegalArgumentException("scheduledOrder must be positive");
        }
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }
}
