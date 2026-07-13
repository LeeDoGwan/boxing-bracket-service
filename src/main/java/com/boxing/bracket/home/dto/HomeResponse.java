package com.boxing.bracket.home.dto;

import com.boxing.bracket.bout.dto.BoutListResponse;
import com.boxing.bracket.notice.dto.NoticeResponse;
import com.boxing.bracket.ring.dto.RingStatusResponse;
import com.boxing.bracket.schedule.dto.ScheduleResponse;

import java.util.List;

public class HomeResponse {

    private final Long tournamentId;
    private final List<NoticeResponse> notices;
    private final List<RingStatusResponse> ringStatuses;
    private final List<BoutListResponse> confirmedResults;
    private final List<ScheduleResponse> schedules;

    private HomeResponse(
            Long tournamentId,
            List<NoticeResponse> notices,
            List<RingStatusResponse> ringStatuses,
            List<BoutListResponse> confirmedResults,
            List<ScheduleResponse> schedules
    ) {
        this.tournamentId = tournamentId;
        this.notices = notices;
        this.ringStatuses = ringStatuses;
        this.confirmedResults = confirmedResults;
        this.schedules = schedules;
    }

    public static HomeResponse of(
            Long tournamentId,
            List<NoticeResponse> notices,
            List<RingStatusResponse> ringStatuses,
            List<BoutListResponse> confirmedResults
    ) {
        return new HomeResponse(tournamentId, notices, ringStatuses, confirmedResults, java.util.Collections.emptyList());
    }

    public static HomeResponse of(
            Long tournamentId,
            List<NoticeResponse> notices,
            List<RingStatusResponse> ringStatuses,
            List<BoutListResponse> confirmedResults,
            List<ScheduleResponse> schedules
    ) {
        return new HomeResponse(tournamentId, notices, ringStatuses, confirmedResults, schedules);
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

    public List<ScheduleResponse> getSchedules() {
        return schedules;
    }
}
