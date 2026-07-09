package com.boxing.bracket.bout.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoutTest {

    @Test
    void startChangesStatusToInProgress() {
        Bout bout = createBout();

        bout.start();

        assertThat(bout.getStatus()).isEqualTo(BoutStatus.IN_PROGRESS);
    }

    @Test
    void confirmResultSetsWinnerAndConfirmedFlag() {
        Bout bout = createBout();

        bout.confirmResult(BoutSide.RED);

        assertThat(bout.getWinnerSide()).isEqualTo(BoutSide.RED);
        assertThat(bout.isResultConfirmed()).isTrue();
    }

    @Test
    void changeStatusUpdatesStatusAndTimestamps() {
        Bout bout = createBout();

        bout.changeStatus(BoutStatus.IN_PROGRESS);

        assertThat(bout.getStatus()).isEqualTo(BoutStatus.IN_PROGRESS);
        assertThat(bout.getStartedAt()).isNotNull();

        bout.changeStatus(BoutStatus.FINISHED);

        assertThat(bout.getStatus()).isEqualTo(BoutStatus.FINISHED);
        assertThat(bout.getEndedAt()).isNotNull();
    }

    @Test
    void updateScheduleChangesScheduleFields() {
        Bout bout = createBout();

        bout.updateSchedule(2L, 3L, 4, " 80 - high school ", 5L, 6L, 4, 7, true);

        assertThat(bout.getTournamentId()).isEqualTo(2L);
        assertThat(bout.getRingId()).isEqualTo(3L);
        assertThat(bout.getBoutNumber()).isEqualTo(4);
        assertThat(bout.getMatchType()).isEqualTo("80 - high school");
        assertThat(bout.getRedAthleteId()).isEqualTo(5L);
        assertThat(bout.getBlueAthleteId()).isEqualTo(6L);
        assertThat(bout.getTotalRounds()).isEqualTo(4);
        assertThat(bout.getScheduledOrder()).isEqualTo(7);
        assertThat(bout.isEventBout()).isTrue();
    }

    @Test
    void updateScheduleRejectsSameAthlete() {
        Bout bout = createBout();

        assertThatThrownBy(() -> bout.updateSchedule(1L, 1L, 1, "75", 10L, 10L, 3, 1, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("redAthleteId and blueAthleteId must be different");
    }

    private Bout createBout() {
        return Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(1)
                .matchType("75 - middle school")
                .redAthleteId(1L)
                .blueAthleteId(2L)
                .totalRounds(3)
                .scheduledOrder(1)
                .build();
    }
}
