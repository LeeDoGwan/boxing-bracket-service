package com.boxing.bracket.bout.admin.service;

import com.boxing.bracket.athlete.exception.AthleteNotFoundException;
import com.boxing.bracket.athlete.repository.AthleteRepository;
import com.boxing.bracket.bout.admin.dto.AdminBoutRequest;
import com.boxing.bracket.bout.admin.dto.AdminBoutResponse;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
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
public class AdminBoutService {

    private final BoutRepository boutRepository;
    private final TournamentRepository tournamentRepository;
    private final RingRepository ringRepository;
    private final AthleteRepository athleteRepository;

    public AdminBoutService(
            BoutRepository boutRepository,
            TournamentRepository tournamentRepository,
            RingRepository ringRepository,
            AthleteRepository athleteRepository
    ) {
        this.boutRepository = boutRepository;
        this.tournamentRepository = tournamentRepository;
        this.ringRepository = ringRepository;
        this.athleteRepository = athleteRepository;
    }

    public AdminBoutResponse createBout(AdminBoutRequest request) {
        validateRequest(request);

        Bout bout = Bout.builder()
                .tournamentId(request.getTournamentId())
                .ringId(request.getRingId())
                .boutNumber(request.getBoutNumber())
                .matchType(request.getMatchType())
                .redAthleteId(request.getRedAthleteId())
                .blueAthleteId(request.getBlueAthleteId())
                .totalRounds(request.getTotalRounds())
                .scheduledOrder(request.getScheduledOrder())
                .eventBout(request.isEventBout())
                .build();

        return AdminBoutResponse.from(boutRepository.save(bout));
    }

    public AdminBoutResponse updateBout(Long boutId, AdminBoutRequest request) {
        validateBoutId(boutId);
        validateRequest(request);

        Bout bout = boutRepository.findById(boutId)
                .orElseThrow(BoutNotFoundException::new);
        bout.updateSchedule(
                request.getTournamentId(),
                request.getRingId(),
                request.getBoutNumber(),
                request.getMatchType(),
                request.getRedAthleteId(),
                request.getBlueAthleteId(),
                request.getTotalRounds(),
                request.getScheduledOrder(),
                request.isEventBout()
        );

        return AdminBoutResponse.from(boutRepository.save(bout));
    }

    public void deleteBout(Long boutId) {
        validateBoutId(boutId);
        if (!boutRepository.existsById(boutId)) {
            throw new BoutNotFoundException();
        }

        boutRepository.deleteById(boutId);
    }

    private void validateRequest(AdminBoutRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("bout request is required");
        }
        validateRequiredFields(request);
        if (request.getRedAthleteId() != null && request.getRedAthleteId().equals(request.getBlueAthleteId())) {
            throw new IllegalArgumentException("redAthleteId and blueAthleteId must be different");
        }
        if (!tournamentRepository.existsById(request.getTournamentId())) {
            throw new TournamentNotFoundException();
        }

        Ring ring = ringRepository.findById(request.getRingId())
                .orElseThrow(RingNotFoundException::new);
        if (!request.getTournamentId().equals(ring.getTournamentId())) {
            throw new IllegalArgumentException("ring does not belong to tournament");
        }

        if (!athleteRepository.existsById(request.getRedAthleteId())
                || !athleteRepository.existsById(request.getBlueAthleteId())) {
            throw new AthleteNotFoundException();
        }
    }

    private void validateRequiredFields(AdminBoutRequest request) {
        if (request.getTournamentId() == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }
        if (request.getRingId() == null) {
            throw new IllegalArgumentException("ringId is required");
        }
        if (request.getBoutNumber() == null) {
            throw new IllegalArgumentException("boutNumber is required");
        }
        if (request.getBoutNumber() <= 0) {
            throw new IllegalArgumentException("boutNumber must be positive");
        }
        if (request.getRedAthleteId() == null) {
            throw new IllegalArgumentException("redAthleteId is required");
        }
        if (request.getBlueAthleteId() == null) {
            throw new IllegalArgumentException("blueAthleteId is required");
        }
        if (request.getTotalRounds() != null && request.getTotalRounds() <= 0) {
            throw new IllegalArgumentException("totalRounds must be positive");
        }
        if (request.getScheduledOrder() != null && request.getScheduledOrder() <= 0) {
            throw new IllegalArgumentException("scheduledOrder must be positive");
        }
    }

    private void validateBoutId(Long boutId) {
        if (boutId == null) {
            throw new IllegalArgumentException("boutId is required");
        }
    }
}
