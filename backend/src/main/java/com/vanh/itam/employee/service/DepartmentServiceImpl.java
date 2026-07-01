package com.vanh.itam.employee.service;

import com.vanh.itam.common.exception.BusinessException;
import com.vanh.itam.employee.dto.request.CreateDepartmentRequest;
import com.vanh.itam.employee.dto.request.UpdateDepartmentRequest;
import com.vanh.itam.employee.dto.response.DepartmentResponse;
import com.vanh.itam.employee.entity.Branch;
import com.vanh.itam.employee.entity.Department;
import com.vanh.itam.employee.entity.Employee;
import com.vanh.itam.employee.exception.BranchNotFoundException;
import com.vanh.itam.employee.exception.DepartmentNotFoundException;
import com.vanh.itam.employee.exception.EmployeeNotFoundException;
import com.vanh.itam.employee.mapper.DepartmentMapper;
import com.vanh.itam.employee.repository.BranchRepository;
import com.vanh.itam.employee.repository.DepartmentRepository;
import com.vanh.itam.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    public Page<DepartmentResponse> getAll(Long branchId, Pageable pageable) {
        return departmentRepository.findAllActivePaged(branchId, pageable)
                .map(departmentMapper::toResponse);
    }

    @Override
    public DepartmentResponse getById(Long id) {
        return departmentMapper.toResponse(findActiveOrThrow(id));
    }

    @Override
    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request) {
        Branch branch = branchRepository.findActiveById(request.getBranchId())
                .orElseThrow(() -> new BranchNotFoundException(request.getBranchId()));

        Department dept = new Department();
        dept.setName(request.getName());
        dept.setBranch(branch);

        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findActiveById(request.getManagerId())
                    .orElseThrow(() -> new EmployeeNotFoundException(request.getManagerId()));
            dept.setManager(manager);
        }

        Department saved = departmentRepository.save(dept);
        log.info("Department created: name={}, branchId={}", saved.getName(), branch.getId());
        return departmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DepartmentResponse update(Long id, UpdateDepartmentRequest request) {
        Department dept = findActiveOrThrow(id);
        dept.setName(request.getName());

        if (request.getManagerId() != null) {
            Employee manager = employeeRepository.findActiveById(request.getManagerId())
                    .orElseThrow(() -> new EmployeeNotFoundException(request.getManagerId()));
            dept.setManager(manager);
        } else {
            dept.setManager(null);
        }

        Department saved = departmentRepository.save(dept);
        log.info("Department updated: id={}", id);
        return departmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        Department dept = findActiveOrThrow(id);
        long count = departmentRepository.countActiveEmployeesByDepartmentId(id);
        if (count > 0) {
            throw new BusinessException("DEPARTMENT_HAS_ACTIVE_EMPLOYEES",
                    "Không thể xoá phòng ban vì vẫn còn " + count + " nhân viên đang hoạt động");
        }
        dept.softDelete();
        departmentRepository.save(dept);
        log.info("Department soft-deleted: id={}", id);
    }

    private Department findActiveOrThrow(Long id) {
        return departmentRepository.findActiveById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));
    }
}
