package com.shiftmate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Records an employee's request for time off over a contiguous date range.
 * Status transitions from {@code PENDING} to {@code APPROVED} or {@code REJECTED}
 * by a manager. Resolved requests record the deciding manager and the resolution
 * timestamp for audit purposes.
 */
@Entity
@Table(name = "time_off_request")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"employee", "resolvedBy"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TimeOffRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotBlank
    @Size(max = 500)
    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private Status status = Status.PENDING;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /** The manager who approved or rejected this request; {@code null} while pending. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private Employee resolvedBy;

    /**
     * Lifecycle states for a time-off request.
     */
    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    /**
     * Sets {@code requestedAt} to the current timestamp on first persist.
     */
    @PrePersist
    private void prePersist() {
        requestedAt = LocalDateTime.now();
    }

    @Builder
    public TimeOffRequest(Employee employee, LocalDate startDate,
                          LocalDate endDate, String reason) {
        this.employee = employee;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = Status.PENDING;
    }
}
