package com.boxing.bracket.scoring.service;

import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.scoring.domain.RoundScore;
import com.boxing.bracket.scoring.dto.RoundScoreResponse;
import com.boxing.bracket.scoring.repository.RoundScoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ScoreQueryServiceTest {

    @Mock
    private BoutRepository boutRepository;

    @Mock
    private RoundScoreRepository roundScoreRepository;

    @InjectMocks
    private ScoreQueryService scoreQueryService;

    @Test
    void getBoutScoresReturnsScoresInRepositoryOrder() {
        RoundScore firstScore = createSubmittedRoundScore(100L, 1L, 1, 10L, 10, 9);
        RoundScore secondScore = createSubmittedRoundScore(101L, 1L, 2, 11L, 9, 10);
        given(boutRepository.existsById(1L)).willReturn(true);
        given(roundScoreRepository.findByBoutIdOrderByRoundNoAscJudgeIdAsc(1L))
                .willReturn(List.of(firstScore, secondScore));

        List<RoundScoreResponse> responses = scoreQueryService.getBoutScores(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getScoreId()).isEqualTo(100L);
        assertThat(responses.get(0).getRoundNo()).isEqualTo(1);
        assertThat(responses.get(1).getScoreId()).isEqualTo(101L);
        assertThat(responses.get(1).getRoundNo()).isEqualTo(2);
    }

    @Test
    void getBoutScoresRejectsMissingBout() {
        given(boutRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> scoreQueryService.getBoutScores(99L))
                .isInstanceOf(BoutNotFoundException.class)
                .hasMessage("Bout not found");
    }

    @Test
    void getBoutScoresRejectsNullBoutId() {
        assertThatThrownBy(() -> scoreQueryService.getBoutScores(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("boutId is required");
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
