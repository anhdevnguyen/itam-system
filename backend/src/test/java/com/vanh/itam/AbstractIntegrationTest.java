package com.vanh.itam;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class cho tất cả Integration Test.
 *
 * - Dùng Testcontainers chạy PostgreSQL 16 thật trong Docker container.
 * - Flyway tự động migrate schema + seed data (V1→V20) khi context khởi động.
 * - Tất cả Integration Test kế thừa class này để tái dùng container và Spring context.
 *
 * QUAN TRỌNG — Tại sao KHÔNG dùng @Testcontainers:
 *   @Testcontainers trên abstract class gây lỗi với TestcontainersExtension:
 *   "required test class is not present in the current ExtensionContext"
 *   (testcontainers-java issue #1843). Thay vào đó, container được start thủ công
 *   trong static initializer — đảm bảo container luôn sẵn sàng trước khi
 *   @DynamicPropertySource được gọi.
 *
 * QUAN TRỌNG — Tại sao chỉ có 1 HikariPool:
 *   static container + @DynamicPropertySource → tất cả subclass dùng cùng
 *   datasource URL → Spring Test context cache reuse cùng 1 ApplicationContext
 *   → chỉ 1 HikariPool, không có deadlock khi shutdown.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    /**
     * Container PostgreSQL dùng chung cho toàn bộ test suite.
     * static + start() trong initializer → container khởi động 1 lần duy nhất,
     * tái dùng qua tất cả test class kế thừa AbstractIntegrationTest.
     */
    static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("itam_test")
                .withUsername("test")
                .withPassword("test");
        postgres.start(); // Start ngay trong static block — trước khi Spring context khởi động
    }

    /**
     * Override datasource properties bằng URL thật từ Testcontainers.
     * Chạy trước khi Spring context được tạo — đảm bảo Flyway connect đúng container.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Helper: đăng nhập và trả về accessToken.
     * Tài khoản demo từ V19__seed_demo_data.sql.
     * V20__test_disable_must_change_password.sql (trong classpath:db/test) tắt
     * must_change_password cho tất cả tài khoản — tránh bị MustChangePasswordFilter chặn.
     *
     * Nếu login trả status khác 200, in response body ra stderr để debug.
     */
    protected String loginAndGetToken(String email, String password) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);

        MvcResult result = mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(mvcResult -> {
                    int status = mvcResult.getResponse().getStatus();
                    if (status != 200) {
                        System.err.printf("[DEBUG loginAndGetToken] email=%s status=%d body=%s%n",
                                email, status, mvcResult.getResponse().getContentAsString());
                    }
                })
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json)
                .path("data")
                .path("accessToken")
                .asText();
    }
}
