package com.boxing.bracket.scoring.repository;

import com.boxing.bracket.scoring.domain.RoundScore;
import com.boxing.bracket.scoring.domain.RoundScoreStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RoundScoreRepositoryTest {

    @Autowired
    private RoundScoreRepository roundScoreRepository;

    @Test
    void findsRoundScoresByBoutAndJudge() {
        RoundScore roundScore = RoundScore.builder()
                .boutId(1L)
                .roundNo(1)
                .judgeId(10L)
                .build();
        roundScore.submit(10, 9);
        roundScoreRepository.saveAndFlush(roundScore);

        assertThat(roundScoreRepository.findByBoutId(1L)).hasSize(1);
        assertThat(roundScoreRepository.findByBoutIdAndJudgeId(1L, 10L)).hasSize(1);
        assertThat(roundScoreRepository.findByBoutIdAndJudgeId(1L, 10L).get(0).getStatus())
                .isEqualTo(RoundScoreStatus.SUBMITTED);
    }
}
