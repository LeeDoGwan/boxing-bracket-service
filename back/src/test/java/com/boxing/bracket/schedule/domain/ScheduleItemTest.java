package com.boxing.bracket.schedule.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleItemTest {

    private final LocalDateTime start = LocalDateTime.of(2026, 8, 1, 9, 0);

    @Test
    void createsScheduleWithDefaultsAndNormalizedTitle() {
        ScheduleItem item = ScheduleItem.builder()
                .tournamentId(1L)
                .type(ScheduleType.BREAK)
                .title("  Morning break  ")
                .startTime(start)
                .build();

        assertThat(item.getTitle()).isEqualTo("Morning break");
        assertThat(item.getStatus()).isEqualTo(ScheduleStatus.SCHEDULED);
        assertThat(item.getEndTime()).isNull();
    }

    @Test
    void rejectsEndTimeBeforeStartTime() {
        assertThatThrownBy(() -> ScheduleItem.builder()
                .tournamentId(1L)
                .type(ScheduleType.EVENT)
                .title("Opening")
                .startTime(start)
                .endTime(start.minusMinutes(1))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("endTime must not be before startTime");
    }

    @Test
    void updatesScheduleInformation() {
        ScheduleItem item = ScheduleItem.builder()
                .tournamentId(1L)
                .type(ScheduleType.EVENT)
                .title("Opening")
                .startTime(start)
                .build();

        item.updateInfo(
                1L,
                2L,
                ScheduleType.PERFORMANCE,
                "Closing performance",
                start.plusHours(2),
                start.plusHours(3),
                null,
                ScheduleStatus.IN_PROGRESS
        );

        assertThat(item.getType()).isEqualTo(ScheduleType.PERFORMANCE);
        assertThat(item.getTitle()).isEqualTo("Closing performance");
        assertThat(item.getRingId()).isEqualTo(2L);
        assertThat(item.getStatus()).isEqualTo(ScheduleStatus.IN_PROGRESS);
    }
}
