package com.vanh.itam.auth;

import com.vanh.itam.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test — Auth flow.
 * Kiểm tra: login, token trả đúng, refresh token cookie, RBAC 403.
 * Chạy trên PostgreSQL thật qua Testcontainers + Flyway V1→V20 migration.
 */
@DisplayName("AuthIntegrationTest — Login, Refresh, RBAC")
class AuthIntegrationTest extends AbstractIntegrationTest {

    private static final String LOGIN_URL   = "/api/v1/auth/login";
    private static final String REFRESH_URL = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL  = "/api/v1/auth/logout";

    // ── Login Flow ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/login")
    class LoginTests {

        @Test
        @DisplayName("Nên trả 200 + accessToken khi đăng nhập đúng thông tin")
        void login_shouldReturn200WithAccessToken_whenValidCredentials() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"admin@itam.local","password":"Admin@123456"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.user.email").value("admin@itam.local"))
                    .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
        }

        @Test
        @DisplayName("Nên set refreshToken httpOnly cookie sau khi login thành công")
        void login_shouldSetRefreshTokenCookie_whenSuccess() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"it.staff@itam.local","password":"Itstaff@123"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().httpOnly("refreshToken", true))
                    .andExpect(cookie().path("refreshToken", "/api/v1/auth"));
        }

        @Test
        @DisplayName("Nên trả 401 khi mật khẩu sai")
        void login_shouldReturn401_whenWrongPassword() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"admin@itam.local","password":"WrongPassword!"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Nên trả 401 khi email không tồn tại")
        void login_shouldReturn401_whenEmailNotFound() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"nobody@itam.local","password":"SomePass@123"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Nên trả 400 khi thiếu email hoặc password")
        void login_shouldReturn400_whenMissingFields() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"admin@itam.local"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Nên trả 400 khi email sai format")
        void login_shouldReturn400_whenInvalidEmailFormat() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"not-an-email","password":"Admin@123456"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("IT Staff login nên trả role IT_STAFF đúng")
        void login_shouldReturnCorrectRole_forItStaff() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"it.staff@itam.local","password":"Itstaff@123"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.user.role").value("IT_STAFF"));
        }

        @Test
        @DisplayName("Manager login nên trả role MANAGER đúng")
        void login_shouldReturnCorrectRole_forManager() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"manager.kd@itam.local","password":"Manager@123"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.user.role").value("MANAGER"));
        }

        @Test
        @DisplayName("Employee login nên trả role EMPLOYEE đúng")
        void login_shouldReturnCorrectRole_forEmployee() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"employee1@itam.local","password":"Employee@123"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.user.role").value("EMPLOYEE"));
        }
    }

    // ── RBAC — 403 Forbidden ───────────────────────────────────────────────

    @Nested
    @DisplayName("RBAC — 403 khi sai role")
    class RbacTests {

        @Test
        @DisplayName("EMPLOYEE không được gọi approve — nên trả 403")
        void approve_shouldReturn403_whenCalledByEmployee() throws Exception {
            String employeeToken = loginAndGetToken("employee1@itam.local", "Employee@123");

            mockMvc.perform(post("/api/v1/requests/1/approve")
                            .header("Authorization", "Bearer " + employeeToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("MANAGER không được gọi fulfill — nên trả 403")
        void fulfill_shouldReturn403_whenCalledByManager() throws Exception {
            String managerToken = loginAndGetToken("manager.kd@itam.local", "Manager@123");

            mockMvc.perform(post("/api/v1/requests/1/fulfill")
                            .header("Authorization", "Bearer " + managerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("MANAGER không được tạo request — nên trả 403")
        void createRequest_shouldReturn403_whenCalledByManager() throws Exception {
            String managerToken = loginAndGetToken("manager.kd@itam.local", "Manager@123");

            mockMvc.perform(post("/api/v1/requests")
                            .header("Authorization", "Bearer " + managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"type":"ASSIGN","assetId":1}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("IT_STAFF không được gọi approve — nên trả 403")
        void approve_shouldReturn403_whenCalledByItStaff() throws Exception {
            String itToken = loginAndGetToken("it.staff@itam.local", "Itstaff@123");

            mockMvc.perform(post("/api/v1/requests/1/approve")
                            .header("Authorization", "Bearer " + itToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Gọi API không có token — nên trả 401")
        void anyProtectedEndpoint_shouldReturn401_whenNoToken() throws Exception {
            mockMvc.perform(get("/api/v1/requests"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Gọi API với token giả — nên trả 401")
        void anyProtectedEndpoint_shouldReturn401_whenFakeToken() throws Exception {
            mockMvc.perform(get("/api/v1/assets")
                            .header("Authorization", "Bearer fake.jwt.token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── Logout ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("Nên trả 200 khi logout (có hoặc không có cookie)")
        void logout_shouldReturn200_regardlessOfCookie() throws Exception {
            mockMvc.perform(post(LOGOUT_URL))
                    .andExpect(status().isOk());
        }
    }
}
