package com.boxing.bracket.ringmanager.service;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.common.exception.WorkflowConflictException;
import com.boxing.bracket.event.dto.BoutEventResponse;
import com.boxing.bracket.event.service.BoutEventPublisher;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class RingManagerServiceTest {

    @Mock
    private BoutRepository boutRepository;

    @Mock
    private RingRepository ringRepository;

    @Mock
    private BoutEventPublisher boutEventPublisher;

    @InjectMocks
    private RingManagerService ringManagerService;

    @Test
    void startBoutStartsBoutAndAssignsRingCurrentBout() {
        Bout bout = createBout(10L);
        Ring ring = createRing(1L);
        given(ringRepository.findWithLockById(1L)).willReturn(Optional.of(ring));
        given(boutRepository.findWithLockById(10L)).willReturn(Optional.of(bout));
        given(boutRepository.save(any(Bout.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(ringRepository.save(any(Ring.class))).willAnswer(invocation -> invocation.getArgument(0));

        RingManagerBoutResponse response = ringManagerService.startBout(10L);

        assertThat(response.getBoutId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(BoutStatus.IN_PROGRESS);
        assertThat(response.getStartedAt()).isNotNull();
        assertThat(ring.getCurrentBoutId()).isEqualTo(10L);
        assertThat(ring.getStatus()).isEqualTo(RingStatus.IN_PROGRESS);
        then(boutEventPublisher).should().publish(any(BoutEventResponse.class));
    }

    @Test
    void startBoutRejectsMissingBout() {
        given(boutRepository.findWithLockById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ringManagerService.startBout(99L))
                .isInstanceOf(BoutNotFoundException.class)
                .hasMessage("Bout not found");
    }

    @Test
    void startBoutRejectsMissingRing() {
        Bout bout = createBout(10L);
        given(boutRepository.findWithLockById(10L)).willReturn(Optional.of(bout));
        given(ringRepository.findWithLockById(1L)).willReturn(Optional.empty());

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
    void getRingBoutsReturnsOfficialBouts() {
        Bout officialBout = createBout(10L);
        Bout eventBout = createBout(11L);
        ReflectionTestUtils.setField(eventBout, "eventBout", true);
        given(ringRepository.existsById(1L)).willReturn(true);
        given(boutRepository.findByRingIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(officialBout, eventBout));

        List<RingManagerBoutResponse> responses = ringManagerService.getRingBouts(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBoutId()).isEqualTo(10L);
        assertThat(responses.get(0).getScheduledOrder()).isEqualTo(1);
    }

    @Test
    void getRingBoutsRejectsMissingRing() {
        given(ringRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> ringManagerService.getRingBouts(99L))
                .isInstanceOf(RingNotFoundException.class)
                .hasMessage("Ring not found");
    }

    @Test
    void getRingBoutsRejectsNullRingId() {
        assertThatThrownBy(() -> ringManagerService.getRingBouts(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ringId is required");
    }

    @Test
    void moveToNextBoutAssignsNextBoutToRing() {
        Ring ring = createRing(1L);
        ring.assignCurrentBout(10L);
        Bout currentBout = createBout(10L);
        currentBout.changeStatus(BoutStatus.FINISHED);
        Bout nextBout = createBout(11L, 2);
        given(ringRepository.findWithLockById(1L)).willReturn(Optional.of(ring));
        given(boutRepository.findByRingIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(currentBout, nextBout));
        given(ringRepository.save(any(Ring.class))).willAnswer(invocation -> invocation.getArgument(0));

        RingManagerBoutResponse response = ringManagerService.moveToNextBout(1L);

        assertThat(response.getBoutId()).isEqualTo(11L);
        assertThat(response.getStatus()).isEqualTo(BoutStatus.READY);
        assertThat(ring.getCurrentBoutId()).isEqualTo(11L);
        assertThat(ring.getStatus()).isEqualTo(RingStatus.READY);
        then(boutEventPublisher).should().publish(any(BoutEventResponse.class));
    }

    @Test
    void moveToNextBoutRejectsMissingNextBout() {
        Ring ring = createRing(1L);
        ring.assignCurrentBout(10L);
        Bout currentBout = createBout(10L);
        currentBout.changeStatus(BoutStatus.FINISHED);
        given(ringRepository.findWithLockById(1L)).willReturn(Optional.of(ring));
        given(boutRepository.findByRingIdOrderByScheduledOrderAsc(1L)).willReturn(List.of(currentBout));

        assertThatThrownBy(() -> ringManagerService.moveToNextBout(1L))
                .isInstanceOf(WorkflowConflictException.class)
                .hasMessage("NEXT_BOUT_NOT_FOUND");
    }

    @Test
    void moveToNextBoutRejectsMissingRing() {
        given(ringRepository.findWithLockById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ringManagerService.moveToNextBout(99L))
                .isInstanceOf(RingNotFoundException.class)
                .hasMessage("Ring not found");
    }

    @Test
    void updateBoutStatusChangesBoutStatus() {
        Bout bout = createBout(10L);
        bout.startForRingManager();
        bout.startRoundForRingManager(1);
        BoutStatusUpdateRequest request = new BoutStatusUpdateRequest(BoutStatus.SCORING);
        given(boutRepository.findWithLockById(10L)).willReturn(Optional.of(bout));
        given(boutRepository.save(any(Bout.class))).willAnswer(invocation -> invocation.getArgument(0));

        RingManagerBoutResponse response = ringManagerService.updateBoutStatus(10L, request);

        assertThat(response.getBoutId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(BoutStatus.SCORING);
        assertThat(bout.getStatus()).isEqualTo(BoutStatus.SCORING);
        then(boutEventPublisher).should().publish(any(BoutEventResponse.class));
    }

    @Test
    void updateBoutStatusRejectsMissingBout() {
        BoutStatusUpdateRequest request = new BoutStatusUpdateRequest(BoutStatus.SCORING);
        given(boutRepository.findWithLockById(99L)).willReturn(Optional.empty());

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

    @Test
    void startRoundUpdatesBoutCurrentRound() {
        Bout bout = createBout(10L);
        bout.startForRingManager();
        given(boutRepository.findWithLockById(10L)).willReturn(Optional.of(bout));
        given(boutRepository.save(any(Bout.class))).willAnswer(invocation -> invocation.getArgument(0));

        RingManagerBoutResponse response = ringManagerService.startRound(10L, 1);

        assertThat(response.getBoutId()).isEqualTo(10L);
        assertThat(response.getCurrentRound()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(BoutStatus.IN_PROGRESS);
        then(boutEventPublisher).should().publish(any(BoutEventResponse.class));
    }

    @Test
    void startRoundRejectsMissingBout() {
        given(boutRepository.findWithLockById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ringManagerService.startRound(99L, 1))
                .isInstanceOf(BoutNotFoundException.class)
                .hasMessage("Bout not found");
    }

    @Test
    void startRoundRejectsInvalidRoundNo() {
        Bout bout = createBout(10L);
        bout.startForRingManager();
        given(boutRepository.findWithLockById(10L)).willReturn(Optional.of(bout));

        assertThatThrownBy(() -> ringManagerService.startRound(10L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_ROUND_NUMBER");
    }

    @Test
    void startBoutReturnsCurrentStateForDuplicateRequestWithoutPublishingAgain() {
        Bout bout = createBout(10L);
        Ring ring = createRing(1L);
        given(ringRepository.findWithLockById(1L)).willReturn(Optional.of(ring));
        given(boutRepository.findWithLockById(10L)).willReturn(Optional.of(bout));
        given(boutRepository.save(any(Bout.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(ringRepository.save(any(Ring.class))).willAnswer(invocation -> invocation.getArgument(0));

        ringManagerService.startBout(10L);
        RingManagerBoutResponse response = ringManagerService.startBout(10L);

        assertThat(response.getStatus()).isEqualTo(BoutStatus.IN_PROGRESS);
        then(boutRepository).should(times(1)).save(any(Bout.class));
        then(ringRepository).should(times(1)).save(any(Ring.class));
        then(boutEventPublisher).should(times(1)).publish(any(BoutEventResponse.class));
    }

    @Test
    void moveToNextBoutRejectsDuplicateRequestAfterPreparingNextBout() {
        Ring ring = createRing(1L);
        ring.assignCurrentBout(10L);
        Bout currentBout = createBout(10L);
        currentBout.changeStatus(BoutStatus.FINISHED);
        Bout nextBout = createBout(11L, 2);
        given(ringRepository.findWithLockById(1L)).willReturn(Optional.of(ring));
        given(boutRepository.findByRingIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(currentBout, nextBout));
        given(ringRepository.save(any(Ring.class))).willAnswer(invocation -> invocation.getArgument(0));

        ringManagerService.moveToNextBout(1L);

        assertThatThrownBy(() -> ringManagerService.moveToNextBout(1L))
                .isInstanceOf(WorkflowConflictException.class)
                .hasMessage("CURRENT_BOUT_NOT_FINISHED");
        then(boutEventPublisher).should(times(1)).publish(any(BoutEventResponse.class));
    }

    @Test
    void updateBoutStatusReturnsCurrentStateForDuplicateRequestWithoutPublishingAgain() {
        Bout bout = createBout(10L);
        bout.changeStatus(BoutStatus.SCORING);
        BoutStatusUpdateRequest request = new BoutStatusUpdateRequest(BoutStatus.SCORING);
        given(boutRepository.findWithLockById(10L)).willReturn(Optional.of(bout));

        RingManagerBoutResponse response = ringManagerService.updateBoutStatus(10L, request);

        assertThat(response.getStatus()).isEqualTo(BoutStatus.SCORING);
        then(boutRepository).should(never()).save(any(Bout.class));
        then(boutEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void startRoundReturnsCurrentStateForDuplicateRequestWithoutPublishingAgain() {
        Bout bout = createBout(10L);
        bout.startForRingManager();
        given(boutRepository.findWithLockById(10L)).willReturn(Optional.of(bout));
        given(boutRepository.save(any(Bout.class))).willAnswer(invocation -> invocation.getArgument(0));

        ringManagerService.startRound(10L, 1);
        RingManagerBoutResponse response = ringManagerService.startRound(10L, 1);

        assertThat(response.getCurrentRound()).isEqualTo(1);
        then(boutRepository).should(times(1)).save(any(Bout.class));
        then(boutEventPublisher).should(times(1)).publish(any(BoutEventResponse.class));
    }

    @Test
    void startBoutRejectsScheduledBoutUntilItIsPrepared() {
        Bout bout = createBout(10L, 1, BoutStatus.SCHEDULED);
        Ring ring = createRing(1L);
        given(ringRepository.findWithLockById(1L)).willReturn(Optional.of(ring));
        given(boutRepository.findWithLockById(10L)).willReturn(Optional.of(bout));

        assertThatThrownBy(() -> ringManagerService.startBout(10L))
                .isInstanceOf(WorkflowConflictException.class)
                .hasMessage("INVALID_BOUT_TRANSITION");
        then(ringRepository).should(never()).save(any(Ring.class));
        then(boutRepository).should(never()).save(any(Bout.class));
        then(boutEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void startRoundRejectsSkippedRound() {
        Bout bout = createBout(10L);
        bout.startForRingManager();
        given(boutRepository.findWithLockById(10L)).willReturn(Optional.of(bout));

        assertThatThrownBy(() -> ringManagerService.startRound(10L, 2))
                .isInstanceOf(WorkflowConflictException.class)
                .hasMessage("ROUND_SEQUENCE_INVALID");
        then(boutRepository).should(never()).save(any(Bout.class));
        then(boutEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void updateBoutStatusRejectsFinishedWithoutConfirmedResult() {
        Bout bout = createBout(10L);
        bout.startForRingManager();
        bout.startRoundForRingManager(1);
        BoutStatusUpdateRequest request = new BoutStatusUpdateRequest(BoutStatus.FINISHED);
        given(boutRepository.findWithLockById(10L)).willReturn(Optional.of(bout));

        assertThatThrownBy(() -> ringManagerService.updateBoutStatus(10L, request))
                .isInstanceOf(WorkflowConflictException.class)
                .hasMessage("BOUT_RESULT_REQUIRED");
        then(boutRepository).should(never()).save(any(Bout.class));
        then(boutEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void updateBoutStatusDoesNotAllowClientToPrepareAnArbitraryScheduledBout() {
        Bout bout = createBout(10L, 1, BoutStatus.SCHEDULED);
        given(boutRepository.findWithLockById(10L)).willReturn(Optional.of(bout));

        assertThatThrownBy(() -> ringManagerService.updateBoutStatus(
                10L,
                new BoutStatusUpdateRequest(BoutStatus.READY)
        ))
                .isInstanceOf(WorkflowConflictException.class)
                .hasMessage("INVALID_BOUT_TRANSITION");
        then(boutRepository).should(never()).save(any(Bout.class));
        then(boutEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void updateBoutStatusRejectsScoringBeforeFinalRound() {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(1)
                .matchType("75 - middle school")
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .status(BoutStatus.IN_PROGRESS)
                .totalRounds(3)
                .currentRound(1)
                .scheduledOrder(1)
                .build();
        ReflectionTestUtils.setField(bout, "id", 10L);
        given(boutRepository.findWithLockById(10L)).willReturn(Optional.of(bout));

        assertThatThrownBy(() -> ringManagerService.updateBoutStatus(
                10L,
                new BoutStatusUpdateRequest(BoutStatus.SCORING)
        ))
                .isInstanceOf(WorkflowConflictException.class)
                .hasMessage("INVALID_BOUT_TRANSITION");
        then(boutRepository).should(never()).save(any(Bout.class));
        then(boutEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    void moveToNextBoutSkipsFinishedAndCanceledCandidates() {
        Ring ring = createRing(1L);
        ring.assignCurrentBout(10L);
        Bout currentBout = createBout(10L);
        currentBout.changeStatus(BoutStatus.FINISHED);
        Bout canceledBout = createBout(11L, 2, BoutStatus.CANCELED);
        Bout nextBout = createBout(12L, 3, BoutStatus.SCHEDULED);
        given(ringRepository.findWithLockById(1L)).willReturn(Optional.of(ring));
        given(boutRepository.findByRingIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(currentBout, canceledBout, nextBout));
        given(ringRepository.save(any(Ring.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(boutRepository.save(any(Bout.class))).willAnswer(invocation -> invocation.getArgument(0));

        RingManagerBoutResponse response = ringManagerService.moveToNextBout(1L);

        assertThat(response.getBoutId()).isEqualTo(12L);
        assertThat(response.getStatus()).isEqualTo(BoutStatus.READY);
    }

    private Bout createBout(Long id) {
        return createBout(id, 1, BoutStatus.READY);
    }

    private Bout createBout(Long id, int scheduledOrder) {
        return createBout(id, scheduledOrder, BoutStatus.READY);
    }

    private Bout createBout(Long id, int scheduledOrder, BoutStatus status) {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(scheduledOrder)
                .matchType("75 - middle school")
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .status(status)
                .scheduledOrder(scheduledOrder)
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
