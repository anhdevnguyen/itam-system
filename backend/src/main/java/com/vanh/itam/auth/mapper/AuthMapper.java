package com.vanh.itam.auth.mapper;

import com.vanh.itam.auth.dto.response.LoginResponse;
import com.vanh.itam.employee.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper cho auth module.
 * Ánh xạ Employee entity → LoginResponse.UserInfo.
 */
@Mapper(componentModel = "spring")
public interface AuthMapper {

    /**
     * Employee → LoginResponse.UserInfo
     * role.code → role (VD: IT_STAFF)
     * branch.id → branchId
     */
    @Mapping(target = "role", source = "role.code")
    @Mapping(target = "branchId", source = "branch.id")
    LoginResponse.UserInfo toUserInfo(Employee employee);
}
