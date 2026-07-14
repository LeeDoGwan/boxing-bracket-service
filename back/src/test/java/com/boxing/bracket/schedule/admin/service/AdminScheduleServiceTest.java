package com.boxing.bracket.schedule.admin.service;

import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.domain.RingStatus;
import com.boxing.bracket.ring.repository.RingRepository;
import com.boxing.bracket.schedule.admin.dto.ScheduleRequest;
import com.boxing.bracket.schedule.domain.ScheduleItem;
import com.boxing.bracket.schedule.domain.ScheduleStatus;
import com.boxing.bracket.schedule.domain.ScheduleType;
import com.boxing.bracket.schedule.dto.ScheduleResponse;
import com.boxing.bracket.schedule.exception.ScheduleItemNotFoundException;
import com.boxing.bracket.schedule.repository.ScheduleItemRepository;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminScheduleServiceTest {

    @Mock
    private ScheduleItemRepository scheduleItemRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private RingRepository ringRepository;

    @Mock
    private com.boxing.bracket.bout.repository.BoutRepository boutRepository;

    @InjectMocks
    private AdminScheduleService adminScheduleService;

    @Test
    void getSchedulesReturnsTournamentItems() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(scheduleItemRepository.findByTournamentIdOrderByStartTimeAscIdAsc(1L))
                .willReturn(List.of(createItem(1L)));

        assertThat(adminScheduleService.getSchedules(1L)).extracting(ScheduleResponse::getScheduleId).containsExactly(1L);
    }

    @Test
    void createScheduleSavesValidatedItem() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(scheduleItemRepository.save(any(ScheduleItem.class))).willAnswer(invocation -> {
            ScheduleItem item = invocation.getArgument(0);
            ReflectionTestUtils.setField(item, "id", 3L);
            return item;
        });

        ScheduleResponse response = adminScheduleService.createSchedule(request(null, null));

        assertThat(response.getScheduleId()).isEqualTo(3L);
        assertThat(response.getTitle()).isEqualTo("Opening");
    }

    @Test
    void createScheduleRejectsRingFromAnotherTournament() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        Ring ring = Ring.builder().tournamentId(2L).name("Ring B").status(RingStatus.READY).build();
        given(ringRepository.findById(8L)).willReturn(Optional.of(ring));

        assertThatThrownBy(() -> adminScheduleService.createSchedule(request(8L, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ring does not belong to tournament");
    }

    @Test
    void updateScheduleChangesExistingItem() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        ScheduleItem item = createItem(3L);
        given(scheduleItemRepository.findById(3L)).willReturn(Optional.of(item));
        given(scheduleItemRepository.save(any(ScheduleItem.class))).willAnswer(invocation -> invocation.getArgument(0));

        ScheduleResponse response = adminScheduleService.updateSchedule(3L, new ScheduleRequest(
                1L, null, ScheduleType.LUNCH, "Lunch", start().plusHours(2), start().plusHours(3), null,
                ScheduleStatus.IN_PROGRESS
        ));

        assertThat(response.getType()).isEqualTo(ScheduleType.LUNCH);
        assertThat(response.getStatus()).isEqualTo(ScheduleStatus.IN_PROGRESS);
    }

    @Test
    void deleteScheduleRejectsMissingItem() {
        given(scheduleItemRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> adminScheduleService.deleteSchedule(99L))
                .isInstanceOf(ScheduleItemNotFoundException.class)
                .hasMessage("Schedule item not found");
        then(scheduleItemRepository).shouldHaveNoMoreInteractions();
    }

    private ScheduleRequest request(Long ringId, Long relatedBoutId) {
        return new ScheduleRequest(
                1L, ringId, ScheduleType.EVENT, "Opening", start(), start().plusHours(1), relatedBoutId,
                ScheduleStatus.SCHEDULED
        );
    }

    private ScheduleItem createItem(Long id) {
        ScheduleItem item = ScheduleItem.builder()
                .tournamentId(1L)
                .type(ScheduleType.EVENT)
                .title("Opening")
                .startTime(start())
                .build();
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private LocalDateTime start() {
        return LocalDateTime.of(2026, 8, 1, 9, 0);
    }
}
