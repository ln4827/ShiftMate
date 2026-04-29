package com.shiftmate.service;

import com.shiftmate.dto.CoverageRequirementRequest;
import com.shiftmate.dto.CreateShiftRequest;
import com.shiftmate.dto.ShiftResponse;
import com.shiftmate.dto.UpdateShiftRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * Business operations for shift management. All methods are scoped to a restaurant
 * via {@code restaurantId} (derived from the authenticated user's session), which
 * acts as the cross-tenant guard to prevent data leakage between restaurants.
 */
public interface ShiftService {

    /**
     * Returns a manager's full weekly schedule for the restaurant.
     * All shifts — published and draft — are included.
     *
     * @param restaurantId the restaurant's ID
     * @param weekStart    the Monday of the week to fetch (inclusive)
     * @return shifts for the seven-day window, ordered by date then start time
     * @throws com.shiftmate.exception.ResourceNotFoundException if the restaurant does not exist
     */
    List<ShiftResponse> getWeeklyScheduleForManager(Long restaurantId, LocalDate weekStart);

    /**
     * Returns the published shifts an employee is assigned to for a given week.
     * Draft shifts are excluded — employees only see published schedules.
     *
     * @param employeeId the employee's ID
     * @param weekStart  the Monday of the week to fetch (inclusive)
     * @return published shifts the employee is assigned to, ordered by date then start time
     */
    List<ShiftResponse> getWeeklyScheduleForEmployee(Long employeeId, LocalDate weekStart);

    /**
     * Returns a single shift by ID, scoped to the given restaurant.
     *
     * @param restaurantId the restaurant's ID
     * @param shiftId      the shift's ID
     * @return the shift response with assignments and coverage requirements
     * @throws com.shiftmate.exception.ResourceNotFoundException if the shift does not exist or
     *         does not belong to the given restaurant
     */
    ShiftResponse getShift(Long restaurantId, Long shiftId);

    /**
     * Creates a new shift in draft state ({@code isPublished = false}).
     *
     * @param restaurantId the restaurant's ID
     * @param createdById  the ID of the manager creating the shift
     * @param request      the creation payload
     * @return the newly created shift response
     * @throws com.shiftmate.exception.ResourceNotFoundException if the department does not exist
     *         or does not belong to the restaurant
     */
    ShiftResponse createShift(Long restaurantId, Long createdById, CreateShiftRequest request);

    /**
     * Updates an existing shift's date, time, or department. A published shift
     * can be updated — managers are responsible for re-notifying staff externally.
     *
     * @param restaurantId the restaurant's ID
     * @param shiftId      the shift's ID
     * @param request      the update payload
     * @return the updated shift response
     * @throws com.shiftmate.exception.ResourceNotFoundException if the shift or department is not found
     */
    ShiftResponse updateShift(Long restaurantId, Long shiftId, UpdateShiftRequest request);

    /**
     * Deletes a shift and all its assignments. A published shift with assignments
     * cannot be deleted to protect data integrity; it must be unpublished first.
     *
     * @param restaurantId the restaurant's ID
     * @param shiftId      the shift's ID
     * @throws com.shiftmate.exception.ResourceNotFoundException if the shift does not exist
     * @throws com.shiftmate.exception.BusinessRuleException     if the shift is published and has assignments
     */
    void deleteShift(Long restaurantId, Long shiftId);

    /**
     * Publishes a shift, making it visible to assigned employees. Validates that
     * all coverage requirements are met before publishing.
     *
     * @param restaurantId the restaurant's ID
     * @param shiftId      the shift's ID
     * @return the updated shift response
     * @throws com.shiftmate.exception.ResourceNotFoundException if the shift does not exist
     * @throws com.shiftmate.exception.BusinessRuleException     if any coverage requirement is unmet
     */
    ShiftResponse publishShift(Long restaurantId, Long shiftId);

    /**
     * Unpublishes a shift, hiding it from employees. Does not affect assignments.
     *
     * @param restaurantId the restaurant's ID
     * @param shiftId      the shift's ID
     * @return the updated shift response
     * @throws com.shiftmate.exception.ResourceNotFoundException if the shift does not exist
     * @throws com.shiftmate.exception.BusinessRuleException     if the shift is already unpublished
     */
    ShiftResponse unpublishShift(Long restaurantId, Long shiftId);

    /**
     * Creates or updates a coverage requirement for a specific role on a shift.
     * If a requirement for the role already exists, its {@code minCount} is updated
     * (upsert semantics).
     *
     * @param restaurantId the restaurant's ID
     * @param shiftId      the shift's ID
     * @param request      the requirement payload containing role ID and minimum count
     * @return the updated shift response
     * @throws com.shiftmate.exception.ResourceNotFoundException if the shift or role is not found
     */
    ShiftResponse setCoverageRequirement(Long restaurantId, Long shiftId,
                                         CoverageRequirementRequest request);

    /**
     * Removes a coverage requirement from a shift.
     *
     * @param restaurantId     the restaurant's ID
     * @param shiftId          the shift's ID
     * @param requirementId    the coverage requirement's ID
     * @throws com.shiftmate.exception.ResourceNotFoundException if the requirement does not exist
     *         or does not belong to the given shift
     */
    void deleteCoverageRequirement(Long restaurantId, Long shiftId, Long requirementId);

    /**
     * Returns all published shifts for a restaurant in a given week.
     * Accessible to both managers and employees — used for the swap-target picker
     * and the employee-facing full schedule view.
     */
    List<ShiftResponse> getAllPublishedForWeek(Long restaurantId, LocalDate weekStart);
}
