package com.boxing.bracket.home.dto;

import com.boxing.bracket.bout.dto.BoutListResponse;
import com.boxing.bracket.notice.dto.NoticeResponse;
import com.boxing.bracket.ring.dto.RingStatusResponse;

import java.util.List;

public class HomeResponse {

    private final Long tournamentId;
    private final List<NoticeResponse> notices;
    private final List<RingStatusResponse> ringStatuses;
    private final List<BoutListResponse> confirmedResults;

    private HomeResponse(
            Long tournamentId,
            List<NoticeResponse> notices,
            List<RingStatusResponse> ringStatuses,
            List<BoutListResponse> confirmedResults
    ) {
        this.tournamentId = tournamentId;
        this.notices = notices;
        this.ringStatuses = ringStatuses;
        this.confirmedResults = confirmedResults;
    }

    public static HomeResponse of(
            Long tournamentId,
            List<NoticeResponse> notices,
            List<RingStatusResponse> ringStatuses,
            List<BoutListResponse> confirmedResults
    ) {
        return new HomeResponse(tournamentId, notices, ringStatuses, confirmedResults);
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public List<NoticeResponse> getNotices() {
        return notices;
    }

    public List<RingStatusResponse> getRingStatuses() {
        return ringStatuses;
    }

    public List<BoutListResponse> getConfirmedResults() {
        return confirmedResults;
    }
}
