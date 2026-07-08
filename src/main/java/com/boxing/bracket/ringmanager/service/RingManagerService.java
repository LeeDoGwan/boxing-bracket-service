package com.boxing.bracket.ringmanager.service;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.exception.RingNotFoundException;
import com.boxing.bracket.ring.repository.RingRepository;
import com.boxing.bracket.ringmanager.dto.RingManagerBoutResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
