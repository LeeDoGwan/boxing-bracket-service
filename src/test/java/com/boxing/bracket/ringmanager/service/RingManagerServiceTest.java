package com.boxing.bracket.ringmanager.service;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.domain.RingStatus;
import com.boxing.bracket.ring.exception.RingNotFoundException;
import com.boxing.bracket.ring.repository.RingRepository;
import com.boxing.bracket.ringmanager.dto.BoutStatusUpdateRequest;
import com.boxing.bracket.ringmanager.dto.RingManagerBoutResponse;
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

@ExtendWith(MockitoExtension.class)
class RingManagerServiceTest {

    @Mock
    private BoutRepository boutRepository;

    @Mock
    private RingRepository ringRepository;

    @InjectMocks
    private RingManagerService ringManagerService;

    @Test
    void startBoutStartsBoutAndAssignsRingCurrentBout() {
        Bout bout = createBout(10L);
        Ring ring = createRing(1L);
        given(boutRepository.findById(10L)).willReturn(Optional.of(bout));
        given(ringRepository.findById(1L)).willReturn(Optional.of(ring));
        given(boutRepository.save(any(Bout.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(ringRepository.save(any(Ring.class))).willAnswer(invocation -> invocation.getArgument(0));

        RingManagerBoutResponse response = ringManagerService.startBout(10L);

        assertThat(response.getBoutId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(BoutStatus.IN_PROGRESS);
        assertThat(response.getStartedAt()).isNotNull();
        assertThat(ring.getCurrentBoutId()).isEqualTo(10L);
        assertThat(ring.getStatus()).isEqualTo(RingStatus.IN_PROGRESS);
    }

    @Test
    void startBoutRejectsMissingBout() {
        given(boutRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ringManagerService.startBout(99L))
                .isInstanceOf(BoutNotFoundException.class)
                .hasMessage("Bout not found");
    }

    @Test
    void startBoutRejectsMissingRing() {
        Bout bout = createBout(10L);
        given(boutRepository.findById(10L)).willReturn(Optional.of(bout));
        given(ringRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ringManagerService.startBout(10L))
                .isInstanceOf(RingNotFoundException.class)
                .hasMessage("Ring not found");
    }

    @Test
    void startBoutRejectsNullBoutId() {
        assertThatThrownBy(() -> ringManagerService.startBout(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("boutId is required");
    }

    @Test
    void updateBoutStatusChangesBoutStatus() {
        Bout bout = createBout(10L);
        BoutStatusUpdateRequest request = new BoutStatusUpdateRequest(BoutStatus.SCORING);
        given(boutRepository.findById(10L)).willReturn(Optional.of(bout));
        given(boutRepository.save(any(Bout.class))).willAnswer(invocation -> invocation.getArgument(0));

        RingManagerBoutResponse response = ringManagerService.updateBoutStatus(10L, request);

        assertThat(response.getBoutId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(BoutStatus.SCORING);
        assertThat(bout.getStatus()).isEqualTo(BoutStatus.SCORING);
    }

    @Test
    void updateBoutStatusRejectsMissingBout() {
        BoutStatusUpdateRequest request = new BoutStatusUpdateRequest(BoutStatus.SCORING);
        given(boutRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ringManagerService.updateBoutStatus(99L, request))
                .isInstanceOf(BoutNotFoundException.class)
                .hasMessage("Bout not found");
    }

    @Test
    void updateBoutStatusRejectsNullStatus() {
        BoutStatusUpdateRequest request = new BoutStatusUpdateRequest(null);

        assertThatThrownBy(() -> ringManagerService.updateBoutStatus(10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("status is required");
    }

    private Bout createBout(Long id) {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(1)
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .status(BoutStatus.READY)
                .build();
        ReflectionTestUtils.setField(bout, "id", id);
        return bout;
    }

    private Ring createRing(Long id) {
        Ring ring = Ring.builder()
                .tournamentId(1L)
                .name("Ring " + id)
                .status(RingStatus.READY)
                .build();
        ReflectionTestUtils.setField(ring, "id", id);
        return ring;
    }
}
