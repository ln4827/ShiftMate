package com.shiftmate.service.impl;

import com.shiftmate.dto.AssignmentResponse;
import com.shiftmate.dto.CreateAssignmentRequest;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

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
    private final AvailabilityRepository availabilityRepository;

    @Override
    @Transactional
    public ShiftResponse assign(Long restaurantId, Long shiftId, CreateAssignmentRequest request) {
        Shift shift  = resolveShift(restaurantId, shiftId);
        Employee emp = resolveActiveEmployee(restaurantId, request.getEmployeeId());
        Role role    = resolveRole(restaurantId, request.getRoleId());

        if (!employeeRoleRepository.existsByEmployeeIdAndRoleId(emp.getId(), role.getId())) {
            throw new BusinessRuleException(
                    "Employee '" + emp.getFullName() + "' does not have role '" + role.getName() + "'.");
        }

        if (assignmentRepository.existsByShiftIdAndEmployeeId(shiftId, emp.getId())) {
            throw new BusinessRuleException(
                    "Employee '" + emp.getFullName() + "' is already assigned to this shift.");
        }

        checkForConflicts(emp, shift);
        checkAvailability(emp, shift);

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

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> listAssignments(Long restaurantId, Long shiftId) {
        resolveShift(restaurantId, shiftId);
        return assignmentRepository.findByShiftIdWithDetails(shiftId, restaurantId)
                .stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Checks that assigning {@code emp} to {@code shift} creates no time-overlap
     * with an existing assignment. Handles three distinct cases:
     * <ol>
     *   <li>Same-date overlaps including overnight existing shifts.</li>
     *   <li>Overnight proposed shifts — pre-midnight and post-midnight windows
     *       are checked separately because the JPQL formula breaks when
     *       {@code endTime < startTime}.</li>
     *   <li>A previous-day overnight shift whose tail bleeds into the target date.</li>
     * </ol>
     */
    private void checkForConflicts(Employee emp, Shift shift) {
        LocalDate date = shift.getShiftDate();
        String name = emp.getFullName();

        if (!assignmentRepository.findOverlappingAssignments(
                emp.getId(), date, shift.getStartTime(), shift.getEndTime(), null).isEmpty()) {
            throw new BusinessRuleException(
                    "Employee '" + name + "' has an overlapping shift on " + date + ".");
        }

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

    /**
     * Checks that the shift falls within the employee's stated availability for
     * that day of the week. If the employee has not set any availability at all,
     * no constraint is applied. If availability is set but the shift day or time
     * is outside a declared window, a {@link BusinessRuleException} is thrown.
     */
    private void checkAvailability(Employee emp, Shift shift) {
        List<Availability> allWindows = availabilityRepository.findByEmployeeId(emp.getId());
        if (allWindows.isEmpty()) return;

        int dayOfWeek = shift.getShiftDate().getDayOfWeek().getValue();
        String dayName = shift.getShiftDate().getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        List<Availability> dayWindows = allWindows.stream()
                .filter(a -> a.getDayOfWeek() == dayOfWeek)
                .toList();

        if (dayWindows.isEmpty()) {
            throw new BusinessRuleException(
                    "Employee '" + emp.getFullName() + "' is not available on " + dayName + "s.");
        }

        boolean fits = dayWindows.stream().anyMatch(w ->
                !shift.getStartTime().isBefore(w.getStartTime()) &&
                shift.getStartTime().isBefore(w.getEndTime()));

        if (!fits) {
            Availability w = dayWindows.get(0);
            throw new BusinessRuleException(
                    "Employee '" + emp.getFullName() + "' is only available " +
                    w.getStartTime() + "–" + w.getEndTime() + " on " + dayName + "s.");
        }
    }
}
