package com.vanh.itam.softdelete;

import com.vanh.itam.asset.entity.Asset;
import com.vanh.itam.asset.entity.AssetStatus;
import com.vanh.itam.asset.repository.AssetRepository;
import com.vanh.itam.audit.entity.AuditSession;
import com.vanh.itam.audit.entity.AuditSessionStatus;
import com.vanh.itam.audit.repository.AuditSessionRepository;
import com.vanh.itam.employee.entity.Employee;
import com.vanh.itam.employee.repository.EmployeeRepository;
import com.vanh.itam.request.entity.Request;
import com.vanh.itam.request.entity.RequestStatus;
import com.vanh.itam.request.entity.RequestType;
import com.vanh.itam.request.repository.RequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Test — Soft Delete filter.
 * Kiểm tra rằng tất cả repository đều filter đúng `deleted_at IS NULL`
 * và không trả về bản ghi đã bị soft-delete.
 *
 * Dùng @DataJpaTest: chỉ load JPA context (Repository layer), không load toàn bộ
 * Spring Boot context → chạy nhanh hơn @SpringBootTest.
 *
 * Database: Testcontainers PostgreSQL thật (không phải H2) — đảm bảo Flyway migration
 * thật chạy và behavior nhất quán với production.
 *
 * QUAN TRỌNG: @DataJpaTest với Testcontainers cần:
 *   - @AutoConfigureTestDatabase(replace = NONE) để không thay thế bằng H2 in-memory
 *   - @Testcontainers + @Container để khởi động PostgreSQL container
 *   - @DynamicPropertySource để inject JDBC URL của container vào Spring context
 */
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("SoftDeleteIntegrationTest — Filter deleted_at IS NULL")
class SoftDeleteIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("itam_soft_delete_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Override Flyway location để chạy cả test migration (V20)
        registry.add("spring.flyway.locations",
                () -> "classpath:db/migration,classpath:db/test");
    }

    @Autowired private AssetRepository assetRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private RequestRepository requestRepository;
    @Autowired private AuditSessionRepository auditSessionRepository;

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Tìm asset từ seed data theo code.
     * Seed data V19 có sẵn: HN-LAP-0001 (AVAILABLE), HN-LAP-0002 (ASSIGNED), ...
     */
    private Asset findAssetByCodeOrFail(String code) {
        return assetRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Seed asset not found: " + code));
    }

    /**
     * Tìm employee từ seed data theo email.
     */
    private Employee findEmployeeByEmailOrFail(String email) {
        return employeeRepository.findByEmailAndNotDeleted(email)
                .orElseThrow(() -> new RuntimeException("Seed employee not found: " + email));
    }

    // ── Asset soft delete ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Asset — soft delete filter")
    class AssetSoftDeleteTests {

        @Test
        @DisplayName("findActiveById nên trả về asset khi chưa soft-delete")
        void findActiveById_shouldReturnAsset_whenNotDeleted() {
            Asset asset = findAssetByCodeOrFail("HN-LAP-0001");

            Optional<Asset> found = assetRepository.findActiveById(asset.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getCode()).isEqualTo("HN-LAP-0001");
        }

        @Test
        @DisplayName("findActiveById nên trả về empty sau khi soft-delete asset")
        void findActiveById_shouldReturnEmpty_afterSoftDelete() {
            Asset asset = findAssetByCodeOrFail("HN-LAP-0001");
            Long assetId = asset.getId();

            // Soft delete
            asset.softDelete();
            assetRepository.save(asset);

            // findActiveById phải lọc ra
            Optional<Asset> found = assetRepository.findActiveById(assetId);
            assertThat(found).isEmpty();

            // findById (không filter) vẫn tìm được
            Optional<Asset> rawFound = assetRepository.findById(assetId);
            assertThat(rawFound).isPresent();
            assertThat(rawFound.get().getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("findByCode nên trả về empty sau khi soft-delete")
        void findByCode_shouldReturnEmpty_afterSoftDelete() {
            Asset asset = findAssetByCodeOrFail("HN-MON-0001");

            asset.softDelete();
            assetRepository.save(asset);

            Optional<Asset> found = assetRepository.findByCode("HN-MON-0001");
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("findAllActive nên không trả về asset đã soft-delete")
        void findAllActive_shouldExcludeSoftDeletedAssets() {
            long countBefore = assetRepository.findAllActive(1L, null, null, null,
                    PageRequest.of(0, 100)).getTotalElements();

            // Soft delete 1 asset
            Asset asset = findAssetByCodeOrFail("HN-KBM-0001");
            asset.softDelete();
            assetRepository.save(asset);

            long countAfter = assetRepository.findAllActive(1L, null, null, null,
                    PageRequest.of(0, 100)).getTotalElements();

            assertThat(countAfter).isEqualTo(countBefore - 1);
        }

        @Test
        @DisplayName("findActiveAssetsByBranch nên không trả về asset đã soft-delete")
        void findActiveAssetsByBranch_shouldExcludeSoftDeleted() {
            int countBefore = assetRepository.findActiveAssetsByBranch(1L).size();

            Asset asset = findAssetByCodeOrFail("HN-NET-0001");
            asset.softDelete();
            assetRepository.save(asset);

            int countAfter = assetRepository.findActiveAssetsByBranch(1L).size();

            assertThat(countAfter).isEqualTo(countBefore - 1);
        }

        @Test
        @DisplayName("softDelete rồi restore lại → findActiveById tìm được")
        void findActiveById_shouldReturn_afterRestore() {
            Asset asset = findAssetByCodeOrFail("HN-LAP-0002");
            Long assetId = asset.getId();

            // Soft delete
            asset.softDelete();
            assetRepository.save(asset);
            assertThat(assetRepository.findActiveById(assetId)).isEmpty();

            // Restore
            asset.restore();
            assetRepository.save(asset);
            assertThat(assetRepository.findActiveById(assetId)).isPresent();
        }
    }

    // ── Employee soft delete ──────────────────────────────────────────────

    @Nested
    @DisplayName("Employee — soft delete filter")
    class EmployeeSoftDeleteTests {

        @Test
        @DisplayName("findActiveById nên trả về employee khi chưa soft-delete")
        void findActiveById_shouldReturnEmployee_whenNotDeleted() {
            Employee emp = findEmployeeByEmailOrFail("employee1@itam.local");

            Optional<Employee> found = employeeRepository.findActiveById(emp.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("employee1@itam.local");
        }

        @Test
        @DisplayName("findActiveById nên trả về empty sau khi soft-delete employee")
        void findActiveById_shouldReturnEmpty_afterSoftDelete() {
            Employee emp = findEmployeeByEmailOrFail("employee2@itam.local");
            Long empId = emp.getId();

            emp.softDelete();
            employeeRepository.save(emp);

            assertThat(employeeRepository.findActiveById(empId)).isEmpty();
        }

        @Test
        @DisplayName("findByEmailAndNotDeleted nên trả về empty sau khi soft-delete")
        void findByEmailAndNotDeleted_shouldReturnEmpty_afterSoftDelete() {
            Employee emp = findEmployeeByEmailOrFail("employee1@itam.local");

            emp.softDelete();
            employeeRepository.save(emp);

            Optional<Employee> found = employeeRepository.findByEmailAndNotDeleted("employee1@itam.local");
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("existsByEmail phải vẫn true sau khi soft-delete (ngăn tái dùng email)")
        void existsByEmail_shouldStillBeTrue_afterSoftDelete() {
            // Behavior đặc biệt: existsByEmail KHÔNG filter deleted_at
            // để ngăn tạo lại account với email đã dùng → đây là thiết kế có chủ đích
            Employee emp = findEmployeeByEmailOrFail("employee1@itam.local");

            emp.softDelete();
            employeeRepository.save(emp);

            // Email vẫn bị "chiếm" dù đã xóa mềm
            assertThat(employeeRepository.existsByEmail("employee1@itam.local")).isTrue();
        }

        @Test
        @DisplayName("findAllActive nên không trả về employee đã soft-delete")
        void findAllActive_shouldExcludeSoftDeletedEmployees() {
            long countBefore = employeeRepository.findAllActive(1L, null, null,
                    PageRequest.of(0, 100)).getTotalElements();

            Employee emp = findEmployeeByEmailOrFail("employee2@itam.local");
            emp.softDelete();
            employeeRepository.save(emp);

            long countAfter = employeeRepository.findAllActive(1L, null, null,
                    PageRequest.of(0, 100)).getTotalElements();

            assertThat(countAfter).isEqualTo(countBefore - 1);
        }
    }

    // ── Request soft delete ───────────────────────────────────────────────

    @Nested
    @DisplayName("Request — soft delete filter")
    class RequestSoftDeleteTests {

        @Test
        @DisplayName("findActiveById nên trả về empty sau khi soft-delete request")
        void findActiveById_shouldReturnEmpty_afterSoftDelete() {
            // Tạo request mới trong test
            Asset asset = findAssetByCodeOrFail("HN-LAP-0001");
            Employee employee = findEmployeeByEmailOrFail("employee1@itam.local");

            Request request = new Request();
            request.setAsset(asset);
            request.setEmployee(employee);
            request.setType(RequestType.ASSIGN);
            request.setStatus(RequestStatus.PENDING);
            Request saved = requestRepository.save(request);

            // Verify có thể find
            assertThat(requestRepository.findActiveById(saved.getId())).isPresent();

            // Soft delete
            saved.softDelete();
            requestRepository.save(saved);

            // findActiveById phải lọc ra
            assertThat(requestRepository.findActiveById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("findAllActive nên không trả về request đã soft-delete")
        void findAllActive_shouldExcludeSoftDeletedRequests() {
            Asset asset = findAssetByCodeOrFail("HN-LAP-0001");
            Employee employee = findEmployeeByEmailOrFail("employee1@itam.local");

            Request request = new Request();
            request.setAsset(asset);
            request.setEmployee(employee);
            request.setType(RequestType.ASSIGN);
            request.setStatus(RequestStatus.PENDING);
            Request saved = requestRepository.save(request);

            long countBefore = requestRepository.findAllActive(null, null, null,
                    PageRequest.of(0, 100)).getTotalElements();

            saved.softDelete();
            requestRepository.save(saved);

            long countAfter = requestRepository.findAllActive(null, null, null,
                    PageRequest.of(0, 100)).getTotalElements();

            assertThat(countAfter).isEqualTo(countBefore - 1);
        }

        @Test
        @DisplayName("existsActiveRequestForAsset nên trả false sau khi soft-delete request")
        void existsActiveRequestForAsset_shouldReturnFalse_afterSoftDelete() {
            Asset asset = findAssetByCodeOrFail("HN-LAP-0001");
            Employee employee = findEmployeeByEmailOrFail("employee1@itam.local");

            // Tạo request PENDING cho asset
            Request request = new Request();
            request.setAsset(asset);
            request.setEmployee(employee);
            request.setType(RequestType.ASSIGN);
            request.setStatus(RequestStatus.PENDING);
            Request saved = requestRepository.save(request);

            // Phải có active request
            assertThat(requestRepository.existsActiveRequestForAsset(asset.getId())).isTrue();

            // Soft delete request
            saved.softDelete();
            requestRepository.save(saved);

            // Sau khi xóa mềm → không còn active request
            assertThat(requestRepository.existsActiveRequestForAsset(asset.getId())).isFalse();
        }
    }

    // ── AuditSession soft delete ──────────────────────────────────────────

    @Nested
    @DisplayName("AuditSession — soft delete filter")
    class AuditSessionSoftDeleteTests {

        @Test
        @DisplayName("findActiveById nên trả về empty sau khi soft-delete audit session")
        void findActiveById_shouldReturnEmpty_afterSoftDelete() {
            Employee creator = findEmployeeByEmailOrFail("it.staff@itam.local");

            // Tìm branch từ asset seed
            Asset asset = findAssetByCodeOrFail("HN-LAP-0001");

            AuditSession session = new AuditSession();
            session.setBranch(asset.getBranch());
            session.setCreatedBy(creator);
            session.setStatus(AuditSessionStatus.IN_PROGRESS);
            session.setStartedAt(Instant.now());
            session.setExpiresAt(Instant.now().plusSeconds(86400 * 3));
            AuditSession saved = auditSessionRepository.save(session);

            // Verify có thể find
            assertThat(auditSessionRepository.findActiveById(saved.getId())).isPresent();

            // Soft delete
            saved.softDelete();
            auditSessionRepository.save(saved);

            // findActiveById phải lọc ra
            assertThat(auditSessionRepository.findActiveById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("findAllActive nên không trả về session đã soft-delete")
        void findAllActive_shouldExcludeSoftDeletedSessions() {
            Employee creator = findEmployeeByEmailOrFail("it.staff@itam.local");
            Asset asset = findAssetByCodeOrFail("HN-LAP-0001");

            // Tạo 2 sessions
            AuditSession session1 = new AuditSession();
            session1.setBranch(asset.getBranch());
            session1.setCreatedBy(creator);
            session1.setStatus(AuditSessionStatus.IN_PROGRESS);
            session1.setStartedAt(Instant.now());
            session1.setExpiresAt(Instant.now().plusSeconds(86400 * 3));
            AuditSession saved1 = auditSessionRepository.save(session1);

            AuditSession session2 = new AuditSession();
            session2.setBranch(asset.getBranch());
            session2.setCreatedBy(creator);
            session2.setStatus(AuditSessionStatus.IN_PROGRESS);
            session2.setStartedAt(Instant.now());
            session2.setExpiresAt(Instant.now().plusSeconds(86400 * 3));
            auditSessionRepository.save(session2);

            long countBefore = auditSessionRepository.findAllActive(null,
                    PageRequest.of(0, 100)).getTotalElements();

            // Soft delete session1
            saved1.softDelete();
            auditSessionRepository.save(saved1);

            long countAfter = auditSessionRepository.findAllActive(null,
                    PageRequest.of(0, 100)).getTotalElements();

            assertThat(countAfter).isEqualTo(countBefore - 1);
        }
    }
}
