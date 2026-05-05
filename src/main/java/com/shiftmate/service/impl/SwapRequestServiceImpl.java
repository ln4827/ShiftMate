package com.shiftmate.service.impl;

import com.shiftmate.dto.CreateSwapRequest;
import com.shiftmate.dto.SwapRequestResponse;
import com.shiftmate.entity.*;
import com.shiftmate.exception.BusinessRuleException;
import com.shiftmate.exception.ResourceNotFoundException;
import com.shiftmate.repository.EmployeeRepository;
import com.shiftmate.repository.ShiftAssignmentRepository;
import com.shiftmate.repository.SwapRequestRepository;
import com.shiftmate.service.NotificationService;
import com.shiftmate.service.SwapRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SwapRequestServiceImpl implements SwapRequestService {

    private final SwapRequestRepository swapRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public SwapRequestResponse createSwapRequest(Long employeeId, Long restaurantId, CreateSwapRequest req) {
        ShiftAssignment requester = assignmentRepository.findById(req.getRequesterAssignmentId())
                .filter(a -> a.getEmployee().getId().equals(employeeId))
                .orElseThrow(() -> new ResourceNotFoundException("ShiftAssignment", req.getRequesterAssignmentId()));

        ShiftAssignment target = assignmentRepository.findById(req.getTargetAssignmentId())
                .filter(a -> a.getShift().getDepartment().getRestaurant().getId().equals(restaurantId))
                .orElseThrow(() -> new ResourceNotFoundException("ShiftAssignment", req.getTargetAssignmentId()));

        if (requester.getEmployee().getId().equals(target.getEmployee().getId())) {
            throw new BusinessRuleException("Cannot request a swap with yourself.");
        }
        if (!requester.getShift().isPublished() || !target.getShift().isPublished()) {
            throw new BusinessRuleException("Both shifts must be published to request a swap.");
        }
        if (swapRepository.existsByRequesterAssignmentIdAndTargetAssignmentIdAndStatus(
                requester.getId(), target.getId(), SwapRequest.Status.PENDING)) {
            throw new BusinessRuleException("A pending swap request already exists for these assignments.");
        }

        SwapRequest swap = SwapRequest.builder()
                .requesterAssignment(requester)
                .targetAssignment(target)
                .build();
        swapRepository.save(swap);

        notificationService.send(target.getEmployee().getId(), Notification.Type.SWAP_REQUESTED,
                requester.getEmployee().getFullName() + " wants to swap their "
                + requester.getShift().getShiftDate() + " shift with your "
                + target.getShift().getShiftDate() + " shift.");

        log.info("Employee id={} requested swap: assignment {} ↔ {}", employeeId,
                requester.getId(), target.getId());

        return loadResponse(swap.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SwapRequestResponse> getMySwapRequests(Long employeeId) {
        return swapRepository.findByEmployeeId(employeeId).stream()
                .map(SwapRequestResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SwapRequestResponse> getPendingForManager(Long restaurantId) {
        return swapRepository.findPendingByRestaurantId(restaurantId).stream()
                .map(SwapRequestResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public SwapRequestResponse approve(Long restaurantId, Long swapRequestId, Long managerId) {
        SwapRequest swap = resolveForRestaurant(restaurantId, swapRequestId);
        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", managerId));

        ShiftAssignment ra = swap.getRequesterAssignment();
        ShiftAssignment ta = swap.getTargetAssignment();

        validateNoOverlapAfterSwap(ra, ta);

        // Atomically swap employees between the two ShiftAssignments (FR-15, AT-09)
        Employee originalRequester = ra.getEmployee();
        Employee originalTarget    = ta.getEmployee();
        ra.setEmployee(originalTarget);
        ta.setEmployee(originalRequester);
        assignmentRepository.save(ra);
        assignmentRepository.save(ta);

        swap.setStatus(SwapRequest.Status.APPROVED);
        swap.setResolvedAt(LocalDateTime.now());
        swap.setResolvedBy(manager);
        swapRepository.save(swap);

        notificationService.send(originalRequester.getId(), Notification.Type.SWAP_APPROVED,
                "Your swap request for the " + ra.getShift().getShiftDate() + " shift has been approved.");
        notificationService.send(originalTarget.getId(), Notification.Type.SWAP_APPROVED,
                "A shift swap involving your " + ta.getShift().getShiftDate() + " shift has been approved.");

        log.info("Manager id={} approved swap request id={}", managerId, swapRequestId);
        return loadResponse(swapRequestId);
    }

    @Override
    @Transactional
    public SwapRequestResponse reject(Long restaurantId, Long swapRequestId, Long managerId) {
        SwapRequest swap = resolveForRestaurant(restaurantId, swapRequestId);
        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", managerId));

        swap.setStatus(SwapRequest.Status.REJECTED);
        swap.setResolvedAt(LocalDateTime.now());
        swap.setResolvedBy(manager);
        swapRepository.save(swap);

        notificationService.send(swap.getRequesterAssignment().getEmployee().getId(),
                Notification.Type.SWAP_REJECTED,
                "Your swap request for the "
                + swap.getRequesterAssignment().getShift().getShiftDate()
                + " shift has been rejected.");

        log.info("Manager id={} rejected swap request id={}", managerId, swapRequestId);
        return loadResponse(swapRequestId);
    }

    // -------------------------------------------------------------------------

    private SwapRequest resolveForRestaurant(Long restaurantId, Long swapRequestId) {
        SwapRequest swap = swapRepository.findById(swapRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("SwapRequest", swapRequestId));

        Long swapRestaurantId = swap.getRequesterAssignment()
                .getShift().getDepartment().getRestaurant().getId();
        if (!swapRestaurantId.equals(restaurantId)) {
            throw new ResourceNotFoundException("SwapRequest", swapRequestId);
        }
        if (swap.getStatus() != SwapRequest.Status.PENDING) {
            throw new BusinessRuleException(
                    "Swap request has already been " + swap.getStatus().name().toLowerCase() + ".");
        }
        return swap;
    }

    private void validateNoOverlapAfterSwap(ShiftAssignment ra, ShiftAssignment ta) {
        Employee requesterEmp = ra.getEmployee();
        Employee targetEmp    = ta.getEmployee();

        if (!assignmentRepository.findOverlappingAssignments(
                targetEmp.getId(), ra.getShift().getShiftDate(),
                ra.getShift().getStartTime(), ra.getShift().getEndTime(), ra.getId()).isEmpty()) {
            throw new BusinessRuleException(targetEmp.getFullName()
                    + " has an overlapping shift and cannot take " + requesterEmp.getFullName() + "'s shift.");
        }
        if (!assignmentRepository.findOverlappingAssignments(
                requesterEmp.getId(), ta.getShift().getShiftDate(),
                ta.getShift().getStartTime(), ta.getShift().getEndTime(), ta.getId()).isEmpty()) {
            throw new BusinessRuleException(requesterEmp.getFullName()
                    + " has an overlapping shift and cannot take " + targetEmp.getFullName() + "'s shift.");
        }
    }

    private SwapRequestResponse loadResponse(Long swapRequestId) {
        return swapRepository.findByIdWithDetails(swapRequestId)
                .map(SwapRequestResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("SwapRequest", swapRequestId));
    }
}
