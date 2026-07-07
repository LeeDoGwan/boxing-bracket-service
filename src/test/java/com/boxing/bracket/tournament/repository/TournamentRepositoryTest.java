package com.boxing.bracket.tournament.repository;

import com.boxing.bracket.tournament.domain.Tournament;
import com.boxing.bracket.tournament.domain.TournamentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TournamentRepositoryTest {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Test
    void savesAndFindsTournament() {
        Tournament saved = tournamentRepository.saveAndFlush(Tournament.builder()
                .name("Seoul Boxing Cup")
                .location("Seoul")
                .status(TournamentStatus.READY)
                .build());

        Tournament found = tournamentRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getName()).isEqualTo("Seoul Boxing Cup");
        assertThat(found.getStatus()).isEqualTo(TournamentStatus.READY);
    }
}
