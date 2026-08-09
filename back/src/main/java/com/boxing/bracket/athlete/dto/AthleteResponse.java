package com.boxing.bracket.athlete.dto;

import com.boxing.bracket.athlete.domain.Athlete;

public class AthleteResponse {

    private final Long athleteId;
    private final Long tournamentId;
    private final String name;
    private final String affiliation;

    private AthleteResponse(Long athleteId, Long tournamentId, String name, String affiliation) {
        this.athleteId = athleteId;
        this.tournamentId = tournamentId;
        this.name = name;
        this.affiliation = affiliation;
    }

    public static AthleteResponse from(Athlete athlete) {
        return new AthleteResponse(
                athlete.getId(),
                athlete.getTournamentId(),
                athlete.getName(),
                athlete.getAffiliation()
        );
    }

    public Long getAthleteId() {
        return athleteId;
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public String getName() {
        return name;
    }

    public String getAffiliation() {
        return affiliation;
    }
}
