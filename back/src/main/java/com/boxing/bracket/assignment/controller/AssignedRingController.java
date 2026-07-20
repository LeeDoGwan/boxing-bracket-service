package com.boxing.bracket.assignment.controller;

import com.boxing.bracket.assignment.dto.AssignedRingResponse;
import com.boxing.bracket.assignment.service.StaffAssignmentService;
import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.ringmanager.dto.RingManagerBoutResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/staff/assignments")
public class AssignedRingController {

    private final StaffAssignmentService staffAssignmentService;

    public AssignedRingController(@Lazy StaffAssignmentService staffAssignmentService) {
        this.staffAssignmentService = staffAssignmentService;
    }

    @GetMapping("/rings")
    public ApiResponse<List<AssignedRingResponse>> getAssignedRings(@RequestParam Long tournamentId) {
        return ApiResponse.success(staffAssignmentService.getAssignedRings(tournamentId), "OK");
    }

    @GetMapping("/rings/{ringId}/bouts")
    public ApiResponse<List<RingManagerBoutResponse>> getAssignedBouts(@PathVariable Long ringId) {
        return ApiResponse.success(staffAssignmentService.getAssignedBouts(ringId), "OK");
    }
}
