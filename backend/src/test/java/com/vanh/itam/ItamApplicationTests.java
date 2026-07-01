package com.vanh.itam;

import org.junit.jupiter.api.Test;

/**
 * Smoke test: Kiểm tra Spring context load thành công.
 *
 * Kế thừa AbstractIntegrationTest để:
 *   1. Tái dùng Testcontainers PostgreSQL container đã start sẵn.
 *   2. Dùng @DynamicPropertySource override datasource URL.
 *   3. Dùng @ActiveProfiles("test") load application-test.yml.
 *   4. KHÔNG tạo context mới — tránh HikariPool-2 và JVM deadlock.
 *
 * QUAN TRỌNG: Không thêm @SpringBootTest ở đây — AbstractIntegrationTest đã có rồi.
 * Thêm lại sẽ tạo context mới với config khác → 2 HikariPool → connection timeout → 500.
 */
class ItamApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // Spring context đã load thành công nếu test này chạy được.
        // Flyway migration V1→V20 đã chạy qua Testcontainers PostgreSQL.
    }
}
