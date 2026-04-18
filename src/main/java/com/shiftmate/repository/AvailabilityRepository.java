package com.shiftmate.repository;

import com.shiftmate.entity.Availability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for {@link Availability} entities.
 */
@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    /**
     * Returns all availability windows declared by an employee across all days.
     *
     * @param employeeId the employee's ID
     * @return list of availability windows, may be empty
     */
    List<Availability> findByEmployeeId(Long employeeId);

    /**
     * Returns availability windows for an employee on a specific day of the week.
     *
     * @param employeeId the employee's ID
     * @param dayOfWeek  ISO-8601 day number: 1 = Monday, 7 = Sunday
     * @return availability windows for that day, may be empty
     */
    List<Availability> findByEmployeeIdAndDayOfWeek(Long employeeId, int dayOfWeek);

    /**
     * Removes all availability windows for an employee. Called before replacing
     * the employee's full availability schedule in a single operation.
     *
     * @param employeeId the employee's ID
     */
    @Modifying
    @Query("DELETE FROM Availability a WHERE a.employee.id = :employeeId")
    void deleteAllByEmployeeId(@Param("employeeId") Long employeeId);
}
