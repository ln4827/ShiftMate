package com.shiftmate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A scheduled shift slot within a department. Shifts are created in draft state
 * ({@code isPublished = false}) and made visible to employees only after a manager
 * explicitly publishes them. The {@code version} field prevents two managers from
 * overwriting each other's assignment changes on the same shift concurrently.
 */
@Entity
@Table(name = "shift")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"department", "createdBy", "assignments", "coverageRequirements"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @NotNull
    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** {@code false} until a manager publishes the shift; employees cannot see unpublished shifts. */
    @Column(name = "is_published", nullable = false)
    private boolean isPublished = false;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Employee createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Incremented by Hibernate on every UPDATE; guards against concurrent assignment edits. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShiftAssignment> assignments = new ArrayList<>();

    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShiftCoverageRequirement> coverageRequirements = new ArrayList<>();

    /**
     * Sets {@code createdAt} to the current timestamp on first persist.
     */
    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Returns {@code true} if this is an overnight shift where the end time
     * does not exceed the start time (e.g. 22:00–02:00). Used by overlap
     * detection logic to handle cross-midnight scheduling correctly.
     *
     * @return {@code true} if the shift spans midnight
     */
    @Transient
    public boolean isOvernight() {
        return !endTime.isAfter(startTime);
    }

    @Builder
    public Shift(Department department, LocalDate shiftDate,
                 LocalTime startTime, LocalTime endTime,
                 boolean isPublished, Employee createdBy) {
        this.department = department;
        this.shiftDate = shiftDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isPublished = isPublished;
        this.createdBy = createdBy;
    }
}
