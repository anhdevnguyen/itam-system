package com.vanh.itam.asset.mapper;

import com.vanh.itam.asset.dto.request.CreateAssetRequest;
import com.vanh.itam.asset.dto.response.AssetAssignmentHistoryResponse;
import com.vanh.itam.asset.dto.response.AssetImageResponse;
import com.vanh.itam.asset.dto.response.AssetResponse;
import com.vanh.itam.asset.entity.Asset;
import com.vanh.itam.asset.entity.AssetAssignmentHistory;
import com.vanh.itam.asset.entity.AssetImage;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AssetMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "assignedTo", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "branch", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Asset toEntity(CreateAssetRequest request);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "branchId", source = "branch.id")
    @Mapping(target = "branchName", source = "branch.name")
    @Mapping(target = "assignedToId", source = "assignedTo.id")
    @Mapping(target = "assignedToName", source = "assignedTo.fullName")
    AssetResponse toResponse(Asset asset);

    @Mapping(target = "assetId", source = "asset.id")
    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "employeeName", source = "employee.fullName")
    @Mapping(target = "requestId", source = "request.id")
    AssetAssignmentHistoryResponse toHistoryResponse(AssetAssignmentHistory history);

    @Mapping(target = "assetId", source = "asset.id")
    AssetImageResponse toImageResponse(AssetImage image);
}
