package com.shiftmate.repository;

import com.shiftmate.entity.SwapRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for {@link SwapRequest} entities.
 */
@Repository
public interface SwapRequestRepository extends JpaRepository<SwapRequest, Long> {

    /**
     * Returns all swap requests with a given status across the entire system.
     *
     * @param status the status to filter by
     * @return list of swap requests, may be empty
     */
    List<SwapRequest> findByStatus(SwapRequest.Status status);

    /**
     * Returns all swap requests where the given employee is either the requester
     * or the target, with both assignments and their shifts eagerly loaded.
     * Used for the employee's personal Swap Request Centre.
     *
     * @param employeeId the employee's ID
     * @return swap requests involving the employee, ordered by request time descending
     */
    @Query("""
            SELECT sr FROM SwapRequest sr
            JOIN FETCH sr.requesterAssignment ra
            JOIN FETCH ra.employee requester
            JOIN FETCH ra.shift rs
            JOIN FETCH sr.targetAssignment ta
            JOIN FETCH ta.employee target
            JOIN FETCH ta.shift ts
            WHERE requester.id = :employeeId OR target.id = :employeeId
            ORDER BY sr.requestedAt DESC
            """)
    List<SwapRequest> findByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * Returns all pending swap requests for a restaurant's approval queue,
     * with full assignment details eagerly loaded for display.
     *
     * @param restaurantId the restaurant's ID
     * @return pending swap requests ordered by request time ascending (oldest first)
     */
    @Query("""
            SELECT sr FROM SwapRequest sr
            JOIN FETCH sr.requesterAssignment ra
            JOIN FETCH ra.employee requester
            JOIN FETCH ra.shift rs
            JOIN FETCH rs.department d
            JOIN FETCH sr.targetAssignment ta
            JOIN FETCH ta.employee target
            WHERE d.restaurant.id = :restaurantId
              AND sr.status = 'PENDING'
            ORDER BY sr.requestedAt ASC
            """)
    List<SwapRequest> findPendingByRestaurantId(@Param("restaurantId") Long restaurantId);

    /**
     * Checks whether a pending swap request already exists between two assignments.
     * Prevents duplicate requests for the same swap.
     *
     * @param requesterAssignmentId the requester's assignment ID
     * @param targetAssignmentId    the target's assignment ID
     * @param status                the status to check against
     * @return {@code true} if a matching request exists
     */
    boolean existsByRequesterAssignmentIdAndTargetAssignmentIdAndStatus(
            Long requesterAssignmentId,
            Long targetAssignmentId,
            SwapRequest.Status status);
}
