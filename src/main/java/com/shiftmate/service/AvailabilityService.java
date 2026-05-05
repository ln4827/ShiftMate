package com.shiftmate.service;

import com.shiftmate.dto.AvailabilityResponse;
import com.shiftmate.dto.SetAvailabilityRequest;

import java.util.List;

/**
 * Business operations for employee availability windows. All write operations
 * are scoped to a restaurant via {@code restaurantId} to enforce tenant isolation
 * and to verify the target employee belongs to the caller's restaurant.
 */
public interface AvailabilityService {

    /**
     * Returns all weekly availability windows for an employee.
     *
     * @param restaurantId the restaurant's ID (used to verify employee ownership)
     * @param employeeId   the employee's ID
     * @return list of availability windows ordered by day then start time, may be empty
     * @throws com.shiftmate.exception.ResourceNotFoundException if the employee does not exist
     *         or does not belong to the given restaurant
     */
    List<AvailabilityResponse> getAvailability(Long restaurantId, Long employeeId);

    /**
     * Replaces an employee's full availability schedule in one atomic operation.
     * All existing windows are deleted and the supplied windows are persisted.
     * Sending an empty {@code windows} list clears all availability.
     *
     * @param restaurantId the restaurant's ID (used to verify employee ownership)
     * @param employeeId   the employee's ID
     * @param request      the new availability windows
     * @return the saved availability windows
     * @throws com.shiftmate.exception.ResourceNotFoundException if the employee does not exist
     *         or does not belong to the given restaurant
     */
    List<AvailabilityResponse> setAvailability(Long restaurantId, Long employeeId,
                                               SetAvailabilityRequest request);
}
