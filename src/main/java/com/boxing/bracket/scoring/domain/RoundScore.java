package com.boxing.bracket.scoring.domain;

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
@Table(name = "round_scores")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoundScore extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long boutId;

    @Column(nullable = false)
    private Integer roundNo;

    @Column(nullable = false)
    private Long judgeId;

    private Integer redScore;

    private Integer blueScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundScoreStatus status = RoundScoreStatus.DRAFT;

    private LocalDateTime submittedAt;

    @Builder
    private RoundScore(Long boutId, Integer roundNo, Long judgeId, Integer redScore, Integer blueScore, RoundScoreStatus status, LocalDateTime submittedAt) {
        this.boutId = boutId;
        this.roundNo = roundNo;
        this.judgeId = judgeId;
        this.redScore = redScore;
        this.blueScore = blueScore;
        this.status = status == null ? RoundScoreStatus.DRAFT : status;
        this.submittedAt = submittedAt;
    }

    public void submit(int redScore, int blueScore) {
        validateScore(redScore);
        validateScore(blueScore);
        this.redScore = redScore;
        this.blueScore = blueScore;
        this.status = RoundScoreStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    private void validateScore(int score) {
        if (score < 0) {
            throw new IllegalArgumentException("Score must be greater than or equal to 0");
        }
    }
}
