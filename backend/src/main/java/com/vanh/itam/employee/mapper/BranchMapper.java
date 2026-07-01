package com.vanh.itam.employee.mapper;

import com.vanh.itam.employee.dto.request.CreateBranchRequest;
import com.vanh.itam.employee.dto.response.BranchResponse;
import com.vanh.itam.employee.entity.Branch;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BranchMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Branch toEntity(CreateBranchRequest request);

    BranchResponse toResponse(Branch branch);
}
