package com.boxing.bracket.operation.dto;

import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.domain.RingStatus;
import lombok.Getter;

@Getter
public class OperationRingStatusResponse {

    private final Long ringId;
    private final String ringName;
    private final RingStatus ringStatus;
    private final OperationBoutResponse currentBout;
    private final OperationBoutResponse nextBout;

    private OperationRingStatusResponse(
            Long ringId,
            String ringName,
            RingStatus ringStatus,
            OperationBoutResponse currentBout,
            OperationBoutResponse nextBout
    ) {
        this.ringId = ringId;
        this.ringName = ringName;
        this.ringStatus = ringStatus;
        this.currentBout = currentBout;
        this.nextBout = nextBout;
    }

    public static OperationRingStatusResponse of(
            Ring ring,
            OperationBoutResponse currentBout,
            OperationBoutResponse nextBout
    ) {
        return new OperationRingStatusResponse(
                ring.getId(),
                ring.getName(),
                ring.getStatus(),
                currentBout,
                nextBout
        );
    }
}
