package com.shiftmate.repository;

import com.shiftmate.entity.ShiftAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for {@link ShiftAssignment} entities.
 */
@Repository
public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

    /**
     * Checks whether an employee is already assigned to a specific shift.
     *
     * @param shiftId    the shift's ID
     * @param employeeId the employee's ID
     * @return {@code true} if the assignment exists
     */
    boolean existsByShiftIdAndEmployeeId(Long shiftId, Long employeeId);

    /**
     * Finds the assignment for a specific employee on a specific shift.
     *
     * @param shiftId    the shift's ID
     * @param employeeId the employee's ID
     * @return an {@link Optional} containing the assignment, or empty if not found
     */
    Optional<ShiftAssignment> findByShiftIdAndEmployeeId(Long shiftId, Long employeeId);

    /**
     * Returns all assignments for a specific shift.
     *
     * @param shiftId the shift's ID
     * @return list of assignments, may be empty
     */
    List<ShiftAssignment> findByShiftId(Long shiftId);

    /**
     * Returns all assignments for a specific employee.
     *
     * @param employeeId the employee's ID
     * @return list of assignments, may be empty
     */
    List<ShiftAssignment> findByEmployeeId(Long employeeId);

    /**
     * Finds existing assignments that time-overlap with a proposed shift window
     * for a given employee on a given date. Used for double-booking prevention.
     * Passing {@code null} for {@code excludeAssignmentId} includes all assignments.
     *
     * @param employeeId          the employee's ID
     * @param date                the date to check
     * @param startTime           the proposed shift start time
     * @param endTime             the proposed shift end time
     * @param excludeAssignmentId assignment ID to exclude from the check (for edit scenarios)
     * @return list of conflicting assignments
     */
    @Query("""
            SELECT sa FROM ShiftAssignment sa
            JOIN FETCH sa.shift s
            WHERE sa.employee.id = :employeeId
              AND s.shiftDate = :date
              AND (
                (s.endTime > s.startTime AND s.startTime < :endTime AND s.endTime > :startTime)
                OR (s.endTime <= s.startTime AND (s.startTime < :endTime OR s.endTime > :startTime))
              )
              AND (:excludeAssignmentId IS NULL OR sa.id <> :excludeAssignmentId)
            """)
    List<ShiftAssignment> findOverlappingAssignments(
            @Param("employeeId") Long employeeId,
            @Param("date") LocalDate date,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime,
            @Param("excludeAssignmentId") Long excludeAssignmentId);

    /**
     * Aggregates total scheduled hours per employee for a restaurant within a
     * date range. Used by the hours report to show weekly or monthly summaries.
     *
     * @param restaurantId the restaurant's ID
     * @param from         the start date (inclusive)
     * @param to           the end date (inclusive)
     * @return list of {@code [employeeId, totalHours]} pairs
     */
    @Query("""
            SELECT sa.employee.id,
                   SUM(FUNCTION('TIMESTAMPDIFF', HOUR,
                       FUNCTION('TIMESTAMP', s.shiftDate, s.startTime),
                       FUNCTION('TIMESTAMP', s.shiftDate, s.endTime)))
            FROM ShiftAssignment sa
            JOIN sa.shift s
            WHERE s.department.restaurant.id = :restaurantId
              AND s.shiftDate BETWEEN :from AND :to
            GROUP BY sa.employee.id
            """)
    List<Object[]> sumHoursPerEmployee(@Param("restaurantId") Long restaurantId,
                                       @Param("from") LocalDate from,
                                       @Param("to") LocalDate to);
}
