package com.shiftmate.repository;

import com.shiftmate.entity.SwapRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SwapRequestRepository extends JpaRepository<SwapRequest, Long> {

    List<SwapRequest> findByStatus(SwapRequest.Status status);

    /**
     * Loads a single swap request with all associations needed to build
     * {@link com.shiftmate.dto.SwapRequestResponse} without additional queries.
     */
    @Query("""
            SELECT sr FROM SwapRequest sr
            JOIN FETCH sr.requesterAssignment ra
            JOIN FETCH ra.employee requester
            JOIN FETCH ra.shift rs
            JOIN FETCH rs.department rsd
            JOIN FETCH ra.role rr
            JOIN FETCH sr.targetAssignment ta
            JOIN FETCH ta.employee target
            JOIN FETCH ta.shift ts
            JOIN FETCH ts.department tsd
            JOIN FETCH ta.role tr
            LEFT JOIN FETCH sr.resolvedBy
            WHERE sr.id = :id
            """)
    Optional<SwapRequest> findByIdWithDetails(@Param("id") Long id);

    /**
     * Returns all swap requests where the given employee is either the requester
     * or the target, with all associations eagerly loaded.
     */
    @Query("""
            SELECT sr FROM SwapRequest sr
            JOIN FETCH sr.requesterAssignment ra
            JOIN FETCH ra.employee requester
            JOIN FETCH ra.shift rs
            JOIN FETCH rs.department rsd
            JOIN FETCH ra.role rr
            JOIN FETCH sr.targetAssignment ta
            JOIN FETCH ta.employee target
            JOIN FETCH ta.shift ts
            JOIN FETCH ts.department tsd
            JOIN FETCH ta.role tr
            LEFT JOIN FETCH sr.resolvedBy
            WHERE requester.id = :employeeId OR target.id = :employeeId
            ORDER BY sr.requestedAt DESC
            """)
    List<SwapRequest> findByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * Returns all pending swap requests for a restaurant's approval queue,
     * with full association graph eagerly loaded for display.
     */
    @Query("""
            SELECT sr FROM SwapRequest sr
            JOIN FETCH sr.requesterAssignment ra
            JOIN FETCH ra.employee requester
            JOIN FETCH ra.shift rs
            JOIN FETCH rs.department d
            JOIN FETCH ra.role rr
            JOIN FETCH sr.targetAssignment ta
            JOIN FETCH ta.employee target
            JOIN FETCH ta.shift ts
            JOIN FETCH ts.department tsd
            JOIN FETCH ta.role tr
            WHERE d.restaurant.id = :restaurantId
              AND sr.status = 'PENDING'
            ORDER BY sr.requestedAt ASC
            """)
    List<SwapRequest> findPendingByRestaurantId(@Param("restaurantId") Long restaurantId);

    boolean existsByRequesterAssignmentIdAndTargetAssignmentIdAndStatus(
            Long requesterAssignmentId,
            Long targetAssignmentId,
            SwapRequest.Status status);

    @Query("""
            SELECT sr FROM SwapRequest sr
            WHERE sr.requesterAssignment.shift.id = :shiftId
               OR sr.targetAssignment.shift.id = :shiftId
            """)
    List<SwapRequest> findByShiftId(@Param("shiftId") Long shiftId);
}
