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

import java.util.List;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public List<AdminBoutResponse> getBouts(Long tournamentId) {
        validateTournamentId(tournamentId);
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException();
        }

        return boutRepository.findByTournamentIdOrderByScheduledOrderAsc(tournamentId).stream()
                .map(AdminBoutResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminBoutResponse getBout(Long boutId) {
        validateBoutId(boutId);
        return boutRepository.findById(boutId)
                .map(AdminBoutResponse::from)
                .orElseThrow(BoutNotFoundException::new);
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
        validateTournamentId(request.getTournamentId());
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

    private void validateTournamentId(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }
    }

    private void validateBoutId(Long boutId) {
        if (boutId == null) {
            throw new IllegalArgumentException("boutId is required");
        }
    }
}
