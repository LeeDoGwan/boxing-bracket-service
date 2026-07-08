package com.boxing.bracket.home.service;

import com.boxing.bracket.bout.dto.BoutListResponse;
import com.boxing.bracket.bout.service.BoutService;
import com.boxing.bracket.home.dto.HomeResponse;
import com.boxing.bracket.ring.service.RingService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Lazy
@Transactional(readOnly = true)
public class HomeService {

    private final RingService ringService;
    private final BoutService boutService;

    public HomeService(@Lazy RingService ringService, @Lazy BoutService boutService) {
        this.ringService = ringService;
        this.boutService = boutService;
    }

    public HomeResponse getHome(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }

        List<BoutListResponse> confirmedResults = boutService.getOfficialBouts(tournamentId).stream()
                .filter(BoutListResponse::isResultConfirmed)
                .collect(Collectors.toList());

        return HomeResponse.of(
                tournamentId,
                ringService.getRingStatuses(tournamentId),
                confirmedResults
        );
    }
}
