package com.boxing.bracket.scoring.repository;

import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.scoring.domain.BoutResult;
import com.boxing.bracket.scoring.domain.DecisionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class BoutResultRepositoryTest {

    @Autowired
    private BoutResultRepository boutResultRepository;

    @Test
    void rejectsDuplicateResultForSameBout() {
        boutResultRepository.saveAndFlush(createResult());

        assertThatThrownBy(() -> boutResultRepository.saveAndFlush(createResult()))
                .isInstanceOf(Exception.class);
    }

    private BoutResult createResult() {
        BoutResult result = BoutResult.builder().boutId(1L).build();
        result.confirm(10, 9, 0, 0, BoutSide.RED, DecisionType.POINTS, 20L);
        return result;
    }
}
