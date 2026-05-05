package com.shiftmate.repository;

import com.shiftmate.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for {@link Department} entities.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * Returns all departments belonging to a restaurant.
     *
     * @param restaurantId the restaurant's ID
     * @return list of departments, may be empty
     */
    List<Department> findByRestaurantId(Long restaurantId);

    /**
     * Finds a department by restaurant and name (case-sensitive).
     *
     * @param restaurantId the restaurant's ID
     * @param name         the department name to search for
     * @return an {@link Optional} containing the department, or empty if not found
     */
    Optional<Department> findByRestaurantIdAndName(Long restaurantId, String name);

    /**
     * Checks whether a department with the given name already exists in a restaurant.
     *
     * @param restaurantId the restaurant's ID
     * @param name         the department name to check
     * @return {@code true} if the department already exists
     */
    boolean existsByRestaurantIdAndName(Long restaurantId, String name);

    /**
     * Fetches departments with their shifts eagerly loaded in a single query,
     * preventing the N+1 problem when rendering the full schedule overview.
     *
     * @param restaurantId the restaurant's ID
     * @return departments with the {@code shifts} collection populated
     */
    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.shifts WHERE d.restaurant.id = :restaurantId")
    List<Department> findByRestaurantIdWithShifts(@Param("restaurantId") Long restaurantId);

    /**
     * Fetches departments with their allowedRoles eagerly loaded.
     * Used whenever {@link com.shiftmate.dto.DepartmentResponse} is constructed,
     * since that DTO reads the allowedRoles collection.
     *
     * @param restaurantId the restaurant's ID
     * @return departments with the {@code allowedRoles} collection populated
     */
    @Query("SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.allowedRoles WHERE d.restaurant.id = :restaurantId")
    List<Department> findByRestaurantIdWithAllowedRoles(@Param("restaurantId") Long restaurantId);
}
