package com.boxing.bracket.ring.dto;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;

public class RingBoutSummaryResponse {

    private final Long boutId;
    private final Integer boutNumber;
    private final String matchType;
    private final String redAthleteName;
    private final String redAthleteAffiliation;
    private final String blueAthleteName;
    private final String blueAthleteAffiliation;
    private final BoutStatus boutStatus;
    private final Integer currentRound;
    private final Integer scheduledOrder;

    private RingBoutSummaryResponse(
            Long boutId,
            Integer boutNumber,
            String matchType,
            String redAthleteName,
            String redAthleteAffiliation,
            String blueAthleteName,
            String blueAthleteAffiliation,
            BoutStatus boutStatus,
            Integer currentRound,
            Integer scheduledOrder
    ) {
        this.boutId = boutId;
        this.boutNumber = boutNumber;
        this.matchType = matchType;
        this.redAthleteName = redAthleteName;
        this.redAthleteAffiliation = redAthleteAffiliation;
        this.blueAthleteName = blueAthleteName;
        this.blueAthleteAffiliation = blueAthleteAffiliation;
        this.boutStatus = boutStatus;
        this.currentRound = currentRound;
        this.scheduledOrder = scheduledOrder;
    }

    public static RingBoutSummaryResponse of(Bout bout, Athlete redAthlete, Athlete blueAthlete) {
        return new RingBoutSummaryResponse(
                bout.getId(),
                bout.getBoutNumber(),
                bout.getMatchType(),
                redAthlete.getName(),
                redAthlete.getAffiliation(),
                blueAthlete.getName(),
                blueAthlete.getAffiliation(),
                bout.getStatus(),
                bout.getCurrentRound(),
                bout.getScheduledOrder()
        );
    }

    public Long getBoutId() {
        return boutId;
    }

    public Integer getBoutNumber() {
        return boutNumber;
    }

    public String getMatchType() {
        return matchType;
    }

    public String getRedAthleteName() {
        return redAthleteName;
    }

    public String getRedAthleteAffiliation() {
        return redAthleteAffiliation;
    }

    public String getBlueAthleteName() {
        return blueAthleteName;
    }

    public String getBlueAthleteAffiliation() {
        return blueAthleteAffiliation;
    }

    public BoutStatus getBoutStatus() {
        return boutStatus;
    }

    public Integer getCurrentRound() {
        return currentRound;
    }

    public Integer getScheduledOrder() {
        return scheduledOrder;
    }
}
