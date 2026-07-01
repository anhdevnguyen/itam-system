package com.vanh.itam.maintenance.mapper;

import com.vanh.itam.maintenance.dto.request.CreateMaintenanceRequest;
import com.vanh.itam.maintenance.dto.response.MaintenanceResponse;
import com.vanh.itam.maintenance.entity.MaintenanceRecord;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MaintenanceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "asset", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "completedDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    MaintenanceRecord toEntity(CreateMaintenanceRequest request);

    @Mapping(target = "assetId", source = "asset.id")
    @Mapping(target = "assetCode", source = "asset.code")
    @Mapping(target = "assetName", source = "asset.name")
    MaintenanceResponse toResponse(MaintenanceRecord record);
}
