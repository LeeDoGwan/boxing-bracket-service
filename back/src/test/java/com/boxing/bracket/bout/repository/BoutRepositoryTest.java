package com.boxing.bracket.bout.repository;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class BoutRepositoryTest {

    @Autowired
    private BoutRepository boutRepository;

    @Test
    void findsBoutsByTournamentAndRingInScheduledOrder() {
        Bout second = createBout(1L, 1L, 2, 2, BoutStatus.SCHEDULED);
        Bout first = createBout(1L, 1L, 1, 1, BoutStatus.IN_PROGRESS);
        boutRepository.saveAll(List.of(second, first));
        boutRepository.flush();

        List<Bout> tournamentBouts = boutRepository.findByTournamentIdOrderByScheduledOrderAsc(1L);
        List<Bout> ringBouts = boutRepository.findByRingIdOrderByScheduledOrderAsc(1L);
        Optional<Bout> currentBout = boutRepository.findFirstByRingIdAndStatusOrderByScheduledOrderAsc(1L, BoutStatus.IN_PROGRESS);

        assertThat(tournamentBouts).extracting(Bout::getScheduledOrder).containsExactly(1, 2);
        assertThat(ringBouts).extracting(Bout::getScheduledOrder).containsExactly(1, 2);
        assertThat(currentBout).isPresent();
        assertThat(currentBout.get().getBoutNumber()).isEqualTo(1);
    }

    private Bout createBout(Long tournamentId, Long ringId, int boutNumber, int scheduledOrder, BoutStatus status) {
        return Bout.builder()
                .tournamentId(tournamentId)
                .ringId(ringId)
                .boutNumber(boutNumber)
                .matchType("75 - middle school")
                .redAthleteId((long) boutNumber)
                .blueAthleteId((long) boutNumber + 10)
                .status(status)
                .totalRounds(3)
                .scheduledOrder(scheduledOrder)
                .build();
    }
}
