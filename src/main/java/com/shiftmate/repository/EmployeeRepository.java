package com.shiftmate.repository;

import com.shiftmate.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for {@link Employee} entities. Custom queries use JOIN
 * FETCH
 * to avoid N+1 problems when role data must be available on the returned
 * objects.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

        /**
         * Finds an employee by their unique email address.
         *
         * @param email the employee's email address
         * @return an {@link Optional} containing the employee, or empty if not found
         */
        Optional<Employee> findByEmail(String email);

        /**
         * Checks whether an account with the given email already exists.
         *
         * @param email the email address to check
         * @return {@code true} if an employee with that email exists
         */
        boolean existsByEmail(String email);

        /**
         * Returns all employees belonging to the specified restaurant.
         *
         * @param restaurantId the restaurant's ID
         * @return list of employees, may be empty
         */
        List<Employee> findByRestaurantId(Long restaurantId);

        /**
         * Returns employees for a restaurant filtered by active status.
         *
         * @param restaurantId the restaurant's ID
         * @param isActive     {@code true} for active employees, {@code false} for
         *                     inactive
         * @return filtered list of employees
         */
        List<Employee> findByRestaurantIdAndIsActive(Long restaurantId, boolean isActive);

        /**
         * Returns employees for a restaurant filtered by manager status.
         *
         * @param restaurantId the restaurant's ID
         * @param isManager    {@code true} to return only managers
         * @return filtered list of employees
         */
        List<Employee> findByRestaurantIdAndIsManager(Long restaurantId, boolean isManager);

        /**
         * Fetches a single employee with their role assignments eagerly loaded in one
         * query, preventing the N+1 problem when role data is required immediately.
         *
         * @param id the employee's ID
         * @return an {@link Optional} containing the employee with roles, or empty
         */
        @Query("""
                        SELECT e FROM Employee e
                        LEFT JOIN FETCH e.employeeRoles er
                        LEFT JOIN FETCH er.role
                        WHERE e.id = :id
                        """)
        Optional<Employee> findByIdWithRoles(@Param("id") Long id);

        /**
         * Fetches all active employees for a restaurant with their roles eagerly loaded
         * in a single query. Used by the schedule builder to populate assignment
         * dropdowns.
         *
         * @param restaurantId the restaurant's ID
         * @return active employees ordered by last name then first name, with roles
         *         populated
         */
        @Query("""
                        SELECT DISTINCT e FROM Employee e
                        LEFT JOIN FETCH e.employeeRoles er
                        LEFT JOIN FETCH er.role
                        WHERE e.restaurant.id = :restaurantId
                          AND e.isActive = true
                        ORDER BY e.lastName, e.firstName
                        """)
        List<Employee> findActiveByRestaurantIdWithRoles(@Param("restaurantId") Long restaurantId);

        /**
         * Fetches ALL employees (active and inactive) for a restaurant with roles.
         */
        @Query("""
                        SELECT DISTINCT e FROM Employee e
                        LEFT JOIN FETCH e.employeeRoles er
                        LEFT JOIN FETCH er.role
                        WHERE e.restaurant.id = :restaurantId
                        ORDER BY e.isActive DESC, e.lastName ASC, e.firstName ASC
                        """)
        List<Employee> findAllByRestaurantIdWithRoles(@Param("restaurantId") Long restaurantId);
}
