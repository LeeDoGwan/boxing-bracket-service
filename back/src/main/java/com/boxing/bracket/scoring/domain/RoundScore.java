package com.boxing.bracket.scoring.domain;

import com.boxing.bracket.common.entity.BaseTimeEntity;
import com.boxing.bracket.common.exception.WorkflowConflictException;
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
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "round_scores",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_round_scores_bout_round_judge",
                columnNames = {"bout_id", "round_no", "judge_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoundScore extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "bout_id", nullable = false)
    private Long boutId;

    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    @Column(name = "judge_id", nullable = false)
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

    public boolean submit(int redScore, int blueScore) {
        validateScore(redScore);
        validateScore(blueScore);
        if (this.status == RoundScoreStatus.SUBMITTED) {
            if (isSameScore(redScore, blueScore)) {
                return false;
            }
            throw new WorkflowConflictException("SCORE_ALREADY_SUBMITTED");
        }
        this.redScore = redScore;
        this.blueScore = blueScore;
        this.status = RoundScoreStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        return true;
    }

    private boolean isSameScore(int redScore, int blueScore) {
        return Integer.valueOf(redScore).equals(this.redScore)
                && Integer.valueOf(blueScore).equals(this.blueScore);
    }

    private void validateScore(int score) {
        if (score < 0 || score > 10) {
            throw new IllegalArgumentException("INVALID_SCORE_VALUE");
        }
    }
}
