package com.boxing.bracket.ringmanager.service;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.assignment.service.StaffAssignmentService;
import com.boxing.bracket.common.exception.WorkflowConflictException;
import com.boxing.bracket.event.domain.BoutEventType;
import com.boxing.bracket.event.dto.BoutEventResponse;
import com.boxing.bracket.event.service.BoutEventPublisher;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.exception.RingNotFoundException;
import com.boxing.bracket.ring.repository.RingRepository;
import com.boxing.bracket.ringmanager.dto.BoutStatusUpdateRequest;
import com.boxing.bracket.ringmanager.dto.RingManagerBoutResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Lazy
@Transactional
public class RingManagerService {

    private final BoutRepository boutRepository;
    private final RingRepository ringRepository;
    private final BoutEventPublisher boutEventPublisher;
    private final StaffAssignmentService staffAssignmentService;

    public RingManagerService(
            BoutRepository boutRepository,
            RingRepository ringRepository,
            @Lazy BoutEventPublisher boutEventPublisher
    ) {
        this(boutRepository, ringRepository, boutEventPublisher, null);
    }

    @Autowired
    public RingManagerService(
            BoutRepository boutRepository,
            RingRepository ringRepository,
            @Lazy BoutEventPublisher boutEventPublisher,
            @Lazy StaffAssignmentService staffAssignmentService
    ) {
        this.boutRepository = boutRepository;
        this.ringRepository = ringRepository;
        this.boutEventPublisher = boutEventPublisher;
        this.staffAssignmentService = staffAssignmentService;
    }

    public RingManagerBoutResponse startBout(Long boutId) {
        if (boutId == null) {
            throw new IllegalArgumentException("boutId is required");
        }

        Bout bout = boutRepository.findWithLockById(boutId)
                .orElseThrow(BoutNotFoundException::new);
        if (staffAssignmentService != null) {
            staffAssignmentService.requireRingAccess(bout.getRingId());
        }
        Ring ring = ringRepository.findWithLockById(bout.getRingId())
                .orElseThrow(RingNotFoundException::new);

        if (bout.getStatus() == BoutStatus.IN_PROGRESS) {
            if (boutId.equals(ring.getCurrentBoutId())) {
                return RingManagerBoutResponse.from(bout);
            }
            throw new WorkflowConflictException("BOUT_ALREADY_STARTED");
        }
        if (ring.getCurrentBoutId() != null && !boutId.equals(ring.getCurrentBoutId())) {
            throw new WorkflowConflictException("RING_ALREADY_HAS_CURRENT_BOUT");
        }

        bout.start();
        ring.assignCurrentBout(bout.getId());

        ringRepository.save(ring);
        Bout savedBout = boutRepository.save(bout);
        boutEventPublisher.publish(BoutEventResponse.of(BoutEventType.BOUT_STARTED, savedBout));
        return RingManagerBoutResponse.from(savedBout);
    }

    @Transactional(readOnly = true)
    public List<RingManagerBoutResponse> getRingBouts(Long ringId) {
        if (ringId == null) {
            throw new IllegalArgumentException("ringId is required");
        }
        if (!ringRepository.existsById(ringId)) {
            throw new RingNotFoundException();
        }
        if (staffAssignmentService != null) {
            staffAssignmentService.requireRingAccess(ringId);
        }

        return boutRepository.findByRingIdOrderByScheduledOrderAsc(ringId).stream()
                .filter(bout -> !bout.isEventBout())
                .map(RingManagerBoutResponse::from)
                .collect(Collectors.toList());
    }

