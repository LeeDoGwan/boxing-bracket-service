package com.boxing.bracket.ring.admin.service;

import com.boxing.bracket.ring.admin.dto.AdminRingRequest;
import com.boxing.bracket.ring.admin.dto.AdminRingResponse;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.exception.RingNotFoundException;
import com.boxing.bracket.ring.repository.RingRepository;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Lazy
@Transactional
public class AdminRingService {

    private final RingRepository ringRepository;
    private final TournamentRepository tournamentRepository;

    public AdminRingService(RingRepository ringRepository, TournamentRepository tournamentRepository) {
        this.ringRepository = ringRepository;
        this.tournamentRepository = tournamentRepository;
    }

    public AdminRingResponse createRing(AdminRingRequest request) {
        validateRequest(request);
        Ring ring = Ring.builder()
                .tournamentId(request.getTournamentId())
                .name(request.getName())
                .status(request.getStatus())
                .build();

        return AdminRingResponse.from(ringRepository.save(ring));
    }

    public AdminRingResponse updateRing(Long ringId, AdminRingRequest request) {
        validateRingId(ringId);
        validateRequest(request);

        Ring ring = ringRepository.findById(ringId)
                .orElseThrow(RingNotFoundException::new);
        ring.updateInfo(request.getTournamentId(), request.getName(), request.getStatus());

        return AdminRingResponse.from(ringRepository.save(ring));
    }

    public void deleteRing(Long ringId) {
        validateRingId(ringId);
        if (!ringRepository.existsById(ringId)) {
            throw new RingNotFoundException();
        }

        ringRepository.deleteById(ringId);
    }

    private void validateRequest(AdminRingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ring request is required");
        }
        if (request.getTournamentId() == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        if (!tournamentRepository.existsById(request.getTournamentId())) {
            throw new TournamentNotFoundException();
        }
    }

    private void validateRingId(Long ringId) {
        if (ringId == null) {
            throw new IllegalArgumentException("ringId is required");
        }
    }
}
