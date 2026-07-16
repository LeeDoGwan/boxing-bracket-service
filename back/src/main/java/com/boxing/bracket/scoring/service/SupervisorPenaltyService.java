package com.boxing.bracket.scoring.service;

import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.assignment.service.StaffAssignmentService;
import com.boxing.bracket.scoring.domain.Penalty;
import com.boxing.bracket.scoring.dto.PenaltyCreateRequest;
import com.boxing.bracket.scoring.dto.PenaltyResponse;
import com.boxing.bracket.scoring.repository.PenaltyRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Lazy
@Transactional
public class SupervisorPenaltyService {

    private final BoutRepository boutRepository;
    private final PenaltyRepository penaltyRepository;
    private final StaffAssignmentService staffAssignmentService;

    public SupervisorPenaltyService(BoutRepository boutRepository, PenaltyRepository penaltyRepository) {
        this(boutRepository, penaltyRepository, null);
    }

    @Autowired
    public SupervisorPenaltyService(
            BoutRepository boutRepository,
            PenaltyRepository penaltyRepository,
            StaffAssignmentService staffAssignmentService
    ) {
        this.boutRepository = boutRepository;
        this.penaltyRepository = penaltyRepository;
        this.staffAssignmentService = staffAssignmentService;
    }

    public PenaltyResponse createPenalty(Long boutId, PenaltyCreateRequest request) {
        validateBoutId(boutId);
        validateRequest(request);

        if (staffAssignmentService != null) {
            staffAssignmentService.requireBoutAccess(boutId);
        }

        if (!boutRepository.existsById(boutId)) {
            throw new BoutNotFoundException();
        }

        Penalty penalty = Penalty.builder()
                .boutId(boutId)
                .targetSide(request.getTargetSide())
                .penaltyPoint(request.getPenaltyPoint())
                .reason(request.getReason())
                .createdBy(request.getCreatedBy())
                .build();

        return PenaltyResponse.from(penaltyRepository.save(penalty));
    }

    @Transactional(readOnly = true)
    public List<PenaltyResponse> getPenalties(Long boutId) {
        validateBoutId(boutId);
        if (staffAssignmentService != null) {
            staffAssignmentService.requireBoutAccess(boutId);
        }
        if (!boutRepository.existsById(boutId)) {
            throw new BoutNotFoundException();
        }

        return penaltyRepository.findByBoutIdOrderByCreatedAtAscIdAsc(boutId).stream()
                .map(PenaltyResponse::from)
                .collect(Collectors.toList());
    }

    private void validateBoutId(Long boutId) {
        if (boutId == null) {
            throw new IllegalArgumentException("boutId is required");
        }
    }

    private void validateRequest(PenaltyCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("penalty request is required");
        }
        if (request.getTargetSide() == null) {
            throw new IllegalArgumentException("targetSide is required");
        }
        if (request.getPenaltyPoint() == null) {
            throw new IllegalArgumentException("penaltyPoint is required");
        }
        if (request.getCreatedBy() == null) {
            throw new IllegalArgumentException("createdBy is required");
        }
    }
}
