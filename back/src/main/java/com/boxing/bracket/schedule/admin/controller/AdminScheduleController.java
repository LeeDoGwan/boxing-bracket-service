package com.boxing.bracket.schedule.admin.controller;

import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.schedule.admin.dto.ScheduleRequest;
import com.boxing.bracket.schedule.admin.service.AdminScheduleService;
import com.boxing.bracket.schedule.dto.ScheduleResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/schedules")
public class AdminScheduleController {

    private final AdminScheduleService adminScheduleService;

    public AdminScheduleController(@Lazy AdminScheduleService adminScheduleService) {
        this.adminScheduleService = adminScheduleService;
    }

    @GetMapping
    public ApiResponse<List<ScheduleResponse>> getSchedules(@RequestParam Long tournamentId) {
        return ApiResponse.success(adminScheduleService.getSchedules(tournamentId), "OK");
    }

    @GetMapping("/{scheduleId}")
    public ApiResponse<ScheduleResponse> getSchedule(@PathVariable Long scheduleId) {
        return ApiResponse.success(adminScheduleService.getSchedule(scheduleId), "OK");
    }

    @PostMapping
    public ApiResponse<ScheduleResponse> createSchedule(@Valid @RequestBody ScheduleRequest request) {
        return ApiResponse.success(adminScheduleService.createSchedule(request), "OK");
    }

    @PutMapping("/{scheduleId}")
    public ApiResponse<ScheduleResponse> updateSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleRequest request
    ) {
        return ApiResponse.success(adminScheduleService.updateSchedule(scheduleId, request), "OK");
    }

    @DeleteMapping("/{scheduleId}")
    public ApiResponse<Void> deleteSchedule(@PathVariable Long scheduleId) {
        adminScheduleService.deleteSchedule(scheduleId);
        return ApiResponse.success(null, "OK");
    }
}
