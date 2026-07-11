package com.boxing.bracket.scoring.service;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.event.dto.BoutEventResponse;
import com.boxing.bracket.event.service.BoutEventPublisher;
import com.boxing.bracket.scoring.domain.RoundScore;
import com.boxing.bracket.scoring.domain.RoundScoreStatus;
import com.boxing.bracket.scoring.dto.RoundScoreResponse;
import com.boxing.bracket.scoring.dto.RoundScoreSubmitRequest;
import com.boxing.bracket.scoring.repository.RoundScoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class JudgeScoreServiceTest {

    @Mock
    private BoutRepository boutRepository;

    @Mock
    private RoundScoreRepository roundScoreRepository;

    @Mock
    private BoutEventPublisher boutEventPublisher;

    @InjectMocks
    private JudgeScoreService judgeScoreService;

    @Test
    void submitRoundScoreCreatesNewSubmittedScore() {
        RoundScoreSubmitRequest request = new RoundScoreSubmitRequest(10L, 10, 9);
        given(boutRepository.findById(1L)).willReturn(Optional.of(createBout(1L)));
        given(roundScoreRepository.findByBoutIdAndRoundNoAndJudgeId(1L, 1, 10L))
                .willReturn(Optional.empty());
        given(roundScoreRepository.save(any(RoundScore.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        RoundScoreResponse response = judgeScoreService.submitRoundScore(1L, 1, request);

        assertThat(response.getBoutId()).isEqualTo(1L);
        assertThat(response.getRoundNo()).isEqualTo(1);
        assertThat(response.getJudgeId()).isEqualTo(10L);
        assertThat(response.getRedScore()).isEqualTo(10);
        assertThat(response.getBlueScore()).isEqualTo(9);
        assertThat(response.getStatus()).isEqualTo(RoundScoreStatus.SUBMITTED);
        assertThat(response.getSubmittedAt()).isNotNull();
        then(boutEventPublisher).should().publish(any(BoutEventResponse.class));
    }

    @Test
    void submitRoundScoreUpdatesExistingScoreForSameJudgeAndRound() {
        RoundScore existingScore = RoundScore.builder()
                .boutId(1L)
                .roundNo(1)
                .judgeId(10L)
                .build();
        ReflectionTestUtils.setField(existingScore, "id", 99L);

        RoundScoreSubmitRequest request = new RoundScoreSubmitRequest(10L, 9, 10);
        given(boutRepository.findById(1L)).willReturn(Optional.of(createBout(1L)));
        given(roundScoreRepository.findByBoutIdAndRoundNoAndJudgeId(1L, 1, 10L))
                .willReturn(Optional.of(existingScore));
        given(roundScoreRepository.save(any(RoundScore.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        RoundScoreResponse response = judgeScoreService.submitRoundScore(1L, 1, request);

        assertThat(response.getScoreId()).isEqualTo(99L);
        assertThat(response.getRedScore()).isEqualTo(9);
        assertThat(response.getBlueScore()).isEqualTo(10);
        assertThat(response.getStatus()).isEqualTo(RoundScoreStatus.SUBMITTED);
        then(boutEventPublisher).should().publish(any(BoutEventResponse.class));
    }

    @Test
    void submitRoundScoreRejectsMissingBout() {
        RoundScoreSubmitRequest request = new RoundScoreSubmitRequest(10L, 10, 9);
        given(boutRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> judgeScoreService.submitRoundScore(99L, 1, request))
                .isInstanceOf(BoutNotFoundException.class)
                .hasMessage("Bout not found");
    }

    @Test
    void submitRoundScoreRejectsNullJudgeId() {
        RoundScoreSubmitRequest request = new RoundScoreSubmitRequest(null, 10, 9);

        assertThatThrownBy(() -> judgeScoreService.submitRoundScore(1L, 1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("judgeId is required");
    }

    @Test
    void submitRoundScoreRejectsInvalidRoundNo() {
        RoundScoreSubmitRequest request = new RoundScoreSubmitRequest(10L, 10, 9);

        assertThatThrownBy(() -> judgeScoreService.submitRoundScore(1L, 0, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roundNo must be greater than or equal to 1");
    }

    @Test
    void submitRoundScoreRejectsNegativeScore() {
        RoundScoreSubmitRequest request = new RoundScoreSubmitRequest(10L, -1, 9);
        given(boutRepository.findById(1L)).willReturn(Optional.of(createBout(1L)));
        given(roundScoreRepository.findByBoutIdAndRoundNoAndJudgeId(1L, 1, 10L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> judgeScoreService.submitRoundScore(1L, 1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Score must be greater than or equal to 0");
    }

    private Bout createBout(Long id) {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(1)
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .status(BoutStatus.IN_PROGRESS)
                .build();
        ReflectionTestUtils.setField(bout, "id", id);
        return bout;
    }

}
