package com.boxing.bracket.tournament.admin.service;

import com.boxing.bracket.tournament.admin.dto.AdminTournamentRequest;
import com.boxing.bracket.tournament.admin.dto.AdminTournamentResponse;
import com.boxing.bracket.tournament.domain.Tournament;
import com.boxing.bracket.tournament.domain.TournamentStatus;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminTournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private AdminTournamentService adminTournamentService;

    @Test
    void createTournamentSavesTournament() {
        given(tournamentRepository.save(any(Tournament.class))).willAnswer(invocation -> {
            Tournament tournament = invocation.getArgument(0);
            ReflectionTestUtils.setField(tournament, "id", 1L);
            return tournament;
        });

        AdminTournamentResponse response = adminTournamentService.createTournament(request());

        assertThat(response.getTournamentId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Seoul Cup");
        assertThat(response.getLocation()).isEqualTo("Seoul");
        assertThat(response.getStatus()).isEqualTo(TournamentStatus.READY);
    }

    @Test
    void createTournamentRejectsBlankName() {
        AdminTournamentRequest request = new AdminTournamentRequest(" ", "Seoul", null, null, null);

        assertThatThrownBy(() -> adminTournamentService.createTournament(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
    }

    @Test
    void createTournamentRejectsInvalidDateRange() {
        AdminTournamentRequest request = new AdminTournamentRequest(
                "Seoul Cup",
                "Seoul",
                LocalDate.of(2026, 8, 2),
                LocalDate.of(2026, 8, 1),
                null
        );

        assertThatThrownBy(() -> adminTournamentService.createTournament(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("endDate must not be before startDate");
    }

    @Test
    void updateTournamentChangesTournament() {
        Tournament tournament = createTournament(1L);
        AdminTournamentRequest request = new AdminTournamentRequest(
                "Busan Cup",
                "Busan",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 2),
                TournamentStatus.IN_PROGRESS
        );
        given(tournamentRepository.findById(1L)).willReturn(Optional.of(tournament));
        given(tournamentRepository.save(any(Tournament.class))).willAnswer(invocation -> invocation.getArgument(0));

        AdminTournamentResponse response = adminTournamentService.updateTournament(1L, request);

        assertThat(response.getTournamentId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Busan Cup");
        assertThat(response.getLocation()).isEqualTo("Busan");
        assertThat(response.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);
    }

    @Test
    void updateTournamentRejectsMissingTournament() {
        given(tournamentRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminTournamentService.updateTournament(99L, request()))
                .isInstanceOf(TournamentNotFoundException.class)
                .hasMessage("Tournament not found");
    }

    @Test
    void updateTournamentRejectsNullTournamentId() {
        assertThatThrownBy(() -> adminTournamentService.updateTournament(null, request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tournamentId is required");
    }

    @Test
    void deleteTournamentDeletesExistingTournament() {
        given(tournamentRepository.existsById(1L)).willReturn(true);

        adminTournamentService.deleteTournament(1L);

        then(tournamentRepository).should().deleteById(1L);
    }

    @Test
    void deleteTournamentRejectsMissingTournament() {
        given(tournamentRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> adminTournamentService.deleteTournament(99L))
                .isInstanceOf(TournamentNotFoundException.class)
                .hasMessage("Tournament not found");
    }

    private AdminTournamentRequest request() {
        return new AdminTournamentRequest(
                " Seoul Cup ",
                " Seoul ",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2),
                null
        );
    }

    private Tournament createTournament(Long id) {
        Tournament tournament = Tournament.builder()
                .name("Seoul Cup")
                .location("Seoul")
                .status(TournamentStatus.READY)
                .build();
        ReflectionTestUtils.setField(tournament, "id", id);
        return tournament;
    }
}
