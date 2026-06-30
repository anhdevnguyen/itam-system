package com.vanh.itam.common.config;

import com.vanh.itam.employee.entity.Employee;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Implements UserDetails — adapter giữa Employee entity và Spring Security.
 * Nhúng đầy đủ thông tin cần thiết để AuthService và Controller có thể truy xuất
 * mà không cần query DB lại.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long employeeId;
    private final String email;
    private final String passwordHash;
    private final String roleCode;
    private final Long branchId;
    private final boolean mustChangePassword;

    public CustomUserDetails(Employee employee) {
        this.employeeId = employee.getId();
        this.email = employee.getEmail();
        this.passwordHash = employee.getPasswordHash();
        this.roleCode = employee.getRole().getCode();
        this.branchId = employee.getBranch().getId();
        this.mustChangePassword = employee.isMustChangePassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security expects "ROLE_" prefix for hasRole() checks
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleCode));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
