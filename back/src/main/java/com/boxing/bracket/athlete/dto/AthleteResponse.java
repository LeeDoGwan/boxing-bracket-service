package com.boxing.bracket.athlete.dto;

import com.boxing.bracket.athlete.domain.Athlete;

public class AthleteResponse {

    private final Long athleteId;
    private final String name;
    private final String affiliation;

    private AthleteResponse(Long athleteId, String name, String affiliation) {
        this.athleteId = athleteId;
        this.name = name;
        this.affiliation = affiliation;
    }

    public static AthleteResponse from(Athlete athlete) {
        return new AthleteResponse(
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
