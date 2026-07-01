package com.vanh.itam.audit;

import com.vanh.itam.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test — Audit lifecycle đầy đủ.
 *
 * Luồng chính (theo docs/07-BUSINESS-RULES.md mục 4):
 *   Tạo session → scan 1 số asset → complete → kiểm tra discrepancy MISSING
 *   → resolve discrepancy (CONFIRM_LOST / FOUND)
 *
 * QUAN TRỌNG: Dùng flat @Order thay vì @Nested để đảm bảo thứ tự thực thi.
 * @Nested class order không được đảm bảo bởi @TestMethodOrder outer class.
 *
 * Seed data từ V19:
 *   - Branch HN (id=1), IT Staff (it.staff@itam.local)
 *   - Assets active (không DISPOSED): HN-LAP-0001, HN-MON-0001, HN-KBM-0001,
 *     HN-NET-0001, HN-LAP-0002, HN-PHN-0001, HN-LAP-0003, HN-PRN-0001 = 8 assets
 *   - Assets DISPOSED: HN-LAP-0000 → không tính trong audit
 */
@DisplayName("AuditIntegrationTest — Lifecycle đầy đủ")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditIntegrationTest extends AbstractIntegrationTest {

    private static final String AUDIT_URL = "/api/v1/audits";

    private String itStaffToken;
    private String adminToken;
    private String employeeToken;

    // Shared state — set trong các test đầu, dùng lại trong test sau
    private static Long createdSessionId;
    private static Long discrepancyId;

    @BeforeEach
    void setupTokens() throws Exception {
        itStaffToken  = loginAndGetToken("it.staff@itam.local",  "Itstaff@123");
        adminToken    = loginAndGetToken("admin@itam.local",     "Admin@123456");
        employeeToken = loginAndGetToken("employee1@itam.local", "Employee@123");
    }

    // ── Tạo session ────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("POST /audits — IT Staff tạo session → 201 IN_PROGRESS với expiresAt")
    void createSession_shouldReturn201_withInProgressStatus() throws Exception {
        String response = mockMvc.perform(post(AUDIT_URL)
                        .header("Authorization", "Bearer " + itStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"branchId": 1, "note": "Kiểm kê tháng 7/2026"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.branchId").value(1))
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.data.startedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.totalAssets").isNumber())
                .andReturn()
                .getResponse().getContentAsString();

        createdSessionId = objectMapper.readTree(response)
                .path("data").path("id").asLong();

        org.assertj.core.api.Assertions.assertThat(createdSessionId).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("POST /audits — EMPLOYEE tạo session → 403 Forbidden")
    void createSession_shouldReturn403_forEmployee() throws Exception {
        mockMvc.perform(post(AUDIT_URL)
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"branchId": 1}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    @DisplayName("POST /audits — thiếu branchId → 400 Bad Request")
    void createSession_shouldReturn400_whenBranchIdMissing() throws Exception {
        mockMvc.perform(post(AUDIT_URL)
                        .header("Authorization", "Bearer " + itStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    @DisplayName("POST /audits — không có token → 401 Unauthorized")
    void createSession_shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(post(AUDIT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"branchId": 1}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── Scan ───────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("POST /audits/{id}/scan — quét HN-LAP-0001 → 200, ghi nhận scan")
    void scan_shouldReturn200_whenAssetBelongsToSessionBranch() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(createdSessionId != null,
                "Cần chạy createSession (Order 1) trước");

        mockMvc.perform(post(AUDIT_URL + "/{id}/scan", createdSessionId)
                        .header("Authorization", "Bearer " + itStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assetCode": "HN-LAP-0001", "scannedLocation": "Phòng A1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assetCode").value("HN-LAP-0001"))
                .andExpect(jsonPath("$.data.scannedLocation").value("Phòng A1"));
    }

    @Test
    @Order(11)
    @DisplayName("POST /audits/{id}/scan — quét HN-MON-0001 → 200")
    void scan_shouldReturn200_forSecondAsset() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(createdSessionId != null,
                "Cần chạy createSession trước");

        mockMvc.perform(post(AUDIT_URL + "/{id}/scan", createdSessionId)
                        .header("Authorization", "Bearer " + itStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assetCode": "HN-MON-0001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assetCode").value("HN-MON-0001"));
    }

    @Test
    @Order(12)
    @DisplayName("POST /audits/{id}/scan — mã không tồn tại → 409 (BusinessException)")
    void scan_shouldReturn409_whenAssetCodeInvalid() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(createdSessionId != null,
                "Cần chạy createSession trước");

        // BusinessException("AUDIT_SCAN_ASSET_CODE_NOT_FOUND") mặc định trả 409 Conflict
        // (xem GlobalExceptionHandler + BusinessException — không phải 400 validation)
        mockMvc.perform(post(AUDIT_URL + "/{id}/scan", createdSessionId)
                        .header("Authorization", "Bearer " + itStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assetCode": "INVALID-CODE-9999"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(13)
    @DisplayName("POST /audits/{id}/scan — thiếu assetCode → 400 validation")
    void scan_shouldReturn400_whenAssetCodeMissing() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(createdSessionId != null,
                "Cần chạy createSession trước");

        mockMvc.perform(post(AUDIT_URL + "/{id}/scan", createdSessionId)
                        .header("Authorization", "Bearer " + itStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── Complete ───────────────────────────────────────────────────────────

    @Test
    @Order(20)
    @DisplayName("POST /audits/{id}/complete — → 200 COMPLETED, tạo MISSING cho asset chưa quét")
    void complete_shouldReturn200_andCreateMissingDiscrepancies() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(createdSessionId != null,
                "Cần chạy createSession trước");

        // Đã quét: HN-LAP-0001, HN-MON-0001
        // 6 asset còn lại (không DISPOSED) → 6 discrepancy MISSING
        mockMvc.perform(post(AUDIT_URL + "/{id}/complete", createdSessionId)
                        .header("Authorization", "Bearer " + itStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());
    }

    @Test
    @Order(21)
    @DisplayName("POST /audits/{id}/complete — complete lần 2 → 409 Conflict")
    void complete_shouldReturn409_whenAlreadyCompleted() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(createdSessionId != null,
                "Cần chạy complete (Order 20) trước");

        mockMvc.perform(post(AUDIT_URL + "/{id}/complete", createdSessionId)
                        .header("Authorization", "Bearer " + itStaffToken))
                .andExpect(status().isConflict());
    }

    // ── Discrepancies ──────────────────────────────────────────────────────

    @Test
    @Order(30)
    @DisplayName("GET /audits/{id}/discrepancies — sau complete có ≥1 MISSING discrepancy")
    void getDiscrepancies_shouldReturnMissingDiscrepancies_afterComplete() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(createdSessionId != null,
                "Cần chạy complete trước");

        String response = mockMvc.perform(
                        get(AUDIT_URL + "/{id}/discrepancies", createdSessionId)
                                .header("Authorization", "Bearer " + itStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[*].type", everyItem(is("MISSING"))))
                .andExpect(jsonPath("$.data[*].status", everyItem(is("OPEN"))))
                .andReturn()
                .getResponse().getContentAsString();

        // Lưu discrepancyId đầu tiên để dùng trong resolve tests
        com.fasterxml.jackson.databind.JsonNode data =
                objectMapper.readTree(response).path("data");
        if (data.isArray() && data.size() > 0) {
            discrepancyId = data.get(0).path("id").asLong();
        }

        org.assertj.core.api.Assertions.assertThat(discrepancyId)
                .as("Phải lấy được discrepancyId")
                .isNotNull().isPositive();
    }

    @Test
    @Order(31)
    @DisplayName("GET /audits/{id}/discrepancies?status=OPEN — chỉ trả OPEN discrepancies")
    void getDiscrepancies_filteredByOpenStatus_shouldReturnOnlyOpen() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(createdSessionId != null,
                "Cần chạy complete trước");

        mockMvc.perform(get(AUDIT_URL + "/{id}/discrepancies", createdSessionId)
                        .param("status", "OPEN")
                        .header("Authorization", "Bearer " + itStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].status", everyItem(is("OPEN"))));
    }

    // ── Resolve ────────────────────────────────────────────────────────────

    @Test
    @Order(40)
    @DisplayName("POST /audits/discrepancies/{id}/resolve — FOUND → RESOLVED, asset giữ nguyên status")
    void resolve_withFound_shouldMarkResolved_withoutChangingAsset() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(discrepancyId != null,
                "Cần chạy getDiscrepancies (Order 30) trước");

        // Lấy assetId của discrepancy đầu tiên
        String discResp = mockMvc.perform(
                        get(AUDIT_URL + "/{id}/discrepancies", createdSessionId)
                                .header("Authorization", "Bearer " + itStaffToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long assetIdToCheck = objectMapper.readTree(discResp)
                .path("data").get(0).path("assetId").asLong();

        // Lưu status asset trước khi resolve
        String assetBefore = mockMvc.perform(
                        get("/api/v1/assets/{id}", assetIdToCheck)
                                .header("Authorization", "Bearer " + itStaffToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String statusBefore = objectMapper.readTree(assetBefore)
                .path("data").path("status").asText();

        // Resolve với FOUND
        mockMvc.perform(post("/api/v1/audits/discrepancies/{id}/resolve", discrepancyId)
                        .header("Authorization", "Bearer " + itStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "FOUND"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolutionAction").value("FOUND"));

        // Asset phải giữ nguyên status
        mockMvc.perform(get("/api/v1/assets/{id}", assetIdToCheck)
                        .header("Authorization", "Bearer " + itStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(statusBefore));
    }

    @Test
    @Order(41)
    @DisplayName("POST /audits/discrepancies/{id}/resolve — đã RESOLVED → 409 Conflict")
    void resolve_shouldReturn409_whenAlreadyResolved() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(discrepancyId != null,
                "Cần chạy resolve (Order 40) trước");

        mockMvc.perform(post("/api/v1/audits/discrepancies/{id}/resolve", discrepancyId)
                        .header("Authorization", "Bearer " + itStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "FOUND"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(42)
    @DisplayName("POST /audits/discrepancies/{id}/resolve — CONFIRM_LOST → asset.status = LOST")
    void resolve_withConfirmLost_shouldSetAssetStatusToLost() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(createdSessionId != null,
                "Cần chạy complete trước");

        // Lấy discrepancy còn OPEN
        String discResp = mockMvc.perform(
                        get(AUDIT_URL + "/{id}/discrepancies", createdSessionId)
                                .param("status", "OPEN")
                                .header("Authorization", "Bearer " + itStaffToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode openDiscs =
                objectMapper.readTree(discResp).path("data");

        org.junit.jupiter.api.Assumptions.assumeTrue(
                openDiscs.isArray() && openDiscs.size() > 0,
                "Cần có ít nhất 1 discrepancy OPEN");

        long openDiscrepancyId = openDiscs.get(0).path("id").asLong();
        long assetId = openDiscs.get(0).path("assetId").asLong();

        mockMvc.perform(post("/api/v1/audits/discrepancies/{id}/resolve", openDiscrepancyId)
                        .header("Authorization", "Bearer " + itStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "CONFIRM_LOST"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolutionAction").value("CONFIRM_LOST"));

        // Asset phải bị LOST
        mockMvc.perform(get("/api/v1/assets/{id}", assetId)
                        .header("Authorization", "Bearer " + itStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LOST"));
    }

    @Test
    @Order(43)
    @DisplayName("POST /audits/discrepancies/{id}/resolve — thiếu action → 400 Bad Request")
    void resolve_shouldReturn400_whenActionMissing() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(discrepancyId != null);

        mockMvc.perform(post("/api/v1/audits/discrepancies/{id}/resolve", discrepancyId)
                        .header("Authorization", "Bearer " + itStaffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(44)
    @DisplayName("POST /audits/discrepancies/{id}/resolve — EMPLOYEE → 403 Forbidden")
    void resolve_shouldReturn403_forEmployee() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(discrepancyId != null);

        mockMvc.perform(post("/api/v1/audits/discrepancies/{id}/resolve", discrepancyId)
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action": "FOUND"}
                                """))
                .andExpect(status().isForbidden());
    }

    // ── GET Session ────────────────────────────────────────────────────────

    @Test
    @Order(50)
    @DisplayName("GET /audits/{id} — session tồn tại → 200 với totalAssets và totalScanned")
    void getSession_shouldReturn200_withFullInfo() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(createdSessionId != null,
                "Cần chạy createSession trước");

        mockMvc.perform(get(AUDIT_URL + "/{id}", createdSessionId)
                        .header("Authorization", "Bearer " + itStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdSessionId))
                .andExpect(jsonPath("$.data.branchId").value(1))
                .andExpect(jsonPath("$.data.totalAssets").isNumber())
                .andExpect(jsonPath("$.data.totalScanned").isNumber());
    }

    @Test
    @Order(51)
    @DisplayName("GET /audits/999999 — session không tồn tại → 404")
    void getSession_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get(AUDIT_URL + "/999999")
                        .header("Authorization", "Bearer " + itStaffToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(52)
    @DisplayName("GET /audits — danh sách sessions → 200, là array")
    void getAllSessions_shouldReturn200() throws Exception {
        mockMvc.perform(get(AUDIT_URL)
                        .header("Authorization", "Bearer " + itStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
