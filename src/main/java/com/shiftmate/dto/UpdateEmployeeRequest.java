package com.shiftmate.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    private String password; // Correct: no validation, so it can be null/empty

    @JsonProperty("manager")
    private boolean isManager;

    private List<Long> roleIds; 
}