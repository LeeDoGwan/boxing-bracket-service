package com.boxing.bracket.scoring.service;

import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.scoring.domain.Penalty;
import com.boxing.bracket.scoring.dto.PenaltyCreateRequest;
import com.boxing.bracket.scoring.dto.PenaltyResponse;
import com.boxing.bracket.scoring.repository.PenaltyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SupervisorPenaltyServiceTest {

    @Mock
    private BoutRepository boutRepository;

    @Mock
    private PenaltyRepository penaltyRepository;

    @InjectMocks
    private SupervisorPenaltyService supervisorPenaltyService;

    @Test
    void createPenaltySavesPenalty() {
        PenaltyCreateRequest request = new PenaltyCreateRequest(BoutSide.RED, 1, "warning", 20L);
        given(boutRepository.existsById(1L)).willReturn(true);
        given(penaltyRepository.save(any(Penalty.class))).willAnswer(invocation -> {
            Penalty penalty = invocation.getArgument(0);
            ReflectionTestUtils.setField(penalty, "id", 100L);
            return penalty;
        });

        PenaltyResponse response = supervisorPenaltyService.createPenalty(1L, request);

        assertThat(response.getPenaltyId()).isEqualTo(100L);
        assertThat(response.getBoutId()).isEqualTo(1L);
        assertThat(response.getTargetSide()).isEqualTo(BoutSide.RED);
        assertThat(response.getPenaltyPoint()).isEqualTo(1);
        assertThat(response.getReason()).isEqualTo("warning");
        assertThat(response.getCreatedBy()).isEqualTo(20L);
    }

    @Test
    void createPenaltyRejectsMissingBout() {
        PenaltyCreateRequest request = new PenaltyCreateRequest(BoutSide.BLUE, 1, "warning", 20L);
        given(boutRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> supervisorPenaltyService.createPenalty(99L, request))
                .isInstanceOf(BoutNotFoundException.class)
                .hasMessage("Bout not found");
    }

    @Test
    void createPenaltyRejectsNullTargetSide() {
        PenaltyCreateRequest request = new PenaltyCreateRequest(null, 1, "warning", 20L);

        assertThatThrownBy(() -> supervisorPenaltyService.createPenalty(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("targetSide is required");
    }

    @Test
    void createPenaltyRejectsNegativePenaltyPoint() {
        PenaltyCreateRequest request = new PenaltyCreateRequest(BoutSide.RED, -1, "warning", 20L);
        given(boutRepository.existsById(1L)).willReturn(true);

        assertThatThrownBy(() -> supervisorPenaltyService.createPenalty(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Penalty point must be greater than or equal to 0");
    }
}
