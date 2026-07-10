package com.boxing.bracket.tournament.admin.dto;

import com.boxing.bracket.tournament.domain.TournamentStatus;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

public class AdminTournamentRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String location;

    private LocalDate startDate;

    private LocalDate endDate;

    private TournamentStatus status;

    protected AdminTournamentRequest() {
    }

    public AdminTournamentRequest(
            String name,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            TournamentStatus status
    ) {
        this.name = name;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public TournamentStatus getStatus() {
        return status;
    }
}
