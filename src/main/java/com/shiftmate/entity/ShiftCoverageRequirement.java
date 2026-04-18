package com.shiftmate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Specifies the minimum number of employees in a given role required for a shift
 * to be considered adequately staffed before publication. For example, a Friday
 * Kitchen shift might require at least one Head Chef, one Sous Chef, and one Line
 * Cook — represented as three separate rows in this table.
 */
@Entity
@Table(
    name = "shift_coverage_requirement",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_scr_shift_role",
        columnNames = {"shift_id", "role_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"shift", "role"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ShiftCoverageRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Min(1)
    @Column(name = "min_count", nullable = false)
    private int minCount = 1;

    @Builder
    public ShiftCoverageRequirement(Shift shift, Role role, int minCount) {
        this.shift = shift;
        this.role = role;
        this.minCount = minCount;
    }
}
