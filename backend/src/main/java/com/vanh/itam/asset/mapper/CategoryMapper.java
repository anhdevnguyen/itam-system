package com.vanh.itam.asset.mapper;

import com.vanh.itam.asset.dto.request.CreateCategoryRequest;
import com.vanh.itam.asset.dto.response.CategoryResponse;
import com.vanh.itam.asset.entity.Category;
import org.mapstruct.*;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Category toEntity(CreateCategoryRequest request);

    CategoryResponse toResponse(Category category);
}
