package com.vanh.itam.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vanh.itam.common.response.ApiError;
import com.vanh.itam.common.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter enforce buộc đổi mật khẩu (must_change_password = true).
 * Khi user có must_change_password = true, chặn mọi API ngoại trừ:
 * - GET  /api/v1/employees/me
 * - PUT  /api/v1/employees/me/change-password
 * - POST /api/v1/auth/* (login, logout, refresh)
 * - OPTIONS (CORS preflight)
 * Trả 403 với mã AUTH_MUST_CHANGE_PASSWORD theo docs/06-AUTHENTICATION.md mục 10.
 */
@Component
@Slf4j
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public MustChangePasswordFilter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Chỉ kiểm tra khi đã authenticated và là CustomUserDetails
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            if (userDetails.isMustChangePassword()) {
                String path = request.getRequestURI();

                // Cho phép đi qua nếu là path được whitelist
                if (isAllowedPath(path, request.getMethod())) {
                    filterChain.doFilter(request, response);
                    return;
                }

                log.warn("Blocked request for must_change_password user: employeeId={}, path={}",
                        userDetails.getEmployeeId(), path);

                sendForbiddenResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedPath(String path, String method) {
        // Luôn cho qua OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) return true;
        // GET /api/v1/employees/me — xem thông tin user hiện tại (cần để frontend biết mustChangePassword)
        if ("GET".equalsIgnoreCase(method) && "/api/v1/employees/me".equals(path)) return true;
        // PUT /api/v1/employees/me/change-password — endpoint đổi mật khẩu
        if ("PUT".equalsIgnoreCase(method) && "/api/v1/employees/me/change-password".equals(path)) return true;
        // Auth endpoints
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/v1/auth/")) return true;
        // Swagger, actuator (dev tools)
        return path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")
                || path.startsWith("/api-docs") || path.startsWith("/actuator");
    }

    private void sendForbiddenResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> body = ApiResponse.error(new ApiError(
                "AUTH_MUST_CHANGE_PASSWORD",
                null,
                "Bạn cần đổi mật khẩu trước khi tiếp tục sử dụng hệ thống"));

        objectMapper.writeValue(response.getWriter(), body);
    }
}
