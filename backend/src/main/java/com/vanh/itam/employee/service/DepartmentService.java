package com.vanh.itam.employee.service;

import com.vanh.itam.employee.dto.request.CreateDepartmentRequest;
import com.vanh.itam.employee.dto.request.UpdateDepartmentRequest;
import com.vanh.itam.employee.dto.response.DepartmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DepartmentService {
    Page<DepartmentResponse> getAll(Long branchId, Pageable pageable);
    DepartmentResponse getById(Long id);
    DepartmentResponse create(CreateDepartmentRequest request);
    DepartmentResponse update(Long id, UpdateDepartmentRequest request);
    void softDelete(Long id);
}
