package com.boxing.bracket.bout.admin.service;

import com.boxing.bracket.athlete.exception.AthleteNotFoundException;
import com.boxing.bracket.athlete.repository.AthleteRepository;
import com.boxing.bracket.bout.admin.dto.AdminBoutRequest;
import com.boxing.bracket.bout.admin.dto.AdminBoutResponse;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminBoutServiceTest {

    @Mock
    private BoutRepository boutRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private RingRepository ringRepository;

    @Mock
    private AthleteRepository athleteRepository;

    @InjectMocks
    private AdminBoutService adminBoutService;

    @Test
    void createBoutSavesBout() {
        givenValidReferences();
        given(boutRepository.save(any(Bout.class))).willAnswer(invocation -> {
            Bout bout = invocation.getArgument(0);
            ReflectionTestUtils.setField(bout, "id", 20L);
            return bout;
        });

        AdminBoutResponse response = adminBoutService.createBout(request());

        assertThat(response.getBoutId()).isEqualTo(20L);
        assertThat(response.getTournamentId()).isEqualTo(1L);
        assertThat(response.getRingId()).isEqualTo(1L);
        assertThat(response.getRedAthleteId()).isEqualTo(10L);
        assertThat(response.getBlueAthleteId()).isEqualTo(11L);
        assertThat(response.getScheduledOrder()).isEqualTo(1);
    }

    @Test
    void createBoutRejectsMissingTournament() {
        given(tournamentRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> adminBoutService.createBout(request()))
                .isInstanceOf(TournamentNotFoundException.class)
                .hasMessage("Tournament not found");
    }

    @Test
    void createBoutRejectsMissingRing() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(ringRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminBoutService.createBout(request()))
                .isInstanceOf(RingNotFoundException.class)
                .hasMessage("Ring not found");
    }

    @Test
    void createBoutRejectsRingFromDifferentTournament() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(ringRepository.findById(1L)).willReturn(Optional.of(createRing(1L, 2L)));

        assertThatThrownBy(() -> adminBoutService.createBout(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ring does not belong to tournament");
    }

    @Test
    void createBoutRejectsMissingAthlete() {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(ringRepository.findById(1L)).willReturn(Optional.of(createRing(1L, 1L)));
        given(athleteRepository.existsById(10L)).willReturn(true);
        given(athleteRepository.existsById(11L)).willReturn(false);

        assertThatThrownBy(() -> adminBoutService.createBout(request()))
                .isInstanceOf(AthleteNotFoundException.class)
                .hasMessage("Athlete not found");
    }

    @Test
    void createBoutRejectsSameAthlete() {
        AdminBoutRequest request = new AdminBoutRequest(1L, 1L, 1, "75", 10L, 10L, 3, 1, false);

        assertThatThrownBy(() -> adminBoutService.createBout(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("redAthleteId and blueAthleteId must be different");
    }

    @Test
    void updateBoutChangesBout() {
        Bout bout = createBout(20L);
        AdminBoutRequest request = new AdminBoutRequest(1L, 1L, 2, "80 - high school", 12L, 13L, 4, 2, true);
        givenValidReferences(12L, 13L);
        given(boutRepository.findById(20L)).willReturn(Optional.of(bout));
        given(boutRepository.save(any(Bout.class))).willAnswer(invocation -> invocation.getArgument(0));

        AdminBoutResponse response = adminBoutService.updateBout(20L, request);

        assertThat(response.getBoutId()).isEqualTo(20L);
        assertThat(response.getBoutNumber()).isEqualTo(2);
        assertThat(response.getMatchType()).isEqualTo("80 - high school");
        assertThat(response.getRedAthleteId()).isEqualTo(12L);
        assertThat(response.getBlueAthleteId()).isEqualTo(13L);
        assertThat(response.isEventBout()).isTrue();
    }

    @Test
    void updateBoutRejectsMissingBout() {
        givenValidReferences();
        given(boutRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminBoutService.updateBout(99L, request()))
                .isInstanceOf(BoutNotFoundException.class)
                .hasMessage("Bout not found");
    }

    @Test
    void updateBoutRejectsNullBoutId() {
        assertThatThrownBy(() -> adminBoutService.updateBout(null, request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("boutId is required");
    }

    @Test
    void deleteBoutDeletesExistingBout() {
        given(boutRepository.existsById(20L)).willReturn(true);

        adminBoutService.deleteBout(20L);

        then(boutRepository).should().deleteById(20L);
    }

    @Test
    void deleteBoutRejectsMissingBout() {
        given(boutRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> adminBoutService.deleteBout(99L))
                .isInstanceOf(BoutNotFoundException.class)
                .hasMessage("Bout not found");
    }

    private void givenValidReferences() {
        givenValidReferences(10L, 11L);
    }

    private void givenValidReferences(Long redAthleteId, Long blueAthleteId) {
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(ringRepository.findById(1L)).willReturn(Optional.of(createRing(1L, 1L)));
        given(athleteRepository.existsById(redAthleteId)).willReturn(true);
        given(athleteRepository.existsById(blueAthleteId)).willReturn(true);
    }

    private AdminBoutRequest request() {
        return new AdminBoutRequest(1L, 1L, 1, "75 - middle school", 10L, 11L, 3, 1, false);
    }

    private Bout createBout(Long id) {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(1)
                .matchType("75 - middle school")
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .totalRounds(3)
                .scheduledOrder(1)
                .build();
        ReflectionTestUtils.setField(bout, "id", id);
        return bout;
    }

    private Ring createRing(Long id, Long tournamentId) {
        Ring ring = Ring.builder()
                .tournamentId(tournamentId)
                .name("Ring " + id)
                .status(RingStatus.READY)
                .build();
        ReflectionTestUtils.setField(ring, "id", id);
        return ring;
    }
}
