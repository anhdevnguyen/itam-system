package com.vanh.itam.employee.mapper;

import com.vanh.itam.employee.dto.response.DepartmentResponse;
import com.vanh.itam.employee.entity.Department;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    @Mapping(target = "branchId", source = "branch.id")
    @Mapping(target = "branchName", source = "branch.name")
    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "managerName", source = "manager.fullName")
    DepartmentResponse toResponse(Department department);
}
