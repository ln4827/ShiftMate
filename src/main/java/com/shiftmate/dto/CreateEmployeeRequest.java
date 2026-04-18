package com.shiftmate.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

/**
 * Request payload for creating a new employee account. All fields are validated
 * before the request reaches the service layer.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreateEmployeeRequest {

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

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 72, message = "Password must be 8–72 characters.")
    private String password;

    private boolean isManager = false;

    /** Optional list of role IDs to assign to the employee on creation. May be null or empty. */
    private List<Long> roleIds;
}
