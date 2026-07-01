package com.vanh.itam.request.mapper;

import com.vanh.itam.request.dto.response.RequestResponse;
import com.vanh.itam.request.entity.Request;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    @Mapping(target = "assetId", source = "asset.id")
    @Mapping(target = "assetCode", source = "asset.code")
    @Mapping(target = "assetName", source = "asset.name")
    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeName", source = "employee.fullName")
    @Mapping(target = "approvedById", source = "approvedBy.id")
    @Mapping(target = "approvedByName", source = "approvedBy.fullName")
    @Mapping(target = "fulfilledById", source = "fulfilledBy.id")
    @Mapping(target = "fulfilledByName", source = "fulfilledBy.fullName")
    RequestResponse toResponse(Request request);
}
