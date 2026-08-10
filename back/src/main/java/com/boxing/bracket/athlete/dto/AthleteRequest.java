package com.boxing.bracket.athlete.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class AthleteRequest {

    @NotNull(message = "tournamentId is required")
    @Positive(message = "tournamentId must be positive")
    private Long tournamentId;

    @NotBlank(message = "name is required")
    private String name;

    private String affiliation;

    protected AthleteRequest() {
    }

    public AthleteRequest(String name, String affiliation) {
        this(1L, name, affiliation);
    }

    public AthleteRequest(Long tournamentId, String name, String affiliation) {
        this.tournamentId = tournamentId;
        this.name = name;
        this.affiliation = affiliation;
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
