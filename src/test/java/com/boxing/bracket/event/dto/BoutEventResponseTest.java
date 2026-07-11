package com.boxing.bracket.event.dto;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.event.domain.BoutEventType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoutEventResponseTest {

    @Test
    void createsEventFromBout() {
        Bout bout = createBout(10L);

        BoutEventResponse response = BoutEventResponse.of(BoutEventType.ROUND_STARTED, bout, 2);

        assertThat(response.getEventType()).isEqualTo(BoutEventType.ROUND_STARTED);
        assertThat(response.getTournamentId()).isEqualTo(1L);
        assertThat(response.getRingId()).isEqualTo(2L);
        assertThat(response.getBoutId()).isEqualTo(10L);
        assertThat(response.getBoutStatus()).isEqualTo(BoutStatus.READY);
        assertThat(response.getRoundNo()).isEqualTo(2);
        assertThat(response.getOccurredAt()).isNotNull();
    }

    @Test
    void rejectsNullBout() {
        assertThatThrownBy(() -> BoutEventResponse.of(BoutEventType.BOUT_STARTED, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("bout is required");
    }

    private Bout createBout(Long id) {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(2L)
                .boutNumber(1)
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .status(BoutStatus.READY)
                .build();
        ReflectionTestUtils.setField(bout, "id", id);
        return bout;
    }
}
