package com.boxing.bracket.schedule.admin.dto;

import com.boxing.bracket.schedule.domain.ScheduleStatus;
import com.boxing.bracket.schedule.domain.ScheduleType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class ScheduleRequest {

    @NotNull(message = "tournamentId is required")
    private Long tournamentId;

    private Long ringId;

    @NotNull(message = "type is required")
    private ScheduleType type;

    @NotBlank(message = "title is required")
    private String title;

    @NotNull(message = "startTime is required")
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long relatedBoutId;

    private ScheduleStatus status;

    protected ScheduleRequest() {
    }

    public ScheduleRequest(
            Long tournamentId,
            Long ringId,
            ScheduleType type,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long relatedBoutId,
            ScheduleStatus status
    ) {
        this.tournamentId = tournamentId;
        this.ringId = ringId;
        this.type = type;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.relatedBoutId = relatedBoutId;
        this.status = status;
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
