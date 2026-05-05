package com.shiftmate.dto;

import com.shiftmate.entity.Department;
import lombok.Getter;

import java.util.List;

@Getter
public class DepartmentResponse {

    private final Long id;
    private final String name;
    private final List<Long> allowedRoleIds;

    private DepartmentResponse(Department d) {
        this.id             = d.getId();
        this.name           = d.getName();
        this.allowedRoleIds = d.getAllowedRoles().stream()
                               .map(r -> r.getId())
                               .sorted()
                               .toList();
    }

    public static DepartmentResponse from(Department d) {
        return new DepartmentResponse(d);
    }
}
