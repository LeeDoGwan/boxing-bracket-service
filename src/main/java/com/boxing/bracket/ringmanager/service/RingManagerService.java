package com.boxing.bracket.ringmanager.service;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.exception.RingNotFoundException;
import com.boxing.bracket.ring.repository.RingRepository;
import com.boxing.bracket.ringmanager.dto.BoutStatusUpdateRequest;
import com.boxing.bracket.ringmanager.dto.RingManagerBoutResponse;
import org.springframework.context.annotation.Lazy;
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

    public RingManagerService(BoutRepository boutRepository, RingRepository ringRepository) {
        this.boutRepository = boutRepository;
        this.ringRepository = ringRepository;
    }

    public RingManagerBoutResponse startBout(Long boutId) {
        if (boutId == null) {
            throw new IllegalArgumentException("boutId is required");
        }

        Bout bout = boutRepository.findById(boutId)
                .orElseThrow(BoutNotFoundException::new);
        Ring ring = ringRepository.findById(bout.getRingId())
                .orElseThrow(RingNotFoundException::new);

        bout.start();
        ring.assignCurrentBout(bout.getId());

        ringRepository.save(ring);
        return RingManagerBoutResponse.from(boutRepository.save(bout));
    }

    @Transactional(readOnly = true)
    public List<RingManagerBoutResponse> getRingBouts(Long ringId) {
        if (ringId == null) {
            throw new IllegalArgumentException("ringId is required");
        }
        if (!ringRepository.existsById(ringId)) {
            throw new RingNotFoundException();
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

        Ring ring = ringRepository.findById(ringId)
                .orElseThrow(RingNotFoundException::new);
        List<Bout> officialBouts = boutRepository.findByRingIdOrderByScheduledOrderAsc(ringId).stream()
                .filter(bout -> !bout.isEventBout())
                .collect(Collectors.toList());
        Bout nextBout = findNextBout(ring, officialBouts)
                .orElseThrow(() -> new IllegalArgumentException("next bout does not exist"));

        nextBout.changeStatus(BoutStatus.READY);
        ring.prepareCurrentBout(nextBout.getId());

        ringRepository.save(ring);
        return RingManagerBoutResponse.from(boutRepository.save(nextBout));
    }

    public RingManagerBoutResponse updateBoutStatus(Long boutId, BoutStatusUpdateRequest request) {
        if (boutId == null) {
            throw new IllegalArgumentException("boutId is required");
        }
        validateStatusUpdateRequest(request);

        Bout bout = boutRepository.findById(boutId)
                .orElseThrow(BoutNotFoundException::new);
        bout.changeStatus(request.getStatus());

        return RingManagerBoutResponse.from(boutRepository.save(bout));
    }

    private void validateStatusUpdateRequest(BoutStatusUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("status request is required");
        }
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("status is required");
        }
    }

    private Optional<Bout> findNextBout(Ring ring, List<Bout> bouts) {
        Optional<Bout> currentBout = findCurrentBout(ring, bouts);
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
