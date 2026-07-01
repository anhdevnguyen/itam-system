package com.vanh.itam.common.config;

import com.vanh.itam.common.util.JwtUtil;
import com.vanh.itam.employee.entity.Employee;
import com.vanh.itam.employee.repository.EmployeeRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter chạy 1 lần mỗi request.
 * Đọc JWT từ header Authorization, validate, và set SecurityContext nếu hợp lệ.
 * Lưu ý: Filter không throw exception — lỗi JWT chỉ log debug và bỏ qua,
 * Spring Security sẽ xử lý 401 khi endpoint yêu cầu authentication.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final EmployeeRepository employeeRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Không có header hoặc không phải Bearer token — bỏ qua, để Security chain tiếp theo xử lý
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.validateAndGetClaims(token);
            Long employeeId = Long.valueOf(claims.getSubject());

            // Chỉ set authentication nếu SecurityContext chưa có (tránh overwrite)
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                // JOIN FETCH role + branch trong 1 query — tránh LazyInitializationException
                // vì filter chạy ngoài transaction context, lazy proxy không thể initialize
                Employee employee = employeeRepository.findByIdWithRoleAndBranch(employeeId).orElse(null);

                // Employee không tồn tại hoặc đã bị soft-delete
                if (employee == null || employee.getDeletedAt() != null) {
                    log.debug("JWT valid but employee not found or deleted: id={}", employeeId);
                    filterChain.doFilter(request, response);
                    return;
                }

                CustomUserDetails userDetails = new CustomUserDetails(employee);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT authentication failed for request [{}]: {}",
                    request.getRequestURI(), e.getMessage());
            // Không set authentication — request sẽ bị từ chối bởi Spring Security nếu endpoint yêu cầu auth
        }

        filterChain.doFilter(request, response);
    }
}
