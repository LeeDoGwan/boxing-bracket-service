package com.boxing.bracket.tournament.admin.dto;

import com.boxing.bracket.tournament.domain.Tournament;
import com.boxing.bracket.tournament.domain.TournamentStatus;

import java.time.LocalDate;

public class AdminTournamentResponse {

    private final Long tournamentId;
    private final String name;
    private final String location;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final TournamentStatus status;

    private AdminTournamentResponse(
            Long tournamentId,
            String name,
            String location,
            LocalDate startDate,
            LocalDate endDate,
            TournamentStatus status
    ) {
        this.tournamentId = tournamentId;
        this.name = name;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public static AdminTournamentResponse from(Tournament tournament) {
        return new AdminTournamentResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getLocation(),
                tournament.getStartDate(),
                tournament.getEndDate(),
                tournament.getStatus()
        );
    }

    public Long getTournamentId() {
        return tournamentId;
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
