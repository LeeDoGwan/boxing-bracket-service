package com.boxing.bracket.ringmanager.service;

import com.boxing.bracket.bout.domain.Bout;
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

import java.util.List;
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
}
