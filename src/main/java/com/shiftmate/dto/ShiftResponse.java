package com.shiftmate.dto;

import com.shiftmate.entity.Shift;
import com.shiftmate.entity.ShiftAssignment;
import com.shiftmate.entity.ShiftCoverageRequirement;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read-only response object representing a shift with its assignments and coverage
 * requirements. Constructed via the static factory {@link #from(Shift)}.
 *
 * <p>The {@code coverageMet} convenience flag lets the frontend instantly show
 * a warning badge without re-computing coverage on the client side.
 */
@Getter
public class ShiftResponse {

    private final Long id;
    private final LocalDate shiftDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final boolean published;
    private final boolean overnight;
    private final LocalDateTime createdAt;

    // Department
    private final Long departmentId;
    private final String departmentName;

    // Creator
    private final Long createdById;
    private final String createdByName;

    private final List<AssignmentDto> assignments;
    private final List<CoverageDto> coverageRequirements;

    /** {@code true} when every coverage requirement for this shift is satisfied. */
    private final boolean coverageMet;

    private ShiftResponse(Shift shift) {
        this.id          = shift.getId();
        this.shiftDate   = shift.getShiftDate();
        this.startTime   = shift.getStartTime();
        this.endTime     = shift.getEndTime();
        this.published   = shift.isPublished();
        this.overnight   = shift.isOvernight();
        this.createdAt   = shift.getCreatedAt();

        this.departmentId   = shift.getDepartment().getId();
        this.departmentName = shift.getDepartment().getName();

        this.createdById   = shift.getCreatedBy().getId();
        this.createdByName = shift.getCreatedBy().getFullName();

        this.assignments = shift.getAssignments().stream()
                .map(AssignmentDto::new)
                .collect(Collectors.toList());

        // Build role -> current assigned count map for coverage calculations
        Map<Long, Long> countByRole = shift.getAssignments().stream()
                .collect(Collectors.groupingBy(
                        a -> a.getRole().getId(),
                        Collectors.counting()
                ));

        this.coverageRequirements = shift.getCoverageRequirements().stream()
                .map(req -> new CoverageDto(req, countByRole))
                .collect(Collectors.toList());

        this.coverageMet = this.coverageRequirements.stream()
                .allMatch(CoverageDto::isMet);
    }

    /**
     * Creates a {@code ShiftResponse} from a fully loaded {@link Shift} entity.
     * The {@code assignments} and {@code coverageRequirements} collections must be
     * initialised before calling this method.
     *
     * @param shift the source entity
     * @return the corresponding response object
     */
    public static ShiftResponse from(Shift shift) {
        return new ShiftResponse(shift);
    }

    // -------------------------------------------------------------------------
    // Nested DTOs
    // -------------------------------------------------------------------------

    /**
     * Lightweight projection of a {@link ShiftAssignment} for embedding inside
     * a {@code ShiftResponse}.
     */
    @Getter
    public static class AssignmentDto {

        private final Long id;
        private final Long employeeId;
        private final String employeeName;
        private final Long roleId;
        private final String roleName;
        private final LocalDateTime assignedAt;

        private AssignmentDto(ShiftAssignment sa) {
            this.id           = sa.getId();
            this.employeeId   = sa.getEmployee().getId();
            this.employeeName = sa.getEmployee().getFullName();
            this.roleId       = sa.getRole().getId();
            this.roleName     = sa.getRole().getName();
            this.assignedAt   = sa.getAssignedAt();
        }
    }

    /**
     * Lightweight projection of a {@link ShiftCoverageRequirement} enriched with
     * the current assignment count for that role so the frontend can display
     * coverage status without additional requests.
     */
    @Getter
    public static class CoverageDto {

        private final Long id;
        private final Long roleId;
        private final String roleName;
        private final int minCount;
        private final int currentCount;
        private final boolean met;

        private CoverageDto(ShiftCoverageRequirement req, Map<Long, Long> countByRole) {
            this.id           = req.getId();
            this.roleId       = req.getRole().getId();
            this.roleName     = req.getRole().getName();
            this.minCount     = req.getMinCount();
            this.currentCount = countByRole.getOrDefault(req.getRole().getId(), 0L).intValue();
            this.met          = this.currentCount >= this.minCount;
        }
    }
}
