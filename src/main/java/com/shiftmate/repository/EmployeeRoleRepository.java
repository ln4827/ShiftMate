package com.shiftmate.repository;

import com.shiftmate.entity.EmployeeRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for {@link EmployeeRole} junction entities. Provides
 * bulk-delete operations used when replacing an employee's full role set
 * or removing a single role assignment.
 */
@Repository
public interface EmployeeRoleRepository extends JpaRepository<EmployeeRole, EmployeeRole.EmployeeRoleId> {

    /**
     * Returns all role assignments for a given employee.
     *
     * @param employeeId the employee's ID
     * @return list of role assignments, may be empty
     */
    List<EmployeeRole> findByEmployeeId(Long employeeId);

    /**
     * Returns all employee assignments for a given role.
     *
     * @param roleId the role's ID
     * @return list of role assignments, may be empty
     */
    List<EmployeeRole> findByRoleId(Long roleId);

    /**
     * Checks whether an employee already holds a specific role.
     *
     * @param employeeId the employee's ID
     * @param roleId     the role's ID
     * @return {@code true} if the assignment exists
     */
    boolean existsByEmployeeIdAndRoleId(Long employeeId, Long roleId);

    /**
     * Removes a specific role assignment from an employee.
     *
     * @param employeeId the employee's ID
     * @param roleId     the role's ID
     */
    @Modifying
    @Query("DELETE FROM EmployeeRole er WHERE er.employee.id = :employeeId AND er.role.id = :roleId")
    void deleteByEmployeeIdAndRoleId(@Param("employeeId") Long employeeId,
                                     @Param("roleId") Long roleId);

    /**
     * Removes all role assignments for an employee. Called before replacing the
     * full set of roles in a single operation.
     *
     * @param employeeId the employee's ID
     */
    @Modifying
    @Query("DELETE FROM EmployeeRole er WHERE er.employee.id = :employeeId")
    void deleteAllByEmployeeId(@Param("employeeId") Long employeeId);
}
