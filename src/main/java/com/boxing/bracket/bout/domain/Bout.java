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
        this.tournamentId = tournamentId;
        this.ringId = ringId;
        this.boutNumber = boutNumber;
        this.matchType = matchType;
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
}
