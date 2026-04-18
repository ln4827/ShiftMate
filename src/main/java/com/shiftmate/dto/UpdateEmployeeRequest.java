package com.shiftmate.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request payload for updating an existing employee's profile details.
 * All fields except {@code password} are required. Omitting or passing
 * {@code null} for {@code password} leaves the existing credential unchanged.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UpdateEmployeeRequest {

    @NotBlank(message = "First name is required.")
    @Size(max = 80)
    private String firstName;

    @NotBlank(message = "Last name is required.")
    @Size(max = 80)
    private String lastName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Must be a valid email address.")
    @Size(max = 150)
    private String email;

    /** New password to set. Pass {@code null} or omit to keep the existing password hash. */
    @Size(min = 8, max = 72, message = "Password must be 8–72 characters.")
    private String password;

    private boolean isManager;
}
