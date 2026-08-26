package com.boxing.bracket.ring.service;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.athlete.repository.AthleteRepository;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.dto.BoutDetailResponse;
import com.boxing.bracket.bout.dto.BoutListResponse;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.ring.exception.RingNotFoundException;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.dto.RingBoutSummaryResponse;
import com.boxing.bracket.ring.dto.RingStatusResponse;
import com.boxing.bracket.ring.repository.RingRepository;
import com.boxing.bracket.scoring.repository.BoutResultRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Lazy
@Transactional(readOnly = true)
public class RingService {

    private final RingRepository ringRepository;
    private final BoutRepository boutRepository;
    private final AthleteRepository athleteRepository;
    private final BoutResultRepository boutResultRepository;

    public RingService(
            RingRepository ringRepository,
            BoutRepository boutRepository,
            AthleteRepository athleteRepository,
            BoutResultRepository boutResultRepository
    ) {
        this.ringRepository = ringRepository;
        this.boutRepository = boutRepository;
        this.athleteRepository = athleteRepository;
        this.boutResultRepository = boutResultRepository;
    }

    public List<RingStatusResponse> getRingStatuses(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }

        return ringRepository.findByTournamentIdOrderByIdAsc(tournamentId).stream()
                .map(this::toRingStatusResponse)
                .collect(Collectors.toList());
    }

    public List<RingStatusResponse> getRingStatuses(Long tournamentId, List<BoutListResponse> officialBouts) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }

        return ringRepository.findByTournamentIdOrderByIdAsc(tournamentId).stream()
                .map(ring -> toRingStatusResponse(ring, officialBouts))
                .collect(Collectors.toList());
    }

    public BoutDetailResponse getCurrentBout(Long ringId) {
        if (ringId == null) {
            throw new IllegalArgumentException("ringId is required");
        }

        Ring ring = ringRepository.findById(ringId)
                .orElseThrow(RingNotFoundException::new);
        List<Bout> officialBouts = getOfficialBoutsByRingId(ring.getId());

        return findCurrentBout(ring, officialBouts)
                .map(this::toBoutDetailResponse)
                .orElse(null);
    }

    private RingStatusResponse toRingStatusResponse(Ring ring) {
        List<Bout> officialBouts = getOfficialBoutsByRingId(ring.getId());

        Optional<Bout> currentBout = findCurrentBout(ring, officialBouts);
        Optional<Bout> nextBout = findNextBout(currentBout, officialBouts);

        return RingStatusResponse.of(
                ring,
                currentBout.map(this::toBoutSummaryResponse).orElse(null),
                nextBout.map(this::toBoutSummaryResponse).orElse(null)
        );
    }

    private RingStatusResponse toRingStatusResponse(Ring ring, List<BoutListResponse> officialBouts) {
        List<BoutListResponse> ringBouts = officialBouts.stream()
                .filter(bout -> ring.getId().equals(bout.getRingId()))
                .collect(Collectors.toList());
        Optional<BoutListResponse> currentBout = findCurrentBoutSummary(ring, ringBouts);
        Optional<BoutListResponse> nextBout = findNextBoutSummary(currentBout, ringBouts);

        return RingStatusResponse.of(
                ring,
                currentBout.map(RingBoutSummaryResponse::of).orElse(null),
                nextBout.map(RingBoutSummaryResponse::of).orElse(null)
        );
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

    private Optional<Bout> findNextBout(Optional<Bout> currentBout, List<Bout> bouts) {
        if (currentBout.isPresent()) {
            Integer currentScheduledOrder = currentBout.get().getScheduledOrder();
            if (currentScheduledOrder == null) {
                return Optional.empty();
            }

            return bouts.stream()
                    .filter(bout -> bout.getScheduledOrder() != null)
                    .filter(bout -> bout.getScheduledOrder() > currentScheduledOrder)
                    .min(Comparator.comparing(Bout::getScheduledOrder));
        }

        return bouts.stream()
                .filter(this::isPreviewStatus)
                .filter(bout -> bout.getScheduledOrder() != null)
                .min(Comparator.comparing(Bout::getScheduledOrder));
    }

    private Optional<BoutListResponse> findCurrentBoutSummary(Ring ring, List<BoutListResponse> bouts) {
        if (ring.getCurrentBoutId() != null) {
            Optional<BoutListResponse> currentByRing = bouts.stream()
                    .filter(bout -> ring.getCurrentBoutId().equals(bout.getBoutId()))
                    .findFirst();
            if (currentByRing.isPresent()) {
                return currentByRing;
            }
        }

        return bouts.stream()
                .filter(bout -> bout.getStatus() == BoutStatus.IN_PROGRESS)
                .findFirst();
    }

    private Optional<BoutListResponse> findNextBoutSummary(Optional<BoutListResponse> currentBout, List<BoutListResponse> bouts) {
        if (currentBout.isPresent()) {
            Integer currentScheduledOrder = currentBout.get().getScheduledOrder();
            if (currentScheduledOrder == null) {
                return Optional.empty();
            }

            return bouts.stream()
                    .filter(bout -> bout.getScheduledOrder() != null)
                    .filter(bout -> bout.getScheduledOrder() > currentScheduledOrder)
                    .min(Comparator.comparing(BoutListResponse::getScheduledOrder));
        }

        return bouts.stream()
                .filter(bout -> bout.getStatus() == BoutStatus.SCHEDULED || bout.getStatus() == BoutStatus.READY)
                .filter(bout -> bout.getScheduledOrder() != null)
                .min(Comparator.comparing(BoutListResponse::getScheduledOrder));
    }

    private boolean isPreviewStatus(Bout bout) {
        return bout.getStatus() == BoutStatus.SCHEDULED || bout.getStatus() == BoutStatus.READY;
    }

    private RingBoutSummaryResponse toBoutSummaryResponse(Bout bout) {
        return RingBoutSummaryResponse.of(
                bout,
                getAthlete(bout.getTournamentId(), bout.getRedAthleteId()),
                getAthlete(bout.getTournamentId(), bout.getBlueAthleteId())
        );
    }

    private BoutDetailResponse toBoutDetailResponse(Bout bout) {
        return BoutDetailResponse.of(
                bout,
                getAthlete(bout.getTournamentId(), bout.getRedAthleteId()),
                getAthlete(bout.getTournamentId(), bout.getBlueAthleteId()),
                boutResultRepository.findByBoutId(bout.getId()).orElse(null)
        );
    }

    private List<Bout> getOfficialBoutsByRingId(Long ringId) {
        return boutRepository.findByRingIdOrderByScheduledOrderAsc(ringId).stream()
                .filter(bout -> !bout.isEventBout())
                .collect(Collectors.toList());
    }

    private Athlete getAthlete(Long tournamentId, Long athleteId) {
        return athleteRepository.findById(athleteId)
                .filter(athlete -> athlete.getTournamentId() == null
                        || tournamentId.equals(athlete.getTournamentId()))
                .orElseThrow(() -> new IllegalStateException("Athlete not found"));
    }
}
