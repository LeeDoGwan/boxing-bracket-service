package com.boxing.bracket.bout.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
