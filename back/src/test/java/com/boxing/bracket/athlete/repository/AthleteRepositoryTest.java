package com.boxing.bracket.athlete.repository;

import com.boxing.bracket.athlete.domain.Athlete;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AthleteRepositoryTest {

    @Autowired
    private AthleteRepository athleteRepository;

    @Test
    void savesAndFindsAthlete() {
        Athlete athlete = Athlete.builder()
                .name("Red Boxer")
                .affiliation("Seoul Gym")
                .build();
        athlete.assignTournament(1L);
        Athlete saved = athleteRepository.saveAndFlush(athlete);

        Athlete found = athleteRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getName()).isEqualTo("Red Boxer");
        assertThat(found.getAffiliation()).isEqualTo("Seoul Gym");
    }
}
