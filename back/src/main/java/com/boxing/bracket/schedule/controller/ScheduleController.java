package com.boxing.bracket.schedule.controller;

import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.schedule.dto.ScheduleResponse;
import com.boxing.bracket.schedule.service.ScheduleService;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(@Lazy ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public ApiResponse<List<ScheduleResponse>> getSchedules(@RequestParam Long tournamentId) {
        return ApiResponse.success(scheduleService.getSchedules(tournamentId), "OK");
    }
}
