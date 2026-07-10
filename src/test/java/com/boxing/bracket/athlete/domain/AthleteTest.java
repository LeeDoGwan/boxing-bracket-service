package com.boxing.bracket.athlete.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AthleteTest {

    @Test
    void updateChangesNameAndAffiliation() {
        Athlete athlete = Athlete.builder()
                .name("Kim Min")
                .affiliation("Blue Gym")
                .build();

        athlete.update(" Lee Jun ", " Red Gym ");

        assertThat(athlete.getName()).isEqualTo("Lee Jun");
        assertThat(athlete.getAffiliation()).isEqualTo("Red Gym");
    }

    @Test
    void updateRejectsBlankName() {
        Athlete athlete = Athlete.builder()
                .name("Kim Min")
                .affiliation("Blue Gym")
                .build();

        assertThatThrownBy(() -> athlete.update(" ", "Red Gym"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
    }
}
