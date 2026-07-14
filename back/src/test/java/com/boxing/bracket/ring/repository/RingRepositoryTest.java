package com.boxing.bracket.ring.repository;

import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.domain.RingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RingRepositoryTest {

    @Autowired
    private RingRepository ringRepository;

    @Test
    void savesAndFindsRing() {
        Ring saved = ringRepository.saveAndFlush(Ring.builder()
                .tournamentId(1L)
                .name("Ring 1")
                .status(RingStatus.READY)
                .build());

        Ring found = ringRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTournamentId()).isEqualTo(1L);
        assertThat(found.getName()).isEqualTo("Ring 1");
        assertThat(found.getStatus()).isEqualTo(RingStatus.READY);
    }
}
