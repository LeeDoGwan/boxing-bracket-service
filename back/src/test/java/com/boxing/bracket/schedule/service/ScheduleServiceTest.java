package com.boxing.bracket.schedule.service;

import com.boxing.bracket.schedule.domain.ScheduleItem;
import com.boxing.bracket.schedule.domain.ScheduleType;
import com.boxing.bracket.schedule.dto.ScheduleResponse;
import com.boxing.bracket.schedule.repository.ScheduleItemRepository;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleItemRepository scheduleItemRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void getSchedulesReturnsOrderedScheduleResponses() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(scheduleItemRepository.findByTournamentIdOrderByStartTimeAscIdAsc(1L))
                .willReturn(List.of(createItem(2L, "Lunch"), createItem(1L, "Opening")));

        List<ScheduleResponse> responses = scheduleService.getSchedules(1L);

        assertThat(responses).extracting(ScheduleResponse::getScheduleId).containsExactly(2L, 1L);
        assertThat(responses.get(0).getTitle()).isEqualTo("Lunch");
    }

    @Test
    void getSchedulesRejectsMissingTournament() {
        given(tournamentRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> scheduleService.getSchedules(99L))
                .isInstanceOf(TournamentNotFoundException.class)
                .hasMessage("Tournament not found");
    }

    @Test
    void getSchedulesRejectsNullTournamentId() {
        assertThatThrownBy(() -> scheduleService.getSchedules(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tournamentId is required");
    }

    private ScheduleItem createItem(Long id, String title) {
        ScheduleItem item = ScheduleItem.builder()
                .tournamentId(1L)
                .type(ScheduleType.EVENT)
                .title(title)
                .startTime(LocalDateTime.of(2026, 8, 1, 9, 0).plusHours(id))
                .build();
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }
}
