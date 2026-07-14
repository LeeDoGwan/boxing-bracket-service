package com.boxing.bracket.event.service;

import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BoutEventStreamServiceTest {

    @Mock
    private BoutEventPublisher boutEventPublisher;

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private BoutEventStreamService boutEventStreamService;

    @Test
    void subscribeReturnsEmitter() {
        SseEmitter emitter = new SseEmitter();
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(boutEventPublisher.subscribe(1L, 2L)).willReturn(emitter);

        SseEmitter response = boutEventStreamService.subscribe(1L, 2L);

        assertThat(response).isSameAs(emitter);
    }

    @Test
    void subscribeRejectsMissingTournament() {
        given(tournamentRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> boutEventStreamService.subscribe(99L, null))
                .isInstanceOf(TournamentNotFoundException.class)
                .hasMessage("Tournament not found");
    }

    @Test
    void subscribeRejectsNullTournamentId() {
        assertThatThrownBy(() -> boutEventStreamService.subscribe(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tournamentId is required");
    }
}
