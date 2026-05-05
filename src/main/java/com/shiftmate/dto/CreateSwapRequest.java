package com.shiftmate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateSwapRequest {

    @NotNull
    private Long requesterAssignmentId;

    @NotNull
    private Long targetAssignmentId;
}