    public RingManagerBoutResponse moveToNextBout(Long ringId) {
        if (ringId == null) {
            throw new IllegalArgumentException("ringId is required");
        }

        Ring ring = ringRepository.findWithLockById(ringId)
                .orElseThrow(RingNotFoundException::new);
        if (staffAssignmentService != null) {
            staffAssignmentService.requireRingAccess(ringId);
        }
        List<Bout> officialBouts = boutRepository.findByRingIdOrderByScheduledOrderAsc(ringId).stream()
                .filter(bout -> !bout.isEventBout())
                .collect(Collectors.toList());
        Optional<Bout> currentBout = findCurrentBout(ring, officialBouts);
        if (currentBout.isPresent() && currentBout.get().getStatus() != BoutStatus.FINISHED) {
            throw new WorkflowConflictException("CURRENT_BOUT_NOT_FINISHED");
        }

        Bout nextBout = findNextBout(currentBout, officialBouts)
                .orElseThrow(() -> new IllegalArgumentException("next bout does not exist"));

        boolean statusChanged = nextBout.changeStatus(BoutStatus.READY);
        ring.prepareCurrentBout(nextBout.getId());

        ringRepository.save(ring);
        Bout savedBout = statusChanged ? boutRepository.save(nextBout) : nextBout;
        boutEventPublisher.publish(BoutEventResponse.of(BoutEventType.NEXT_BOUT_READY, savedBout));
        return RingManagerBoutResponse.from(savedBout);
    }

    public RingManagerBoutResponse updateBoutStatus(Long boutId, BoutStatusUpdateRequest request) {
        if (boutId == null) {
            throw new IllegalArgumentException("boutId is required");
        }
        validateStatusUpdateRequest(request);

        Bout bout = boutRepository.findWithLockById(boutId)
                .orElseThrow(BoutNotFoundException::new);
        if (staffAssignmentService != null) {
            staffAssignmentService.requireRingAccess(bout.getRingId());
        }
        boolean statusChanged = bout.changeStatus(request.getStatus());
        if (!statusChanged) {
            return RingManagerBoutResponse.from(bout);
        }

        Bout savedBout = boutRepository.save(bout);
        boutEventPublisher.publish(BoutEventResponse.of(BoutEventType.BOUT_STATUS_CHANGED, savedBout));
        return RingManagerBoutResponse.from(savedBout);
    }

    public RingManagerBoutResponse startRound(Long boutId, Integer roundNo) {
        if (boutId == null) {
            throw new IllegalArgumentException("boutId is required");
        }

        Bout bout = boutRepository.findWithLockById(boutId)
                .orElseThrow(BoutNotFoundException::new);
        if (staffAssignmentService != null) {
            staffAssignmentService.requireRingAccess(bout.getRingId());
        }
        boolean roundStarted = bout.startRound(roundNo);
        if (!roundStarted) {
            return RingManagerBoutResponse.from(bout);
        }

        Bout savedBout = boutRepository.save(bout);
        boutEventPublisher.publish(BoutEventResponse.of(BoutEventType.ROUND_STARTED, savedBout, roundNo));
        return RingManagerBoutResponse.from(savedBout);
    }

    private void validateStatusUpdateRequest(BoutStatusUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("status request is required");
        }
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("status is required");
        }
    }

    private Optional<Bout> findNextBout(Optional<Bout> currentBout, List<Bout> bouts) {
        if (currentBout.isPresent() && currentBout.get().getScheduledOrder() != null) {
            Integer currentScheduledOrder = currentBout.get().getScheduledOrder();
            return bouts.stream()
                    .filter(bout -> bout.getScheduledOrder() != null)
                    .filter(bout -> bout.getScheduledOrder() > currentScheduledOrder)
                    .min(Comparator.comparing(Bout::getScheduledOrder));
        }

        return bouts.stream()
                .filter(this::isNextCandidate)
                .filter(bout -> bout.getScheduledOrder() != null)
                .min(Comparator.comparing(Bout::getScheduledOrder));
    }

    private Optional<Bout> findCurrentBout(Ring ring, List<Bout> bouts) {
        if (ring.getCurrentBoutId() != null) {
            Optional<Bout> currentByRing = bouts.stream()
                    .filter(bout -> ring.getCurrentBoutId().equals(bout.getId()))
                    .findFirst();
            if (currentByRing.isPresent()) {
                return currentByRing;
            }
        }

        return bouts.stream()
                .filter(bout -> bout.getStatus() == BoutStatus.IN_PROGRESS)
                .findFirst();
    }

    private boolean isNextCandidate(Bout bout) {
        return bout.getStatus() == BoutStatus.SCHEDULED || bout.getStatus() == BoutStatus.READY;
    }
}
