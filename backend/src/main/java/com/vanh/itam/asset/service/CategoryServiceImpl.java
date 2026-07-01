package com.vanh.itam.asset.service;

import com.vanh.itam.asset.dto.request.CreateCategoryRequest;
import com.vanh.itam.asset.dto.request.UpdateCategoryRequest;
import com.vanh.itam.asset.dto.response.CategoryResponse;
import com.vanh.itam.asset.entity.Category;
import com.vanh.itam.asset.exception.CategoryNotFoundException;
import com.vanh.itam.asset.mapper.CategoryMapper;
import com.vanh.itam.asset.repository.AssetRepository;
import com.vanh.itam.asset.repository.CategoryRepository;
import com.vanh.itam.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final AssetRepository assetRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAllActive().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getById(Long id) {
        return categoryMapper.toResponse(findActiveOrThrow(id));
    }

    @Override
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        if (categoryRepository.existsByCode(request.getCode())) {
            throw new BusinessException("CATEGORY_CODE_DUPLICATE", "Mã danh mục đã tồn tại");
        }
        Category category = categoryMapper.toEntity(request);
        Category saved = categoryRepository.save(category);
        log.info("Category created: code={}, name={}", saved.getCode(), saved.getName());
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, UpdateCategoryRequest request) {
        Category category = findActiveOrThrow(id);
        category.setName(request.getName());
        Category saved = categoryRepository.save(category);
        log.info("Category updated: id={}", id);
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        Category category = findActiveOrThrow(id);
        long assetCount = assetRepository.countActiveByCategoryId(id);
        if (assetCount > 0) {
            throw new BusinessException("CATEGORY_HAS_ACTIVE_ASSETS",
                    "Không thể xoá danh mục vì vẫn còn " + assetCount + " thiết bị đang sử dụng");
        }
        category.softDelete();
        categoryRepository.save(category);
        log.info("Category soft-deleted: id={}", id);
    }

    private Category findActiveOrThrow(Long id) {
        return categoryRepository.findActiveById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }
}
