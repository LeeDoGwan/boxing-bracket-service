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

@Getter
@Entity
@Table(name = "penalties")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Penalty extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long boutId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoutSide targetSide;

    @Column(name = "round_no")
    private Integer roundNo;

    @Column(nullable = false)
    private Integer penaltyPoint;

    private String reason;

    @Column(nullable = false)
    private Long createdBy;

    @Builder
    private Penalty(Long boutId, BoutSide targetSide, Integer roundNo, Integer penaltyPoint, String reason, Long createdBy) {
        if (penaltyPoint == null || penaltyPoint < 1) {
            throw new IllegalArgumentException("INVALID_PENALTY_VALUE");
        }
        this.boutId = boutId;
        this.targetSide = targetSide;
        this.roundNo = roundNo;
        this.penaltyPoint = penaltyPoint;
        this.reason = reason;
        this.createdBy = createdBy;
    }
}
