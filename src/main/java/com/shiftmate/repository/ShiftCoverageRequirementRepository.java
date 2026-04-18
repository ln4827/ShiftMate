package com.shiftmate.repository;

import com.shiftmate.entity.ShiftCoverageRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for {@link ShiftCoverageRequirement} entities.
 */
@Repository
public interface ShiftCoverageRequirementRepository
        extends JpaRepository<ShiftCoverageRequirement, Long> {

    /**
     * Returns all coverage requirements for a specific shift.
     *
     * @param shiftId the shift's ID
     * @return list of requirements, may be empty
     */
    List<ShiftCoverageRequirement> findByShiftId(Long shiftId);

    /**
     * Finds the coverage requirement for a specific shift and role combination.
     *
     * @param shiftId the shift's ID
     * @param roleId  the role's ID
     * @return an {@link Optional} containing the requirement, or empty if not found
     */
    Optional<ShiftCoverageRequirement> findByShiftIdAndRoleId(Long shiftId, Long roleId);

    /**
     * Removes all coverage requirements for a shift. Called before replacing
     * the full set of requirements in a single operation.
     *
     * @param shiftId the shift's ID
     */
    @Modifying
    @Query("DELETE FROM ShiftCoverageRequirement scr WHERE scr.shift.id = :shiftId")
    void deleteAllByShiftId(@Param("shiftId") Long shiftId);
}
