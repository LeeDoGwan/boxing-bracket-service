package com.boxing.bracket.operation.dto;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OperationBoutResponse {

    private final Long boutId;
    private final Integer boutNumber;
    private final String matchType;
    private final BoutStatus status;
    private final Integer currentRound;
    private final Integer totalRounds;
    private final Integer scheduledOrder;
    private final boolean resultConfirmed;
    private final LocalDateTime startedAt;

    private OperationBoutResponse(
            Long boutId,
            Integer boutNumber,
            String matchType,
            BoutStatus status,
            Integer currentRound,
            Integer totalRounds,
            Integer scheduledOrder,
            boolean resultConfirmed,
            LocalDateTime startedAt
    ) {
        this.boutId = boutId;
        this.boutNumber = boutNumber;
        this.matchType = matchType;
        this.status = status;
        this.currentRound = currentRound;
        this.totalRounds = totalRounds;
        this.scheduledOrder = scheduledOrder;
        this.resultConfirmed = resultConfirmed;
        this.startedAt = startedAt;
    }

    public static OperationBoutResponse from(Bout bout) {
        return new OperationBoutResponse(
                bout.getId(),
                bout.getBoutNumber(),
                bout.getMatchType(),
                bout.getStatus(),
                bout.getCurrentRound(),
                bout.getTotalRounds(),
                bout.getScheduledOrder(),
                bout.isResultConfirmed(),
                bout.getStartedAt()
        );
    }
}
