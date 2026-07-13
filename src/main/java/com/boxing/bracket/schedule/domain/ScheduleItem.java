package com.boxing.bracket.schedule.domain;

import com.boxing.bracket.common.entity.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "schedule_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tournamentId;

    private Long ringId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long relatedBoutId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status = ScheduleStatus.SCHEDULED;

    @Builder
    private ScheduleItem(
            Long tournamentId,
            Long ringId,
            ScheduleType type,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long relatedBoutId,
            ScheduleStatus status
    ) {
        validate(tournamentId, type, title, startTime, endTime);
        this.tournamentId = tournamentId;
        this.ringId = ringId;
        this.type = type;
        this.title = title.trim();
        this.startTime = startTime;
        this.endTime = endTime;
        this.relatedBoutId = relatedBoutId;
        this.status = status == null ? ScheduleStatus.SCHEDULED : status;
    }

    public void updateInfo(
            Long tournamentId,
            Long ringId,
            ScheduleType type,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long relatedBoutId,
            ScheduleStatus status
    ) {
        validate(tournamentId, type, title, startTime, endTime);
        this.tournamentId = tournamentId;
        this.ringId = ringId;
        this.type = type;
        this.title = title.trim();
        this.startTime = startTime;
        this.endTime = endTime;
        this.relatedBoutId = relatedBoutId;
        if (status != null) {
            this.status = status;
        }
    }

    private void validate(
            Long tournamentId,
            ScheduleType type,
            String title,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("title is required");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("startTime is required");
        }
        if (endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime must not be before startTime");
        }
    }
}
