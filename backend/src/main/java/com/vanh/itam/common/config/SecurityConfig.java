package com.vanh.itam.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vanh.itam.common.response.ApiError;
import com.vanh.itam.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Cấu hình Spring Security cho ITAM:
 * - Stateless JWT (không dùng session/cookie cho auth)
 * - CSRF disabled (API JSON, không có form HTML)
 * - Whitelist: /auth/login, /auth/refresh, /auth/logout, /actuator/health, Swagger
 * - JwtAuthenticationFilter chạy trước UsernamePasswordAuthenticationFilter
 * - @EnableMethodSecurity bật @PreAuthorize ở Controller/Service
 * - AuthenticationEntryPoint trả 401 đúng format ApiResponse khi unauthenticated
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final MustChangePasswordFilter mustChangePasswordFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — stateless JWT API
                .csrf(AbstractHttpConfigurer::disable)

                // CORS từ CorsConfig bean
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Stateless — không tạo HttpSession
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints — public (bao gồm logout để gọi không cần token)
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout"
                        ).permitAll()
                        // Actuator health — public
                        .requestMatchers("/actuator/health").permitAll()
                        // Swagger UI + OpenAPI docs — public (dev)
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**").permitAll()
                        // Mọi request khác phải xác thực
                        .anyRequest().authenticated()
                )

                // 401 khi không có hoặc có token không hợp lệ — đúng format ApiResponse
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedEntryPoint()))

                // Đặt JWT filter trước filter xác thực mặc định
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // must_change_password check — chạy sau JWT filter
                .addFilterAfter(mustChangePasswordFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Trả 401 Unauthorized đúng format ApiResponse khi request không có token hoặc token không hợp lệ.
     * Spring Security mặc định dùng Http403ForbiddenEntryPoint — cần override để trả 401.
     */
    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Void> body = ApiResponse.error(
                    new ApiError("AUTH_UNAUTHORIZED", null, "Bạn cần đăng nhập để thực hiện hành động này"));
            mapper.writeValue(response.getWriter(), body);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
