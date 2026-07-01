package com.vanh.itam.employee.service;

import com.vanh.itam.employee.dto.request.CreateBranchRequest;
import com.vanh.itam.employee.dto.request.UpdateBranchRequest;
import com.vanh.itam.employee.dto.response.BranchResponse;

import java.util.List;

public interface BranchService {
    List<BranchResponse> getAll();
    BranchResponse getById(Long id);
    BranchResponse create(CreateBranchRequest request);
    BranchResponse update(Long id, UpdateBranchRequest request);
    void softDelete(Long id);
    BranchResponse restore(Long id);
}
