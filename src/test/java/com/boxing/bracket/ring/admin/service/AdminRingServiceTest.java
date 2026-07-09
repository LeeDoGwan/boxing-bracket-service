package com.boxing.bracket.ring.admin.service;

import com.boxing.bracket.ring.admin.dto.AdminRingRequest;
import com.boxing.bracket.ring.admin.dto.AdminRingResponse;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.domain.RingStatus;
import com.boxing.bracket.ring.exception.RingNotFoundException;
import com.boxing.bracket.ring.repository.RingRepository;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import com.boxing.bracket.tournament.repository.TournamentRepository;
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
class AdminRingServiceTest {

    @Mock
    private RingRepository ringRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private AdminRingService adminRingService;

    @Test
    void getRingsReturnsTournamentRings() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(ringRepository.findByTournamentIdOrderByIdAsc(1L))
                .willReturn(List.of(createRing(10L), createRing(11L)));

        List<AdminRingResponse> responses = adminRingService.getRings(1L);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getRingId()).isEqualTo(10L);
        assertThat(responses.get(1).getRingId()).isEqualTo(11L);
    }

    @Test
    void getRingsRejectsMissingTournament() {
        given(tournamentRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> adminRingService.getRings(99L))
                .isInstanceOf(TournamentNotFoundException.class)
                .hasMessage("Tournament not found");
    }

    @Test
    void getRingReturnsRing() {
        given(ringRepository.findById(10L)).willReturn(Optional.of(createRing(10L)));

        AdminRingResponse response = adminRingService.getRing(10L);

        assertThat(response.getRingId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Ring 1");
    }

    @Test
    void getRingRejectsMissingRing() {
        given(ringRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminRingService.getRing(99L))
                .isInstanceOf(RingNotFoundException.class)
                .hasMessage("Ring not found");
    }

    @Test
    void createRingSavesRing() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(ringRepository.save(any(Ring.class))).willAnswer(invocation -> {
            Ring ring = invocation.getArgument(0);
            ReflectionTestUtils.setField(ring, "id", 10L);
            return ring;
        });

        AdminRingResponse response = adminRingService.createRing(new AdminRingRequest(1L, " Ring 1 ", null));

        assertThat(response.getRingId()).isEqualTo(10L);
        assertThat(response.getTournamentId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Ring 1");
        assertThat(response.getStatus()).isEqualTo(RingStatus.READY);
    }

    @Test
    void createRingRejectsMissingTournament() {
        given(tournamentRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> adminRingService.createRing(new AdminRingRequest(99L, "Ring 1", null)))
                .isInstanceOf(TournamentNotFoundException.class)
                .hasMessage("Tournament not found");
    }

    @Test
    void createRingRejectsBlankName() {
        assertThatThrownBy(() -> adminRingService.createRing(new AdminRingRequest(1L, " ", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
    }

    @Test
    void updateRingChangesRing() {
        Ring ring = createRing(10L);
        AdminRingRequest request = new AdminRingRequest(1L, "Main Ring", RingStatus.CLOSED);
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(ringRepository.findById(10L)).willReturn(Optional.of(ring));
        given(ringRepository.save(any(Ring.class))).willAnswer(invocation -> invocation.getArgument(0));

        AdminRingResponse response = adminRingService.updateRing(10L, request);

        assertThat(response.getRingId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Main Ring");
        assertThat(response.getStatus()).isEqualTo(RingStatus.CLOSED);
    }

    @Test
    void updateRingRejectsMissingRing() {
        AdminRingRequest request = new AdminRingRequest(1L, "Main Ring", RingStatus.READY);
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(ringRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminRingService.updateRing(99L, request))
                .isInstanceOf(RingNotFoundException.class)
                .hasMessage("Ring not found");
    }

    @Test
    void updateRingRejectsNullRingId() {
        AdminRingRequest request = new AdminRingRequest(1L, "Main Ring", RingStatus.READY);

        assertThatThrownBy(() -> adminRingService.updateRing(null, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ringId is required");
    }

    @Test
    void deleteRingDeletesExistingRing() {
        given(ringRepository.existsById(10L)).willReturn(true);

        adminRingService.deleteRing(10L);

        then(ringRepository).should().deleteById(10L);
    }

    @Test
    void deleteRingRejectsMissingRing() {
        given(ringRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> adminRingService.deleteRing(99L))
                .isInstanceOf(RingNotFoundException.class)
                .hasMessage("Ring not found");
    }

    private Ring createRing(Long id) {
        Ring ring = Ring.builder()
                .tournamentId(1L)
                .name("Ring 1")
                .status(RingStatus.READY)
                .build();
        ReflectionTestUtils.setField(ring, "id", id);
        return ring;
    }
}
