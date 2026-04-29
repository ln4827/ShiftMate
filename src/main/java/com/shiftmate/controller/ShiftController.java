package com.shiftmate.controller;

import com.shiftmate.dto.CoverageRequirementRequest;
import com.shiftmate.dto.CreateShiftRequest;
import com.shiftmate.dto.ShiftResponse;
import com.shiftmate.dto.UpdateShiftRequest;
import com.shiftmate.security.ShiftMateUserDetails;
import com.shiftmate.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for shift management operations.
 *
 * <p>All endpoints are scoped to the authenticated user's restaurant via
 * {@link ShiftMateUserDetails#getRestaurantId()}, ensuring tenants cannot
 * access each other's schedules. Manager-only operations are enforced via
 * {@code @PreAuthorize("hasRole('MANAGER')")}.
 *
 * <p>The {@code weekStart} query parameter accepts any date; if it is not a
 * Monday the service normalises it to the Monday of that week so the frontend
 * does not have to compute week boundaries itself.
 */
@Slf4j
@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    // -------------------------------------------------------------------------
    // Weekly schedule
    // -------------------------------------------------------------------------

    /**
     * Returns the full weekly schedule for the authenticated manager's restaurant.
     * All shifts — published and draft — are included.
     *
     * @param principal the authenticated manager
     * @param weekStart any date within the target week (defaults to today)
     * @return shifts for the seven-day window ordered by date then start time
     */
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public List<ShiftResponse> getManagerSchedule(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {

        LocalDate monday = toMonday(weekStart != null ? weekStart : LocalDate.now());
        return shiftService.getWeeklyScheduleForManager(principal.getRestaurantId(), monday);
    }

    /**
     * Returns the published weekly schedule for the authenticated employee.
     * Only shifts the employee is assigned to are returned.
     *
     * @param principal the authenticated employee
     * @param weekStart any date within the target week (defaults to today)
     * @return published shifts the employee is assigned to
     */
    @GetMapping("/all-published")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public List<ShiftResponse> getAllPublished(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {

        LocalDate monday = toMonday(weekStart != null ? weekStart : LocalDate.now());
        return shiftService.getAllPublishedForWeek(principal.getRestaurantId(), monday);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public List<ShiftResponse> getMySchedule(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {

        LocalDate monday = toMonday(weekStart != null ? weekStart : LocalDate.now());
        return shiftService.getWeeklyScheduleForEmployee(principal.getEmployeeId(), monday);
    }

    // -------------------------------------------------------------------------
    // Single shift
    // -------------------------------------------------------------------------

    /**
     * Returns a single shift by ID.
     *
     * @param principal the authenticated user
     * @param id        the shift's ID
     * @return the shift with assignments and coverage requirements
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ShiftResponse getShift(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id) {

        return shiftService.getShift(principal.getRestaurantId(), id);
    }

    // -------------------------------------------------------------------------
    // CRUD — manager only
    // -------------------------------------------------------------------------

    /**
     * Creates a new shift in draft state.
     *
     * @param principal the authenticated manager (becomes the shift creator)
     * @param request   the shift creation payload
     * @return the created shift with HTTP 201
     */
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftResponse createShift(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @Valid @RequestBody CreateShiftRequest request) {

        return shiftService.createShift(
                principal.getRestaurantId(), principal.getEmployeeId(), request);
    }

    /**
     * Updates an existing shift's date, time, or department.
     *
     * @param principal the authenticated manager
     * @param id        the shift's ID
     * @param request   the update payload
     * @return the updated shift
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ShiftResponse updateShift(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateShiftRequest request) {

        return shiftService.updateShift(principal.getRestaurantId(), id, request);
    }

    /**
     * Deletes a shift. A published shift with assignments cannot be deleted.
     *
     * @param principal the authenticated manager
     * @param id        the shift's ID
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShift(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id) {

        shiftService.deleteShift(principal.getRestaurantId(), id);
    }

    // -------------------------------------------------------------------------
    // Publish / unpublish
    // -------------------------------------------------------------------------

    /**
     * Publishes a shift, making it visible to assigned employees.
     * Validates all coverage requirements before publishing.
     *
     * @param principal the authenticated manager
     * @param id        the shift's ID
     * @return the updated shift
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('MANAGER')")
    public ShiftResponse publishShift(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id) {

        return shiftService.publishShift(principal.getRestaurantId(), id);
    }

    /**
     * Unpublishes a shift, hiding it from employees.
     *
     * @param principal the authenticated manager
     * @param id        the shift's ID
     * @return the updated shift
     */
    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('MANAGER')")
    public ShiftResponse unpublishShift(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id) {

        return shiftService.unpublishShift(principal.getRestaurantId(), id);
    }

    // -------------------------------------------------------------------------
    // Coverage requirements
    // -------------------------------------------------------------------------

    /**
     * Creates or updates a coverage requirement for a role on a shift (upsert).
     *
     * @param principal the authenticated manager
     * @param id        the shift's ID
     * @param request   the requirement payload
     * @return the updated shift
     */
    @PostMapping("/{id}/coverage")
    @PreAuthorize("hasRole('MANAGER')")
    public ShiftResponse setCoverage(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody CoverageRequirementRequest request) {

        return shiftService.setCoverageRequirement(principal.getRestaurantId(), id, request);
    }

    /**
     * Removes a coverage requirement from a shift.
     *
     * @param principal     the authenticated manager
     * @param id            the shift's ID
     * @param requirementId the coverage requirement's ID
     */
    @DeleteMapping("/{id}/coverage/{requirementId}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCoverage(
            @AuthenticationPrincipal ShiftMateUserDetails principal,
            @PathVariable Long id,
            @PathVariable Long requirementId) {

        shiftService.deleteCoverageRequirement(principal.getRestaurantId(), id, requirementId);
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /**
     * Normalises any date to the Monday of its ISO week, so the frontend does
     * not have to compute week boundaries before calling these endpoints.
     *
     * @param date any date
     * @return the Monday of the same ISO week
     */
    private LocalDate toMonday(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }
}
