package com.boxing.bracket.event.dto;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.event.domain.BoutEventType;

import java.time.LocalDateTime;

public class BoutEventResponse {

    private final BoutEventType eventType;
    private final Long tournamentId;
    private final Long ringId;
    private final Long boutId;
    private final BoutStatus boutStatus;
    private final Integer roundNo;
    private final LocalDateTime occurredAt;

    private BoutEventResponse(
            BoutEventType eventType,
            Long tournamentId,
            Long ringId,
            Long boutId,
            BoutStatus boutStatus,
            Integer roundNo,
            LocalDateTime occurredAt
    ) {
        this.eventType = eventType;
        this.tournamentId = tournamentId;
        this.ringId = ringId;
        this.boutId = boutId;
        this.boutStatus = boutStatus;
        this.roundNo = roundNo;
        this.occurredAt = occurredAt;
    }

    public static BoutEventResponse of(BoutEventType eventType, Bout bout) {
        return of(eventType, bout, null);
    }

    public static BoutEventResponse of(BoutEventType eventType, Bout bout, Integer roundNo) {
        if (eventType == null) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (bout == null) {
            throw new IllegalArgumentException("bout is required");
        }
        return new BoutEventResponse(
                eventType,
                bout.getTournamentId(),
                bout.getRingId(),
                bout.getId(),
                bout.getStatus(),
                roundNo,
                LocalDateTime.now()
        );
    }

    public BoutEventType getEventType() {
        return eventType;
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public Long getRingId() {
        return ringId;
    }

    public Long getBoutId() {
        return boutId;
    }

    public BoutStatus getBoutStatus() {
        return boutStatus;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
