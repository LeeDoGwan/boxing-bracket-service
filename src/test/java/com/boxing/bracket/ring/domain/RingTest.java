package com.boxing.bracket.ring.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RingTest {

    @Test
    void assignCurrentBoutSetsBoutAndInProgressStatus() {
        Ring ring = Ring.builder()
                .tournamentId(1L)
                .name("Ring 1")
                .status(RingStatus.READY)
                .build();

        ring.assignCurrentBout(10L);

        assertThat(ring.getCurrentBoutId()).isEqualTo(10L);
        assertThat(ring.getStatus()).isEqualTo(RingStatus.IN_PROGRESS);
    }

    @Test
    void assignCurrentBoutRejectsNullBoutId() {
        Ring ring = Ring.builder()
                .tournamentId(1L)
                .name("Ring 1")
                .build();

        assertThatThrownBy(() -> ring.assignCurrentBout(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("boutId is required");
    }
}
