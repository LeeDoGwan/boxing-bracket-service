package com.boxing.bracket.athlete.service;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.athlete.dto.AthleteRequest;
import com.boxing.bracket.athlete.dto.AthleteResponse;
import com.boxing.bracket.athlete.exception.AthleteNotFoundException;
import com.boxing.bracket.athlete.repository.AthleteRepository;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.common.exception.WorkflowConflictException;
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
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminAthleteServiceTest {

    @Mock
    private AthleteRepository athleteRepository;

    @Mock
    private BoutRepository boutRepository;

    @InjectMocks
    private AdminAthleteService adminAthleteService;

    @Test
    void getAthletesScopesResultsToTournament() {
        Athlete athlete = createAthlete(10L);
        athlete.assignTournament(2L);
        given(athleteRepository.findByTournamentIdOrderByIdAsc(2L))
                .willReturn(List.of(athlete));

        List<AthleteResponse> responses = adminAthleteService.getAthletes(2L, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTournamentId()).isEqualTo(2L);
    }

    @Test
    void getAthletesSearchesWithinTournament() {
        Athlete athlete = createAthlete(10L);
        athlete.assignTournament(2L);
        given(athleteRepository.searchByTournamentId(2L, "kim"))
                .willReturn(List.of(athlete));

        List<AthleteResponse> responses = adminAthleteService.getAthletes(2L, " kim ");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTournamentId()).isEqualTo(2L);
    }

    @Test
    void getAthletesRejectsMissingTournamentId() {
        assertThatThrownBy(() -> adminAthleteService.getAthletes(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tournamentId is required");
    }

    @Test
    void getAthleteReturnsAthleteWithinTournament() {
        Athlete athlete = createAthlete(10L);
        athlete.assignTournament(2L);
        given(athleteRepository.findByIdAndTournamentId(10L, 2L)).willReturn(Optional.of(athlete));

        AthleteResponse response = adminAthleteService.getAthlete(2L, 10L);

        assertThat(response.getAthleteId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Kim Min");
    }

    @Test
    void getAthleteRejectsAthleteOutsideTournament() {
        given(athleteRepository.findByIdAndTournamentId(99L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminAthleteService.getAthlete(2L, 99L))
                .isInstanceOf(AthleteNotFoundException.class)
                .hasMessage("Athlete not found");
    }

    @Test
    void createAthleteSavesAthlete() {
        AthleteRequest request = new AthleteRequest(" Kim Min ", " Blue Gym ");
        given(athleteRepository.save(any(Athlete.class))).willAnswer(invocation -> {
            Athlete athlete = invocation.getArgument(0);
            ReflectionTestUtils.setField(athlete, "id", 10L);
            return athlete;
        });

        AthleteResponse response = adminAthleteService.createAthlete(request);

        assertThat(response.getAthleteId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Kim Min");
        assertThat(response.getAffiliation()).isEqualTo("Blue Gym");
    }

    @Test
    void updateAthleteChangesAthlete() {
        Athlete athlete = createAthlete(10L);
        athlete.assignTournament(1L);
        AthleteRequest request = new AthleteRequest("Lee Jun", "Red Gym");
        given(athleteRepository.findByIdAndTournamentId(10L, 1L)).willReturn(Optional.of(athlete));
        given(athleteRepository.save(any(Athlete.class))).willAnswer(invocation -> invocation.getArgument(0));

        AthleteResponse response = adminAthleteService.updateAthlete(1L, 10L, request);

        assertThat(response.getAthleteId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Lee Jun");
        assertThat(response.getAffiliation()).isEqualTo("Red Gym");
        assertThat(athlete.getName()).isEqualTo("Lee Jun");
    }

    @Test
    void updateAthleteRejectsMissingAthlete() {
        AthleteRequest request = new AthleteRequest("Lee Jun", "Red Gym");
        given(athleteRepository.findByIdAndTournamentId(99L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminAthleteService.updateAthlete(1L, 99L, request))
                .isInstanceOf(AthleteNotFoundException.class)
                .hasMessage("Athlete not found");
    }

    @Test
    void updateAthleteRejectsBlankName() {
        AthleteRequest request = new AthleteRequest(" ", "Red Gym");

        assertThatThrownBy(() -> adminAthleteService.updateAthlete(1L, 10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
    }

    @Test
    void updateAthleteRejectsNullAthleteId() {
        AthleteRequest request = new AthleteRequest("Lee Jun", "Red Gym");

        assertThatThrownBy(() -> adminAthleteService.updateAthlete(1L, null, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("athleteId is required");
    }

    @Test
    void deleteAthleteDeletesExistingAthlete() {
        given(athleteRepository.existsByIdAndTournamentId(10L, 1L)).willReturn(true);

        adminAthleteService.deleteAthlete(1L, 10L);

        then(athleteRepository).should().deleteById(10L);
    }

    @Test
    void deleteAthleteRejectsMissingAthlete() {
        given(athleteRepository.existsByIdAndTournamentId(99L, 1L)).willReturn(false);

        assertThatThrownBy(() -> adminAthleteService.deleteAthlete(1L, 99L))
                .isInstanceOf(AthleteNotFoundException.class)
                .hasMessage("Athlete not found");
    }

    @Test
    void deleteAthleteRejectsReferencedAthlete() {
        given(athleteRepository.existsByIdAndTournamentId(10L, 1L)).willReturn(true);
        given(boutRepository.existsByRedAthleteIdOrBlueAthleteId(10L, 10L)).willReturn(true);

        assertThatThrownBy(() -> adminAthleteService.deleteAthlete(1L, 10L))
                .isInstanceOf(WorkflowConflictException.class)
                .hasMessage("ATHLETE_DELETE_NOT_ALLOWED");
    }

    @Test
    void createAthleteRejectsNullRequest() {
        assertThatThrownBy(() -> adminAthleteService.createAthlete(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("athlete request is required");
    }

    private Athlete createAthlete(Long id) {
        Athlete athlete = Athlete.builder()
                .name("Kim Min")
                .affiliation("Blue Gym")
                .build();
        ReflectionTestUtils.setField(athlete, "id", id);
        return athlete;
    }
}
