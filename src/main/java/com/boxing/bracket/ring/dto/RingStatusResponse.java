package com.boxing.bracket.ring.dto;

import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.domain.RingStatus;

public class RingStatusResponse {

    private final Long ringId;
    private final String ringName;
    private final RingStatus ringStatus;
    private final RingBoutSummaryResponse currentBout;
    private final RingBoutSummaryResponse nextBout;

    private RingStatusResponse(
            Long ringId,
            String ringName,
            RingStatus ringStatus,
            RingBoutSummaryResponse currentBout,
            RingBoutSummaryResponse nextBout
    ) {
        this.ringId = ringId;
        this.ringName = ringName;
        this.ringStatus = ringStatus;
        this.currentBout = currentBout;
        this.nextBout = nextBout;
    }

    public static RingStatusResponse of(
            Ring ring,
            RingBoutSummaryResponse currentBout,
            RingBoutSummaryResponse nextBout
    ) {
        return new RingStatusResponse(
                ring.getId(),
                ring.getName(),
                ring.getStatus(),
                currentBout,
                nextBout
        );
    }

    public Long getRingId() {
        return ringId;
    }

    public String getRingName() {
        return ringName;
    }

    public RingStatus getRingStatus() {
        return ringStatus;
    }

    public RingBoutSummaryResponse getCurrentBout() {
        return currentBout;
    }

    public RingBoutSummaryResponse getNextBout() {
        return nextBout;
    }
}
