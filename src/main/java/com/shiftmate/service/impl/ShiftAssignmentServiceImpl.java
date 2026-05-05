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

import java.time.LocalTime;
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

        checkForConflicts(emp, shift);

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

    /**
     * Checks that assigning {@code emp} to {@code shift} creates no time-overlap
     * with an existing assignment. Handles three distinct cases:
     * <ol>
     *   <li>Same-date overlaps including overnight <em>existing</em> shifts (delegated to the
     *       updated JPQL query in {@link ShiftAssignmentRepository}).</li>
     *   <li>Overnight <em>proposed</em> shifts — the JPQL end-time comparison breaks when
     *       {@code endTime < startTime}, so the pre-midnight and post-midnight windows are
     *       checked separately.</li>
     *   <li>A previous-day overnight shift whose tail bleeds into the target date.</li>
     * </ol>
     */
    private void checkForConflicts(Employee emp, Shift shift) {
        LocalDate date = shift.getShiftDate();
        String name = emp.getFullName();

        // Case 1 — standard same-date check (updated JPQL handles overnight existing shifts)
        if (!assignmentRepository.findOverlappingAssignments(
                emp.getId(), date, shift.getStartTime(), shift.getEndTime(), null).isEmpty()) {
            throw new BusinessRuleException(
                    "Employee '" + name + "' has an overlapping shift on " + date + ".");
        }

        // Case 2 — proposed shift is overnight (endTime < startTime):
        //   the JPQL formula s.startTime < :endTime breaks because :endTime wraps to the next day.
        //   Explicitly check the window [startTime, midnight) on the same date,
        //   then [midnight, endTime) on the next calendar date.
        if (shift.isOvernight()) {
            if (!assignmentRepository.findOverlappingAssignments(
                    emp.getId(), date, shift.getStartTime(), LocalTime.MAX, null).isEmpty()) {
                throw new BusinessRuleException(
                        "Employee '" + name + "' has an overlapping shift on " + date + ".");
            }
            if (!assignmentRepository.findOverlappingAssignments(
                    emp.getId(), date.plusDays(1), LocalTime.MIDNIGHT, shift.getEndTime(), null).isEmpty()) {
                throw new BusinessRuleException(
                        "Employee '" + name + "' has a conflicting shift early on " + date.plusDays(1) + ".");
            }
        }

        // Case 3 — a previous-day overnight shift whose tail bleeds into the target date.
        //   shiftDate is D-1, but the shift runs past midnight into D, so same-date queries miss it.
        boolean prevDayConflict = shiftRepository.findByEmployeeAndDate(emp.getId(), date.minusDays(1))
                .stream()
                .filter(Shift::isOvernight)
                .anyMatch(prev -> prev.getEndTime().isAfter(shift.getStartTime()));
        if (prevDayConflict) {
            throw new BusinessRuleException(
                    "Employee '" + name + "' has an overnight shift from " + date.minusDays(1)
                    + " that runs into " + date + " and conflicts with this shift.");
        }
    }

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
