package com.boxing.bracket.scoring.service;

import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class JudgeScoreServiceTest {

    @Mock
    private BoutRepository boutRepository;

    @Mock
    private RoundScoreRepository roundScoreRepository;

    @InjectMocks
    private JudgeScoreService judgeScoreService;

    @Test
    void submitRoundScoreCreatesNewSubmittedScore() {
        RoundScoreSubmitRequest request = new RoundScoreSubmitRequest(10L, 10, 9);
        given(boutRepository.existsById(1L)).willReturn(true);
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
        given(boutRepository.existsById(1L)).willReturn(true);
        given(roundScoreRepository.findByBoutIdAndRoundNoAndJudgeId(1L, 1, 10L))
                .willReturn(Optional.of(existingScore));
        given(roundScoreRepository.save(any(RoundScore.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        RoundScoreResponse response = judgeScoreService.submitRoundScore(1L, 1, request);

        assertThat(response.getScoreId()).isEqualTo(99L);
        assertThat(response.getRedScore()).isEqualTo(9);
        assertThat(response.getBlueScore()).isEqualTo(10);
        assertThat(response.getStatus()).isEqualTo(RoundScoreStatus.SUBMITTED);
    }

    @Test
    void submitRoundScoreRejectsMissingBout() {
        RoundScoreSubmitRequest request = new RoundScoreSubmitRequest(10L, 10, 9);
        given(boutRepository.existsById(99L)).willReturn(false);

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
        given(boutRepository.existsById(1L)).willReturn(true);
        given(roundScoreRepository.findByBoutIdAndRoundNoAndJudgeId(1L, 1, 10L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> judgeScoreService.submitRoundScore(1L, 1, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Score must be greater than or equal to 0");
    }

    @Test
    void getBoutScoresReturnsScoresInRepositoryOrder() {
        RoundScore firstScore = createSubmittedRoundScore(100L, 1L, 1, 10L, 10, 9);
        RoundScore secondScore = createSubmittedRoundScore(101L, 1L, 2, 11L, 9, 10);
        given(boutRepository.existsById(1L)).willReturn(true);
        given(roundScoreRepository.findByBoutIdOrderByRoundNoAscJudgeIdAsc(1L))
                .willReturn(List.of(firstScore, secondScore));

        List<RoundScoreResponse> responses = judgeScoreService.getBoutScores(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getScoreId()).isEqualTo(100L);
        assertThat(responses.get(0).getRoundNo()).isEqualTo(1);
        assertThat(responses.get(1).getScoreId()).isEqualTo(101L);
        assertThat(responses.get(1).getRoundNo()).isEqualTo(2);
    }

    @Test
    void getBoutScoresRejectsMissingBout() {
        given(boutRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> judgeScoreService.getBoutScores(99L))
                .isInstanceOf(BoutNotFoundException.class)
                .hasMessage("Bout not found");
    }

    private RoundScore createSubmittedRoundScore(Long id, Long boutId, Integer roundNo, Long judgeId, int redScore, int blueScore) {
        RoundScore roundScore = RoundScore.builder()
                .boutId(boutId)
                .roundNo(roundNo)
                .judgeId(judgeId)
                .build();
        ReflectionTestUtils.setField(roundScore, "id", id);
        roundScore.submit(redScore, blueScore);
        return roundScore;
    }
}
