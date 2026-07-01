package com.vanh.itam.asset.service;

import com.vanh.itam.asset.dto.request.CreateCategoryRequest;
import com.vanh.itam.asset.dto.request.UpdateCategoryRequest;
import com.vanh.itam.asset.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAll();
    CategoryResponse getById(Long id);
    CategoryResponse create(CreateCategoryRequest request);
    CategoryResponse update(Long id, UpdateCategoryRequest request);
    void softDelete(Long id);
}
