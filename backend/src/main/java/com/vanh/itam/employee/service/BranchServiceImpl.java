package com.vanh.itam.employee.service;

import com.vanh.itam.common.exception.BusinessException;
import com.vanh.itam.employee.dto.request.CreateBranchRequest;
import com.vanh.itam.employee.dto.request.UpdateBranchRequest;
import com.vanh.itam.employee.dto.response.BranchResponse;
import com.vanh.itam.employee.entity.Branch;
import com.vanh.itam.employee.exception.BranchNotFoundException;
import com.vanh.itam.employee.mapper.BranchMapper;
import com.vanh.itam.employee.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;

    @Override
    public List<BranchResponse> getAll() {
        return branchRepository.findAllActive().stream()
                .map(branchMapper::toResponse)
                .toList();
    }

    @Override
    public BranchResponse getById(Long id) {
        return branchMapper.toResponse(findActiveOrThrow(id));
    }

    @Override
    @Transactional
    public BranchResponse create(CreateBranchRequest request) {
        if (branchRepository.existsByCode(request.getCode())) {
            throw new BusinessException("BRANCH_CODE_DUPLICATE", "Mã chi nhánh đã tồn tại");
        }
        Branch branch = branchMapper.toEntity(request);
        Branch saved = branchRepository.save(branch);
        log.info("Branch created: code={}, name={}", saved.getCode(), saved.getName());
        return branchMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BranchResponse update(Long id, UpdateBranchRequest request) {
        Branch branch = findActiveOrThrow(id);
        branch.setName(request.getName());
        if (request.getAddress() != null) branch.setAddress(request.getAddress());
        Branch saved = branchRepository.save(branch);
        log.info("Branch updated: id={}", id);
        return branchMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        Branch branch = findActiveOrThrow(id);
        long empCount = branchRepository.countActiveEmployeesByBranchId(id);
        if (empCount > 0) {
            throw new BusinessException("BRANCH_HAS_ACTIVE_DEPENDENCIES",
                    "Không thể xoá chi nhánh vì vẫn còn " + empCount + " nhân viên đang hoạt động");
        }
        branch.softDelete();
        branchRepository.save(branch);
        log.info("Branch soft-deleted: id={}", id);
    }

    @Override
    @Transactional
    public BranchResponse restore(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new BranchNotFoundException(id));
        branch.restore();
        return branchMapper.toResponse(branchRepository.save(branch));
    }

    private Branch findActiveOrThrow(Long id) {
        return branchRepository.findActiveById(id)
                .orElseThrow(() -> new BranchNotFoundException(id));
    }
}
