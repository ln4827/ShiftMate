package com.shiftmate.service.impl;

import com.shiftmate.dto.AssignEmployeeRequest;
import com.shiftmate.dto.ShiftResponse;
import com.shiftmate.entity.*;
import com.shiftmate.exception.BusinessRuleException;
import com.shiftmate.exception.ResourceNotFoundException;
import com.shiftmate.repository.*;
import com.shiftmate.service.ShiftAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftAssignmentServiceImpl implements ShiftAssignmentService {

    private final ShiftRepository shiftRepository;
    private final ShiftAssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final TimeOffRequestRepository timeOffRequestRepository;

    @Override
    @Transactional
    public ShiftResponse assign(Long restaurantId, Long shiftId, AssignEmployeeRequest request) {
        Shift shift    = resolveShift(restaurantId, shiftId);
        Employee emp   = resolveActiveEmployee(restaurantId, request.getEmployeeId());
        Role role      = resolveRole(restaurantId, request.getRoleId());

        if (!employeeRoleRepository.existsByEmployeeIdAndRoleId(emp.getId(), role.getId())) {
            throw new BusinessRuleException(
                    "Employee '" + emp.getFullName() + "' does not have role '" + role.getName() + "'.");
        }

        if (assignmentRepository.existsByShiftIdAndEmployeeId(shiftId, emp.getId())) {
            throw new BusinessRuleException(
                    "Employee '" + emp.getFullName() + "' is already assigned to this shift.");
        }

        List<ShiftAssignment> overlaps = assignmentRepository.findOverlappingAssignments(
                emp.getId(), shift.getShiftDate(), shift.getStartTime(), shift.getEndTime(), null);
        if (!overlaps.isEmpty()) {
            throw new BusinessRuleException(
                    "Employee '" + emp.getFullName() + "' has an overlapping shift on " + shift.getShiftDate() + ".");
        }

        if (!timeOffRequestRepository.findApprovedOverlapping(emp.getId(), shift.getShiftDate()).isEmpty()) {
            throw new BusinessRuleException(
                    "Employee '" + emp.getFullName() + "' has approved time off on " + shift.getShiftDate() +
                    ". Reject the time-off request before assigning.");
        }

        ShiftAssignment assignment = ShiftAssignment.builder()
                .shift(shift)
                .employee(emp)
                .role(role)
                .build();
        assignmentRepository.save(assignment);

        log.info("Assigned employee id={} to shift id={} in role '{}'",
                emp.getId(), shiftId, role.getName());

        return ShiftResponse.from(resolveShiftWithCoverage(restaurantId, shiftId));
    }

    @Override
    @Transactional
    public void unassign(Long restaurantId, Long shiftId, Long assignmentId) {
        resolveShift(restaurantId, shiftId);

        ShiftAssignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> a.getShift().getId().equals(shiftId))
                .orElseThrow(() -> new ResourceNotFoundException("ShiftAssignment", assignmentId));

        boolean hasPendingSwaps =
                assignment.getOutgoingSwapRequests().stream()
                        .anyMatch(sr -> sr.getStatus() == SwapRequest.Status.PENDING)
                || assignment.getIncomingSwapRequests().stream()
                        .anyMatch(sr -> sr.getStatus() == SwapRequest.Status.PENDING);

        if (hasPendingSwaps) {
            throw new BusinessRuleException(
                    "Cannot remove assignment: there is a pending swap request involving this assignment.");
        }

        assignmentRepository.delete(assignment);
        log.info("Removed assignment id={} from shift id={}", assignmentId, shiftId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Shift resolveShift(Long restaurantId, Long shiftId) {
        return shiftRepository.findById(shiftId)
                .filter(s -> s.getDepartment().getRestaurant().getId().equals(restaurantId))
                .orElseThrow(() -> new ResourceNotFoundException("Shift", shiftId));
    }

    private Shift resolveShiftWithCoverage(Long restaurantId, Long shiftId) {
        return shiftRepository.findByIdWithCoverageAndAssignments(shiftId)
                .filter(s -> s.getDepartment().getRestaurant().getId().equals(restaurantId))
                .orElseThrow(() -> new ResourceNotFoundException("Shift", shiftId));
    }

    private Employee resolveActiveEmployee(Long restaurantId, Long employeeId) {
        return employeeRepository.findById(employeeId)
                .filter(e -> e.getRestaurant().getId().equals(restaurantId) && e.isActive())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
    }

    private Role resolveRole(Long restaurantId, Long roleId) {
        return roleRepository.findById(roleId)
                .filter(r -> r.getRestaurant().getId().equals(restaurantId))
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
    }
}
