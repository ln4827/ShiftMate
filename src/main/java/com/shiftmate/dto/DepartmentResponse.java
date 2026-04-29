package com.shiftmate.dto;

import com.shiftmate.entity.Department;
import lombok.Getter;

@Getter
public class DepartmentResponse {

    private final Long id;
    private final String name;

    private DepartmentResponse(Department d) {
        this.id   = d.getId();
        this.name = d.getName();
    }

    public static DepartmentResponse from(Department d) {
        return new DepartmentResponse(d);
    }
}
