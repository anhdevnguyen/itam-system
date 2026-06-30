package com.vanh.itam.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Cấu hình Spring Security cho ITAM:
 * - Stateless JWT (không dùng session/cookie cho auth)
 * - CSRF disabled (API JSON, không có form HTML)
 * - Whitelist: /auth/login, /auth/refresh, /actuator/health, Swagger
 * - JwtAuthenticationFilter chạy trước UsernamePasswordAuthenticationFilter
 * - @EnableMethodSecurity bật @PreAuthorize ở Controller/Service
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
                        // Auth endpoints — public
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        // Actuator health — public
                        .requestMatchers("/actuator/health").permitAll()
                        // Swagger UI + OpenAPI docs — public (dev)
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**").permitAll()
                        // Mọi request khác phải xác thực
                        .anyRequest().authenticated()
                )

                // Đặt JWT filter trước filter xác thực mặc định
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // must_change_password check — chạy sau JWT filter
                .addFilterAfter(mustChangePasswordFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
