package com.shiftmate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SetAllowedRolesRequest {

    @NotNull
    private List<Long> roleIds;
}
