package com.vanh.itam.employee.mapper;

import com.vanh.itam.employee.dto.response.EmployeeResponse;
import com.vanh.itam.employee.entity.Employee;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(target = "roleId", source = "role.id")
    @Mapping(target = "roleCode", source = "role.code")
    @Mapping(target = "branchId", source = "branch.id")
    @Mapping(target = "branchName", source = "branch.name")
    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    EmployeeResponse toResponse(Employee employee);
}
