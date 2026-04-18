package com.shiftmate.repository;

import com.shiftmate.entity.TimeOffRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Data access layer for {@link TimeOffRequest} entities.
 */
@Repository
public interface TimeOffRequestRepository extends JpaRepository<TimeOffRequest, Long> {

    /**
     * Returns all time-off requests submitted by an employee.
     *
     * @param employeeId the employee's ID
     * @return list of requests, may be empty
     */
    List<TimeOffRequest> findByEmployeeId(Long employeeId);

    /**
     * Returns an employee's time-off requests filtered by status.
     *
     * @param employeeId the employee's ID
     * @param status     the status to filter by
     * @return filtered list of requests
     */
    List<TimeOffRequest> findByEmployeeIdAndStatus(Long employeeId, TimeOffRequest.Status status);

    /**
     * Returns all time-off requests system-wide with a given status.
     *
     * @param status the status to filter by
     * @return list of requests, may be empty
     */
    List<TimeOffRequest> findByStatus(TimeOffRequest.Status status);

    /**
     * Returns all pending time-off requests for a restaurant's approval queue,
     * with employee details eagerly loaded. Ordered oldest-first for fair processing.
     *
     * @param restaurantId the restaurant's ID
     * @return pending requests with employees populated
     */
    @Query("""
            SELECT tor FROM TimeOffRequest tor
            JOIN FETCH tor.employee e
            WHERE e.restaurant.id = :restaurantId
              AND tor.status = 'PENDING'
            ORDER BY tor.requestedAt ASC
            """)
    List<TimeOffRequest> findPendingByRestaurantId(@Param("restaurantId") Long restaurantId);

    /**
     * Finds approved time-off requests that overlap a given date for an employee.
     * Called during shift assignment to warn managers that the employee has
     * approved leave on the proposed shift date.
     *
     * @param employeeId the employee's ID
     * @param date       the proposed shift date
     * @return approved requests whose date range includes the given date
     */
    @Query("""
            SELECT tor FROM TimeOffRequest tor
            WHERE tor.employee.id = :employeeId
              AND tor.status = 'APPROVED'
              AND tor.startDate <= :date
              AND tor.endDate >= :date
            """)
    List<TimeOffRequest> findApprovedOverlapping(@Param("employeeId") Long employeeId,
                                                 @Param("date") LocalDate date);
}
