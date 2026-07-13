package com.boxing.bracket.schedule.dto;

import com.boxing.bracket.schedule.domain.ScheduleItem;
import com.boxing.bracket.schedule.domain.ScheduleStatus;
import com.boxing.bracket.schedule.domain.ScheduleType;

import java.time.LocalDateTime;

public class ScheduleResponse {

    private final Long scheduleId;
    private final Long tournamentId;
    private final Long ringId;
    private final ScheduleType type;
    private final String title;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Long relatedBoutId;
    private final ScheduleStatus status;

    private ScheduleResponse(
            Long scheduleId,
            Long tournamentId,
            Long ringId,
            ScheduleType type,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long relatedBoutId,
            ScheduleStatus status
    ) {
        this.scheduleId = scheduleId;
        this.tournamentId = tournamentId;
        this.ringId = ringId;
        this.type = type;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.relatedBoutId = relatedBoutId;
        this.status = status;
    }

    public static ScheduleResponse from(ScheduleItem item) {
        return new ScheduleResponse(
                item.getId(),
                item.getTournamentId(),
                item.getRingId(),
                item.getType(),
                item.getTitle(),
                item.getStartTime(),
                item.getEndTime(),
                item.getRelatedBoutId(),
                item.getStatus()
        );
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public Long getRingId() {
        return ringId;
    }

    public ScheduleType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public Long getRelatedBoutId() {
        return relatedBoutId;
    }

    public ScheduleStatus getStatus() {
        return status;
    }
}
