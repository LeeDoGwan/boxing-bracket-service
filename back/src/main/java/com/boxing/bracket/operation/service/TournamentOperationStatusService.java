package com.boxing.bracket.operation.service;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.operation.dto.JudgeScoreSubmissionStatusResponse;
import com.boxing.bracket.operation.dto.OperationBoutResponse;
import com.boxing.bracket.operation.dto.OperationRingStatusResponse;
import com.boxing.bracket.operation.dto.TournamentOperationStatusResponse;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.repository.RingRepository;
import com.boxing.bracket.scoring.domain.RoundScore;
import com.boxing.bracket.scoring.repository.RoundScoreRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Lazy
@Transactional(readOnly = true)
public class TournamentOperationStatusService {

    private static final long STALLED_BOUT_THRESHOLD_MINUTES = 15L;

    private final BoutRepository boutRepository;
    private final RingRepository ringRepository;
    private final RoundScoreRepository roundScoreRepository;

    public TournamentOperationStatusService(
            BoutRepository boutRepository,
            RingRepository ringRepository,
            RoundScoreRepository roundScoreRepository
    ) {
        this.boutRepository = boutRepository;
        this.ringRepository = ringRepository;
        this.roundScoreRepository = roundScoreRepository;
    }

    public TournamentOperationStatusResponse getStatus(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }

        List<Bout> officialBouts = boutRepository.findByTournamentIdOrderByScheduledOrderAsc(tournamentId).stream()
                .filter(bout -> !bout.isEventBout())
                .collect(Collectors.toList());
        Map<Long, List<Bout>> boutsByRingId = officialBouts.stream()
                .collect(Collectors.groupingBy(Bout::getRingId));
        List<RoundScore> roundScores = findRoundScores(officialBouts);

        return TournamentOperationStatusResponse.of(
                tournamentId,
                officialBouts.size(),
                countBoutsByStatus(officialBouts),
                getRingStatuses(tournamentId, boutsByRingId),
                getJudgeScoreSubmissionStatuses(officialBouts, roundScores),
                getPendingResultBouts(officialBouts),
                getStalledBouts(officialBouts)
        );
    }

    private List<RoundScore> findRoundScores(List<Bout> officialBouts) {
        if (officialBouts.isEmpty()) {
            return List.of();
        }

        List<Long> boutIds = officialBouts.stream()
                .map(Bout::getId)
                .collect(Collectors.toList());
        return roundScoreRepository.findByBoutIdIn(boutIds);
    }

    private Map<BoutStatus, Integer> countBoutsByStatus(Collection<Bout> bouts) {
        Map<BoutStatus, Integer> counts = new EnumMap<>(BoutStatus.class);
        for (BoutStatus status : BoutStatus.values()) {
            counts.put(status, 0);
        }
        for (Bout bout : bouts) {
            counts.compute(bout.getStatus(), (status, count) -> count + 1);
        }
        return counts;
    }

    private List<OperationRingStatusResponse> getRingStatuses(
            Long tournamentId,
            Map<Long, List<Bout>> boutsByRingId
    ) {
        return ringRepository.findByTournamentIdOrderByIdAsc(tournamentId).stream()
                .map(ring -> toRingStatusResponse(ring, boutsByRingId.getOrDefault(ring.getId(), List.of())))
                .collect(Collectors.toList());
    }

    private OperationRingStatusResponse toRingStatusResponse(Ring ring, List<Bout> bouts) {
        Optional<Bout> currentBout = findCurrentBout(ring, bouts);
        Optional<Bout> nextBout = findNextBout(currentBout, bouts);
        return OperationRingStatusResponse.of(
                ring,
                currentBout.map(OperationBoutResponse::from).orElse(null),
                nextBout.map(OperationBoutResponse::from).orElse(null)
        );
    }

    private Optional<Bout> findCurrentBout(Ring ring, List<Bout> bouts) {
        if (ring.getCurrentBoutId() != null) {
            Optional<Bout> currentBout = bouts.stream()
                    .filter(bout -> ring.getCurrentBoutId().equals(bout.getId()))
                    .findFirst();
            if (currentBout.isPresent()) {
                return currentBout;
            }
        }

        return bouts.stream()
                .filter(bout -> bout.getStatus() == BoutStatus.IN_PROGRESS)
                .findFirst();
    }

    private Optional<Bout> findNextBout(Optional<Bout> currentBout, List<Bout> bouts) {
        if (currentBout.isPresent() && currentBout.get().getScheduledOrder() != null) {
            int currentScheduledOrder = currentBout.get().getScheduledOrder();
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

    private boolean isPreviewStatus(Bout bout) {
        return bout.getStatus() == BoutStatus.SCHEDULED || bout.getStatus() == BoutStatus.READY;
    }

    private List<JudgeScoreSubmissionStatusResponse> getJudgeScoreSubmissionStatuses(
            List<Bout> officialBouts,
            List<RoundScore> roundScores
    ) {
        Map<Long, Map<Integer, List<RoundScore>>> scoresByBoutAndRound = roundScores.stream()
                .collect(Collectors.groupingBy(
                        RoundScore::getBoutId,
                        Collectors.groupingBy(RoundScore::getRoundNo)
                ));
        List<JudgeScoreSubmissionStatusResponse> responses = new ArrayList<>();

        for (Bout bout : officialBouts) {
            Map<Integer, List<RoundScore>> scoresByRound = scoresByBoutAndRound.get(bout.getId());
            if (scoresByRound == null) {
                continue;
            }
            scoresByRound.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> JudgeScoreSubmissionStatusResponse.of(bout, entry.getKey(), entry.getValue()))
                    .forEach(responses::add);
        }
        return responses;
    }

    private List<OperationBoutResponse> getPendingResultBouts(List<Bout> officialBouts) {
        return officialBouts.stream()
                .filter(bout -> bout.getStatus() == BoutStatus.FINISHED)
                .filter(bout -> !bout.isResultConfirmed())
                .map(OperationBoutResponse::from)
                .collect(Collectors.toList());
    }

    private List<OperationBoutResponse> getStalledBouts(List<Bout> officialBouts) {
        LocalDateTime stalledBefore = LocalDateTime.now().minusMinutes(STALLED_BOUT_THRESHOLD_MINUTES);
        return officialBouts.stream()
                .filter(bout -> bout.getStatus() == BoutStatus.IN_PROGRESS)
                .filter(bout -> bout.getStartedAt() != null && bout.getStartedAt().isBefore(stalledBefore))
                .map(OperationBoutResponse::from)
                .collect(Collectors.toList());
    }
}
