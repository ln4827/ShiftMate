package com.shiftmate.repository;

import com.shiftmate.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for {@link Shift} entities. Custom queries use JOIN FETCH
 * to load the full object graph required for schedule rendering in a single
 * round trip, avoiding N+1 problems.
 */
@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    /**
     * Fetches a complete weekly schedule for a restaurant within a date range.
     * Assignments, their employees, and roles are all loaded eagerly to
     * support full schedule grid rendering without additional queries.
     *
     * @param restaurantId the restaurant's ID
     * @param from         the start date (inclusive)
     * @param to           the end date (inclusive)
     * @return shifts ordered by date then start time, with assignments populated
     */
    @Query("""
            SELECT DISTINCT s FROM Shift s
            JOIN FETCH s.department d
            LEFT JOIN FETCH s.assignments sa
            LEFT JOIN FETCH sa.employee e
            LEFT JOIN FETCH sa.role r
            WHERE d.restaurant.id = :restaurantId
              AND s.shiftDate BETWEEN :from AND :to
            ORDER BY s.shiftDate, s.startTime
            """)
    List<Shift> findWeeklySchedule(@Param("restaurantId") Long restaurantId,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to);

    /**
     * Fetches only published shifts that an employee is assigned to within a
     * date range. Used to render the employee's personal weekly view.
     *
     * @param employeeId the employee's ID
     * @param from       the start date (inclusive)
     * @param to         the end date (inclusive)
     * @return published shifts the employee is assigned to, ordered by date then start time
     */
    @Query("""
            SELECT DISTINCT s FROM Shift s
            JOIN FETCH s.department
            JOIN s.assignments sa
            WHERE sa.employee.id = :employeeId
              AND s.shiftDate BETWEEN :from AND :to
              AND s.isPublished = true
            ORDER BY s.shiftDate, s.startTime
            """)
    List<Shift> findPublishedShiftsForEmployee(@Param("employeeId") Long employeeId,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    /**
     * Finds all shifts an employee is already assigned to on a given date,
     * regardless of published status. Used as the first pass in overlap detection.
     *
     * @param employeeId the employee's ID
     * @param date       the date to check
     * @return shifts the employee is assigned to on that date
     */
    @Query("""
            SELECT s FROM Shift s
            JOIN s.assignments sa
            WHERE sa.employee.id = :employeeId
              AND s.shiftDate = :date
            """)
    List<Shift> findByEmployeeAndDate(@Param("employeeId") Long employeeId,
                                      @Param("date") LocalDate date);

    /**
     * Returns all shifts for a department within a date range.
     *
     * @param departmentId the department's ID
     * @param from         the start date (inclusive)
     * @param to           the end date (inclusive)
     * @return list of shifts, may be empty
     */
    List<Shift> findByDepartmentIdAndShiftDateBetween(Long departmentId,
                                                      LocalDate from, LocalDate to);

    /**
     * Fetches a shift with its coverage requirements and current assignments
     * eagerly loaded. Used during shift publication to validate staffing levels.
     *
     * @param id the shift's ID
     * @return an {@link Optional} containing the shift with requirements and assignments, or empty
     */
    @Query("""
            SELECT s FROM Shift s
            LEFT JOIN FETCH s.coverageRequirements cr
            LEFT JOIN FETCH cr.role
            LEFT JOIN FETCH s.assignments sa
            LEFT JOIN FETCH sa.role
            WHERE s.id = :id
            """)
    Optional<Shift> findByIdWithCoverageAndAssignments(@Param("id") Long id);
}
