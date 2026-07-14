package com.boxing.bracket.scoring.domain;

import com.boxing.bracket.bout.domain.BoutSide;
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
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "bout_results",
        uniqueConstraints = @UniqueConstraint(name = "uk_bout_results_bout", columnNames = "bout_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoutResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "bout_id", nullable = false)
    private Long boutId;

    private Integer redTotalScore;

    private Integer blueTotalScore;

    private Integer redPenaltyTotal;

    private Integer bluePenaltyTotal;

    @Enumerated(EnumType.STRING)
    private BoutSide winnerSide;

    @Enumerated(EnumType.STRING)
    private DecisionType decisionType;

    @Column(nullable = false)
    private Long confirmedBy;

    private LocalDateTime confirmedAt;

    @Builder
    private BoutResult(
            Long boutId,
            Integer redTotalScore,
            Integer blueTotalScore,
            Integer redPenaltyTotal,
            Integer bluePenaltyTotal,
            BoutSide winnerSide,
            DecisionType decisionType,
            Long confirmedBy,
            LocalDateTime confirmedAt
    ) {
        this.boutId = boutId;
        this.redTotalScore = redTotalScore;
        this.blueTotalScore = blueTotalScore;
        this.redPenaltyTotal = redPenaltyTotal;
        this.bluePenaltyTotal = bluePenaltyTotal;
        this.winnerSide = winnerSide;
        this.decisionType = decisionType == null ? DecisionType.UNKNOWN : decisionType;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = confirmedAt;
    }

    public void confirm(
            Integer redTotalScore,
            Integer blueTotalScore,
            Integer redPenaltyTotal,
            Integer bluePenaltyTotal,
            BoutSide winnerSide,
            DecisionType decisionType,
            Long confirmedBy
    ) {
        validateTotal(redTotalScore, "redTotalScore");
        validateTotal(blueTotalScore, "blueTotalScore");
        validateTotal(redPenaltyTotal, "redPenaltyTotal");
        validateTotal(bluePenaltyTotal, "bluePenaltyTotal");
        this.redTotalScore = redTotalScore;
        this.blueTotalScore = blueTotalScore;
        this.redPenaltyTotal = redPenaltyTotal;
        this.bluePenaltyTotal = bluePenaltyTotal;
        this.winnerSide = winnerSide;
        this.decisionType = decisionType == null ? DecisionType.UNKNOWN : decisionType;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = LocalDateTime.now();
    }

    public boolean matchesConfirmation(BoutSide winnerSide, DecisionType decisionType, Long confirmedBy) {
        return this.winnerSide == winnerSide
                && this.decisionType == (decisionType == null ? DecisionType.UNKNOWN : decisionType)
                && this.confirmedBy.equals(confirmedBy);
    }

    private void validateTotal(Integer total, String fieldName) {
        if (total != null && total < 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than or equal to 0");
        }
    }
}
