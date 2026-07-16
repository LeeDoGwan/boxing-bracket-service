package com.boxing.bracket.scoring.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoundScoreTest {

    @Test
    void submitStoresScoresAndMarksSubmitted() {
        RoundScore roundScore = createRoundScore();

        roundScore.submit(10, 9);

        assertThat(roundScore.getRedScore()).isEqualTo(10);
        assertThat(roundScore.getBlueScore()).isEqualTo(9);
        assertThat(roundScore.getStatus()).isEqualTo(RoundScoreStatus.SUBMITTED);
    }

    @Test
    void submitRejectsNegativeRedScore() {
        RoundScore roundScore = createRoundScore();

        assertThatThrownBy(() -> roundScore.submit(-1, 9))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submitRejectsNegativeBlueScore() {
        RoundScore roundScore = createRoundScore();

        assertThatThrownBy(() -> roundScore.submit(10, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submitRejectsScoreAboveTen() {
        RoundScore roundScore = createRoundScore();

        assertThatThrownBy(() -> roundScore.submit(11, 9))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submitAcceptsZeroScores() {
        RoundScore roundScore = createRoundScore();

        roundScore.submit(0, 0);

        assertThat(roundScore.getRedScore()).isZero();
        assertThat(roundScore.getBlueScore()).isZero();
        assertThat(roundScore.getStatus()).isEqualTo(RoundScoreStatus.SUBMITTED);
    }

    private RoundScore createRoundScore() {
        return RoundScore.builder()
                .boutId(1L)
                .roundNo(1)
                .judgeId(1L)
                .build();
    }
}
