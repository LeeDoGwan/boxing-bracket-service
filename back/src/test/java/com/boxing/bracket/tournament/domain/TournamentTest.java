package com.boxing.bracket.tournament.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TournamentTest {

    @Test
    void updateInfoChangesTournamentFields() {
        Tournament tournament = Tournament.builder()
                .name("Seoul Cup")
                .location("Seoul")
                .status(TournamentStatus.READY)
                .build();

        tournament.updateInfo(
                " Busan Cup ",
                " Busan ",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2),
                TournamentStatus.IN_PROGRESS
        );

        assertThat(tournament.getName()).isEqualTo("Busan Cup");
        assertThat(tournament.getLocation()).isEqualTo("Busan");
        assertThat(tournament.getStartDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(tournament.getEndDate()).isEqualTo(LocalDate.of(2026, 8, 2));
        assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);
    }

    @Test
    void updateInfoRejectsEndDateBeforeStartDate() {
        Tournament tournament = Tournament.builder()
                .name("Seoul Cup")
                .location("Seoul")
                .build();

        assertThatThrownBy(() -> tournament.updateInfo(
                "Seoul Cup",
                "Seoul",
                LocalDate.of(2026, 8, 2),
                LocalDate.of(2026, 8, 1),
                TournamentStatus.READY
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("endDate must not be before startDate");
    }
}
