package com.boxing.bracket.athlete.dto;

import javax.validation.constraints.NotBlank;

public class AthleteRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String affiliation;

    protected AthleteRequest() {
    }

    public AthleteRequest(String name, String affiliation) {
        this.name = name;
        this.affiliation = affiliation;
    }

    public String getName() {
        return name;
    }

    public String getAffiliation() {
        return affiliation;
    }
}
