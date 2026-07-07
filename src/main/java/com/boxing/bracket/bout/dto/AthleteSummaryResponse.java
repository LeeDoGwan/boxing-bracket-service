package com.boxing.bracket.bout.dto;

import com.boxing.bracket.athlete.domain.Athlete;

public class AthleteSummaryResponse {

    private final Long athleteId;
    private final String name;
    private final String affiliation;

    private AthleteSummaryResponse(Long athleteId, String name, String affiliation) {
        this.athleteId = athleteId;
        this.name = name;
        this.affiliation = affiliation;
    }

    public static AthleteSummaryResponse from(Athlete athlete) {
        return new AthleteSummaryResponse(
                athlete.getId(),
                athlete.getName(),
                athlete.getAffiliation()
        );
    }

    public Long getAthleteId() {
        return athleteId;
    }

    public String getName() {
        return name;
    }

    public String getAffiliation() {
        return affiliation;
    }
}
