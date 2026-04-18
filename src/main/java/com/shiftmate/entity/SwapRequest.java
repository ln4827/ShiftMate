package com.shiftmate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Records a request from one employee to swap their shift assignment with another
 * employee's assignment. The status transitions from {@code PENDING} to either
 * {@code APPROVED} or {@code REJECTED} by a manager. When approved, both
 * assignments are atomically exchanged within a single transaction. All foreign
 * keys are retained after resolution to provide a complete audit trail.
 */
@Entity
@Table(
    name = "swap_request",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_sr_assignments",
        columnNames = {"requester_assignment_id", "target_assignment_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"requesterAssignment", "targetAssignment", "resolvedBy"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SwapRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_assignment_id", nullable = false)
    private ShiftAssignment requesterAssignment;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_assignment_id", nullable = false)
    private ShiftAssignment targetAssignment;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private Status status = Status.PENDING;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /** The manager who approved or rejected the request; {@code null} while the request is pending. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private Employee resolvedBy;

    /**
     * Lifecycle states for a swap request.
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
    public SwapRequest(ShiftAssignment requesterAssignment,
                       ShiftAssignment targetAssignment) {
        this.requesterAssignment = requesterAssignment;
        this.targetAssignment = targetAssignment;
        this.status = Status.PENDING;
    }
}
