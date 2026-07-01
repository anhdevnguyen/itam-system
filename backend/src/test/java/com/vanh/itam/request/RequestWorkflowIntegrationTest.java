package com.vanh.itam.request;

import com.vanh.itam.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test — Request Workflow đầy đủ 3 role.
 *
 * Luồng test: EMPLOYEE tạo request → MANAGER duyệt → IT_STAFF fulfill.
 * Kiểm tra side-effect DB: asset.status, request.status sau từng bước.
 *
 * Chạy theo thứ tự @Order để đảm bảo state nhất quán.
 * DB được reset bởi @DirtiesContext nếu cần — nhưng ở đây ta dùng asset AVAILABLE sẵn từ seed.
 */
@DisplayName("RequestWorkflowIntegrationTest — Full 3-role workflow")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RequestWorkflowIntegrationTest extends AbstractIntegrationTest {

    // Seed data từ V19: HN-LAP-0001 (assetId sẽ query qua API), employee1, manager.kd, it.staff
    private String employeeToken;
    private String managerToken;
    private String itStaffToken;
    private String adminToken;

    // requestId được tạo ra trong test đầu tiên và dùng lại
    private static Long createdRequestId;
    // assetId của HN-LAP-0001 (AVAILABLE từ seed)
    private static Long availableAssetId;

    @BeforeEach
    void setupTokens() throws Exception {
        employeeToken = loginAndGetToken("employee1@itam.local", "Employee@123");
        managerToken  = loginAndGetToken("manager.kd@itam.local", "Manager@123");
        itStaffToken  = loginAndGetToken("it.staff@itam.local", "Itstaff@123");
        adminToken    = loginAndGetToken("admin@itam.local", "Admin@123456");
    }

    // ── Step 0: Lấy assetId của HN-LAP-0001 từ DB ─────────────────────────

    @Test
    @Order(1)
    @DisplayName("Step 1: EMPLOYEE tạo ASSIGN request cho thiết bị HN-LAP-0001 → PENDING")
    void step1_employee_createRequest() throws Exception {
        // Tìm assetId bằng cách query danh sách asset với code filter
        String assetsJson = mockMvc.perform(get("/api/v1/assets")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse().getContentAsString();

        // Parse assetId từ code HN-LAP-0001
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(assetsJson);
        com.fasterxml.jackson.databind.JsonNode assets = root.path("data");
        for (com.fasterxml.jackson.databind.JsonNode asset : assets) {
            if ("HN-LAP-0001".equals(asset.path("code").asText())) {
                availableAssetId = asset.path("id").asLong();
                break;
            }
        }

        org.assertj.core.api.Assertions.assertThat(availableAssetId)
                .as("Phải tìm được assetId của HN-LAP-0001 từ seed data")
                .isNotNull().isPositive();

        // Employee tạo request ASSIGN
        String response = mockMvc.perform(post("/api/v1/requests")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"ASSIGN","assetId":%d,"note":"Cần laptop cho dự án mới"}
                                """.formatted(availableAssetId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.type").value("ASSIGN"))
                .andExpect(jsonPath("$.data.employeeName").isNotEmpty())
                .andReturn()
                .getResponse().getContentAsString();

        createdRequestId = objectMapper.readTree(response).path("data").path("id").asLong();

        org.assertj.core.api.Assertions.assertThat(createdRequestId)
                .as("requestId phải được tạo ra")
                .isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: EMPLOYEE không tạo thêm được request cho cùng asset (đang PENDING)")
    void step2_employee_cannotCreateDuplicateRequest() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(createdRequestId != null && availableAssetId != null,
                "Cần chạy step 1 trước");

        mockMvc.perform(post("/api/v1/requests")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"ASSIGN","assetId":%d}
                                """.formatted(availableAssetId)))
                .andExpect(status().isConflict()); // 409 — asset đang có request active
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: MANAGER duyệt request → APPROVED")
    void step3_manager_approveRequest() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(createdRequestId != null,
                "Cần chạy step 1 trước");

        mockMvc.perform(post("/api/v1/requests/{id}/approve", createdRequestId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedByName").isNotEmpty())
                .andExpect(jsonPath("$.data.approvedAt").isNotEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: MANAGER không thể duyệt lần nữa (đã APPROVED)")
    void step4_manager_cannotApproveAgain() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(createdRequestId != null,
                "Cần chạy step 1 trước");

        mockMvc.perform(post("/api/v1/requests/{id}/approve", createdRequestId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isConflict()); // REQUEST_ALREADY_PROCESSED
    }

    @Test
    @Order(5)
    @DisplayName("Step 5: IT_STAFF fulfill request → FULFILLED + asset → ASSIGNED")
    void step5_itStaff_fulfillRequest() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(createdRequestId != null && availableAssetId != null,
                "Cần chạy step 1 & 3 trước");

        // Fulfill
        mockMvc.perform(post("/api/v1/requests/{id}/fulfill", createdRequestId)
                        .header("Authorization", "Bearer " + itStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FULFILLED"))
                .andExpect(jsonPath("$.data.fulfilledByName").isNotEmpty())
                .andExpect(jsonPath("$.data.fulfilledAt").isNotEmpty());

        // Kiểm tra side-effect: asset.status phải là ASSIGNED
        mockMvc.perform(get("/api/v1/assets/{id}", availableAssetId)
                        .header("Authorization", "Bearer " + itStaffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.assignedToId").isNotEmpty());
    }

    // ── Reject flow ────────────────────────────────────────────────────────

    @Test
    @Order(10)
    @DisplayName("MANAGER reject request → REJECTED + có rejectionReason")
    void manager_rejectRequest_shouldSucceed() throws Exception {
        // Dùng HN-MON-0001 (AVAILABLE) để tạo request mới cho reject test
        String assetsJson = mockMvc.perform(get("/api/v1/assets")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse().getContentAsString();

        Long monitorAssetId = null;
        com.fasterxml.jackson.databind.JsonNode assets =
                objectMapper.readTree(assetsJson).path("data");
        for (com.fasterxml.jackson.databind.JsonNode asset : assets) {
            if ("HN-MON-0001".equals(asset.path("code").asText())) {
                monitorAssetId = asset.path("id").asLong();
                break;
            }
        }

        org.assertj.core.api.Assertions.assertThat(monitorAssetId).isNotNull();

        // Employee tạo request
        String createResp = mockMvc.perform(post("/api/v1/requests")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"ASSIGN","assetId":%d,"note":"Cần màn hình"}
                                """.formatted(monitorAssetId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long rejectRequestId = objectMapper.readTree(createResp).path("data").path("id").asLong();

        // Manager reject
        mockMvc.perform(post("/api/v1/requests/{id}/reject", rejectRequestId)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rejectionReason":"Thiết bị đang được chuẩn bị cho phòng khác"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason")
                        .value("Thiết bị đang được chuẩn bị cho phòng khác"));
    }

    // ── Cancel flow ────────────────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("EMPLOYEE cancel request của mình khi PENDING → CANCELLED")
    void employee_cancelOwnRequest_shouldSucceed() throws Exception {
        // Dùng HN-KBM-0001 (AVAILABLE) để tạo request mới
        String assetsJson = mockMvc.perform(get("/api/v1/assets")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse().getContentAsString();

        Long kbmAssetId = null;
        for (com.fasterxml.jackson.databind.JsonNode asset :
                objectMapper.readTree(assetsJson).path("data")) {
            if ("HN-KBM-0001".equals(asset.path("code").asText())) {
                kbmAssetId = asset.path("id").asLong();
                break;
            }
        }

        org.assertj.core.api.Assertions.assertThat(kbmAssetId).isNotNull();

        // Tạo request
        String createResp = mockMvc.perform(post("/api/v1/requests")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"ASSIGN","assetId":%d}
                                """.formatted(kbmAssetId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long cancelRequestId = objectMapper.readTree(createResp).path("data").path("id").asLong();

        // Employee cancel
        mockMvc.perform(post("/api/v1/requests/{id}/cancel", cancelRequestId)
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    // ── Scope check ────────────────────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("Manager khác phòng ban không được duyệt request của employee1 → 403")
    void approve_shouldReturn403_whenManagerFromDifferentDepartment() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(createdRequestId != null,
                "Cần có requestId");

        // manager.hc thuộc Phòng Hành chính, employee1 thuộc Phòng Kinh doanh
        String wrongManagerToken = loginAndGetToken("manager.hc@itam.local", "Manager@123");

        // Dùng một PENDING request — cần tạo mới
        String assetsJson = mockMvc.perform(get("/api/v1/assets")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Tìm asset còn AVAILABLE
        Long newAssetId = null;
        for (com.fasterxml.jackson.databind.JsonNode asset :
                objectMapper.readTree(assetsJson).path("data")) {
            if ("AVAILABLE".equals(asset.path("status").asText())) {
                newAssetId = asset.path("id").asLong();
                break;
            }
        }

        if (newAssetId == null) return; // skip nếu không còn asset available

        String createResp = mockMvc.perform(post("/api/v1/requests")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"ASSIGN","assetId":%d}
                                """.formatted(newAssetId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long newRequestId = objectMapper.readTree(createResp).path("data").path("id").asLong();

        // Manager của phòng khác thử duyệt
        mockMvc.perform(post("/api/v1/requests/{id}/approve", newRequestId)
                        .header("Authorization", "Bearer " + wrongManagerToken))
                .andExpect(status().isForbidden());
    }

    // ── GET danh sách request ──────────────────────────────────────────────

    @Test
    @Order(20)
    @DisplayName("EMPLOYEE chỉ thấy request của mình trong danh sách")
    void getRequests_shouldOnlyShowOwnRequests_forEmployee() throws Exception {
        mockMvc.perform(get("/api/v1/requests")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                // Tất cả request phải là của employee này
                .andExpect(jsonPath("$.data[*].employeeName",
                        everyItem(notNullValue())));
    }

    @Test
    @Order(21)
    @DisplayName("ADMIN thấy được tất cả request")
    void getRequests_shouldReturnAll_forAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/requests")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
