package com.shiftmate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SetAllowedRolesRequest {

    @NotNull
    private List<Long> roleIds;
}
