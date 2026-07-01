package com.vanh.itam.asset.exception;

import com.vanh.itam.common.exception.ResourceNotFoundException;

public class CategoryNotFoundException extends ResourceNotFoundException {

    public CategoryNotFoundException(Long id) {
        super("CATEGORY_NOT_FOUND", "Không tìm thấy danh mục với ID: " + id);
    }
}
