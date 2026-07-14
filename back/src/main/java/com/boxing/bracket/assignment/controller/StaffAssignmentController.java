package com.boxing.bracket.assignment.controller;

import com.boxing.bracket.assignment.dto.StaffAssignmentActiveRequest;
import com.boxing.bracket.assignment.dto.StaffAssignmentRequest;
import com.boxing.bracket.assignment.dto.StaffAssignmentResponse;
import com.boxing.bracket.assignment.service.StaffAssignmentService;
import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.ringmanager.dto.RingManagerBoutResponse;
import com.boxing.bracket.user.domain.UserRole;
import org.springframework.context.annotation.Lazy;
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
@RequestMapping("/api/admin/assignments")
public class StaffAssignmentController {

    private final StaffAssignmentService staffAssignmentService;

    public StaffAssignmentController(@Lazy StaffAssignmentService staffAssignmentService) {
        this.staffAssignmentService = staffAssignmentService;
    }

    @GetMapping
    public ApiResponse<List<StaffAssignmentResponse>> getAssignments(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active
    ) {
        return ApiResponse.success(staffAssignmentService.getAssignments(tournamentId, accountId, role, active), "OK");
    }

    @PostMapping
    public ApiResponse<StaffAssignmentResponse> createAssignment(
            @Valid @RequestBody StaffAssignmentRequest request
    ) {
        return ApiResponse.success(staffAssignmentService.createAssignment(request), "OK");
    }

    @PutMapping("/{assignmentId}/active")
    public ApiResponse<StaffAssignmentResponse> changeActive(
            @PathVariable Long assignmentId,
            @Valid @RequestBody StaffAssignmentActiveRequest request
    ) {
        return ApiResponse.success(staffAssignmentService.changeActive(assignmentId, request), "OK");
    }

    @GetMapping("/rings/{ringId}/bouts")
    public ApiResponse<List<RingManagerBoutResponse>> getAssignedBouts(@PathVariable Long ringId) {
        return ApiResponse.success(staffAssignmentService.getAssignedBouts(ringId), "OK");
    }
}
