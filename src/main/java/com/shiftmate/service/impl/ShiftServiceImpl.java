package com.shiftmate.service.impl;

import com.shiftmate.dto.CoverageRequirementRequest;
import com.shiftmate.dto.CreateShiftRequest;
import com.shiftmate.dto.ShiftResponse;
import com.shiftmate.dto.UpdateShiftRequest;
import com.shiftmate.entity.*;
import com.shiftmate.entity.Notification;
import com.shiftmate.exception.BusinessRuleException;
import com.shiftmate.exception.ResourceNotFoundException;
import com.shiftmate.repository.*;
import com.shiftmate.service.NotificationService;
import com.shiftmate.service.ShiftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Default implementation of {@link ShiftService}.
 *
 * <p>Read operations use {@code @Transactional(readOnly = true)} to skip
 * Hibernate dirty-checking on SELECT-only paths. Write operations use a
 * full read-write transaction.
 *
 * <p>Tenant isolation is enforced by always filtering on the restaurant ID
 * derived from the authenticated user's session principal. The department
 * restaurant check in {@link #resolveShift} ensures a manager cannot reach
 * shifts outside their own restaurant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShiftServiceImpl implements ShiftService {

    private final ShiftRepository shiftRepository;
    private final ShiftCoverageRequirementRepository coverageRequirementRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final NotificationService notificationService;

    // -------------------------------------------------------------------------
    // Weekly schedule queries
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<ShiftResponse> getWeeklyScheduleForManager(Long restaurantId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return shiftRepository.findWeeklySchedule(restaurantId, weekStart, weekEnd)
                .stream()
                .map(ShiftResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftResponse> getWeeklyScheduleForEmployee(Long employeeId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return shiftRepository.findPublishedShiftsForEmployee(employeeId, weekStart, weekEnd)
                .stream()
                .map(ShiftResponse::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Single shift fetch
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ShiftResponse getShift(Long restaurantId, Long shiftId) {
        Shift shift = resolveShiftWithCoverage(restaurantId, shiftId);
        return ShiftResponse.from(shift);
    }

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ShiftResponse createShift(Long restaurantId, Long createdById,
                                     CreateShiftRequest request) {
        Department department = resolveDepartment(restaurantId, request.getDepartmentId());
        Employee createdBy = resolveEmployee(restaurantId, createdById);

        Shift shift = Shift.builder()
                .department(department)
                .shiftDate(request.getShiftDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isPublished(false)
                .createdBy(createdBy)
                .build();

        shift = shiftRepository.save(shift);
        log.info("Created shift id={} date={} dept={} by manager id={}",
                shift.getId(), shift.getShiftDate(),
                department.getName(), createdById);

        // Re-fetch with full graph so the response is complete
        return ShiftResponse.from(resolveShiftWithCoverage(restaurantId, shift.getId()));
    }

    @Override
    @Transactional
    public ShiftResponse updateShift(Long restaurantId, Long shiftId,
                                     UpdateShiftRequest request) {
        Shift shift = resolveShift(restaurantId, shiftId);
        Department department = resolveDepartment(restaurantId, request.getDepartmentId());

        shift.setDepartment(department);
        shift.setShiftDate(request.getShiftDate());
        shift.setStartTime(request.getStartTime());
        shift.setEndTime(request.getEndTime());

        shiftRepository.save(shift);
        log.info("Updated shift id={}", shiftId);

        return ShiftResponse.from(resolveShiftWithCoverage(restaurantId, shiftId));
    }

    @Override
    @Transactional
    public void deleteShift(Long restaurantId, Long shiftId) {
        Shift shift = resolveShiftWithCoverage(restaurantId, shiftId);

        if (shift.isPublished() && !shift.getAssignments().isEmpty()) {
            throw new BusinessRuleException(
                    "Cannot delete a published shift that has employees assigned. " +
                    "Unpublish the shift first.");
        }

        shiftRepository.delete(shift);
        log.info("Deleted shift id={}", shiftId);
    }

    // -------------------------------------------------------------------------
    // Publish / unpublish
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ShiftResponse publishShift(Long restaurantId, Long shiftId) {
        // Use the coverage-aware fetch so we can validate requirements in the same tx
        Shift shift = resolveShiftWithCoverage(restaurantId, shiftId);

        if (shift.isPublished()) {
            throw new BusinessRuleException("Shift is already published.");
        }

        validateCoverage(shift);

        shift.setPublished(true);
        shiftRepository.save(shift);
        log.info("Published shift id={}", shiftId);

        shift.getAssignments().forEach(a ->
                notificationService.send(a.getEmployee().getId(),
                        Notification.Type.SCHEDULE_PUBLISHED,
                        "Your shift on " + shift.getShiftDate() + " ("
                        + shift.getDepartment().getName() + ", "
                        + shift.getStartTime() + "–" + shift.getEndTime()
                        + ") has been published."));

        return ShiftResponse.from(resolveShiftWithCoverage(restaurantId, shiftId));
    }

    @Override
    @Transactional
    public ShiftResponse unpublishShift(Long restaurantId, Long shiftId) {
        Shift shift = resolveShift(restaurantId, shiftId);

        if (!shift.isPublished()) {
            throw new BusinessRuleException("Shift is already unpublished.");
        }

        shift.setPublished(false);
        shiftRepository.save(shift);
        log.info("Unpublished shift id={}", shiftId);

        return ShiftResponse.from(resolveShiftWithCoverage(restaurantId, shiftId));
    }

    // -------------------------------------------------------------------------
    // Coverage requirements
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ShiftResponse setCoverageRequirement(Long restaurantId, Long shiftId,
                                                CoverageRequirementRequest request) {
        Shift shift = resolveShift(restaurantId, shiftId);
        Role role = resolveRole(restaurantId, request.getRoleId());

        // Upsert: update existing requirement or create a new one
        ShiftCoverageRequirement requirement =
                coverageRequirementRepository.findByShiftIdAndRoleId(shiftId, role.getId())
                        .orElseGet(() -> {
                            ShiftCoverageRequirement r = ShiftCoverageRequirement.builder()
                                    .shift(shift)
                                    .role(role)
                                    .minCount(request.getMinCount())
                                    .build();
                            return r;
                        });

        requirement.setMinCount(request.getMinCount());
        coverageRequirementRepository.save(requirement);

        log.info("Set coverage requirement: shift id={} role='{}' minCount={}",
                shiftId, role.getName(), request.getMinCount());

        return ShiftResponse.from(resolveShiftWithCoverage(restaurantId, shiftId));
    }

    @Override
    @Transactional
    public void deleteCoverageRequirement(Long restaurantId, Long shiftId, Long requirementId) {
        // Validate the shift belongs to the restaurant
        resolveShift(restaurantId, shiftId);

        ShiftCoverageRequirement requirement = coverageRequirementRepository.findById(requirementId)
                .filter(r -> r.getShift().getId().equals(shiftId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Coverage requirement with id " + requirementId +
                        " not found on shift " + shiftId + "."));

        coverageRequirementRepository.delete(requirement);
        log.info("Deleted coverage requirement id={} from shift id={}", requirementId, shiftId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftResponse> getAllPublishedForWeek(Long restaurantId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return shiftRepository.findPublishedWeeklySchedule(restaurantId, weekStart, weekEnd)
                .stream()
                .map(ShiftResponse::from)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers — mirrors EmployeeServiceImpl resolve pattern
    // -------------------------------------------------------------------------

    /**
     * Resolves a shift and validates it belongs to the given restaurant via its
     * department. Loads only the shift itself — not the coverage/assignment graph.
     * Use {@link #resolveShiftWithCoverage} when those collections are needed.
     */
    private Shift resolveShift(Long restaurantId, Long shiftId) {
        return shiftRepository.findById(shiftId)
                .filter(s -> s.getDepartment().getRestaurant().getId().equals(restaurantId))
                .orElseThrow(() -> new ResourceNotFoundException("Shift", shiftId));
    }

    /**
     * Resolves a shift with its coverage requirements and assignments fully loaded
     * in a single query. Use this when the response DTO or coverage validation
     * needs to access those collections.
     */
    private Shift resolveShiftWithCoverage(Long restaurantId, Long shiftId) {
        return shiftRepository.findByIdWithCoverageAndAssignments(shiftId)
                .filter(s -> s.getDepartment().getRestaurant().getId().equals(restaurantId))
                .orElseThrow(() -> new ResourceNotFoundException("Shift", shiftId));
    }

    /**
     * Resolves a department and validates it belongs to the given restaurant.
     */
    private Department resolveDepartment(Long restaurantId, Long departmentId) {
        return departmentRepository.findById(departmentId)
                .filter(d -> d.getRestaurant().getId().equals(restaurantId))
                .orElseThrow(() -> new ResourceNotFoundException("Department", departmentId));
    }

    /**
     * Resolves an employee and validates they belong to the given restaurant.
     */
    private Employee resolveEmployee(Long restaurantId, Long employeeId) {
        return employeeRepository.findById(employeeId)
                .filter(e -> e.getRestaurant().getId().equals(restaurantId))
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
    }

    /**
     * Resolves a role and validates it belongs to the given restaurant.
     */
    private Role resolveRole(Long restaurantId, Long roleId) {
        return roleRepository.findById(roleId)
                .filter(r -> r.getRestaurant().getId().equals(restaurantId))
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
    }

    /**
     * Validates that all coverage requirements for a shift are met by the current
     * assignments. Throws {@link BusinessRuleException} listing the unmet roles.
     * Called inside the publish transaction so the check is atomic.
     */
    private void validateCoverage(Shift shift) {
        if (shift.getCoverageRequirements().isEmpty()) {
            return;
        }

        // Count current assignments per role
        java.util.Map<Long, Long> countByRole = shift.getAssignments().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        a -> a.getRole().getId(),
                        java.util.stream.Collectors.counting()
                ));

        List<String> unmet = shift.getCoverageRequirements().stream()
                .filter(req -> countByRole.getOrDefault(req.getRole().getId(), 0L) < req.getMinCount())
                .map(req -> String.format("'%s' (needs %d, has %d)",
                        req.getRole().getName(),
                        req.getMinCount(),
                        countByRole.getOrDefault(req.getRole().getId(), 0L).intValue()))
                .toList();

        if (!unmet.isEmpty()) {
            throw new BusinessRuleException(
                    "Cannot publish: coverage requirements not met for " +
                    String.join(", ", unmet) + ".");
        }
    }
}
