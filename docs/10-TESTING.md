# 10 — TESTING

> Chiến lược kiểm thử đầy đủ cho ITAM: công cụ, phạm vi test theo tầng, Test Data Strategy, và E2E testing. Áp dụng cho cả Backend (Spring Boot) và Frontend (React).

## Mục lục

1. [Tổng quan chiến lược Testing](#1-tổng-quan-chiến-lược-testing)
2. [Backend Testing — Công cụ](#2-backend-testing--công-cụ)
3. [Phạm vi Test — 2 tầng](#3-phạm-vi-test--2-tầng)
4. [Test Coverage — Ưu tiên theo Business Logic](#4-test-coverage--ưu-tiên-theo-business-logic)
5. [Unit Test — Ví dụ chi tiết](#5-unit-test--ví-dụ-chi-tiết)
6. [Integration Test — Testcontainers](#6-integration-test--testcontainers)
7. [Test Data Strategy](#7-test-data-strategy)
8. [E2E Testing (Frontend — Playwright)](#8-e2e-testing-frontend--playwright)
9. [CI/CD Test Gate](#9-cicd-test-gate)
10. [Quy tắc đặt tên Test](#10-quy-tắc-đặt-tên-test)
11. [TODO / Open Questions](#11-todo--open-questions)

---

## 1. Tổng quan chiến lược Testing

| Hạng mục | Lựa chọn |
|---|---|
| Backend Unit Test | JUnit 5 + Mockito |
| Backend Integration Test | Spring Boot Test (`@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`) |
| Database cho Integration Test | **Testcontainers** (PostgreSQL thật trong container) |
| Frontend E2E | Playwright |
| Test Coverage | **Không đặt mục tiêu % cứng** — ưu tiên test đúng business logic quan trọng |

**Triết lý xuyên suốt:** Vì đây là dự án quy mô MVP/demo (không phải hệ thống tài chính/y tế đòi hỏi coverage tuyệt đối), chiến lược test **ưu tiên chất lượng hơn số lượng** — tập trung vào các luồng nghiệp vụ có rủi ro cao nếu sai (workflow duyệt, RBAC, soft delete, audit/discrepancy logic, JWT auth flow) thay vì chạy theo chỉ số coverage hình thức.

## 2. Backend Testing — Công cụ

| Loại Test | Công cụ | Annotation chính |
|---|---|---|
| Unit Test | **JUnit 5 + Mockito** | `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks` |
| Integration Test (Controller) | **Spring Boot Test** | `@WebMvcTest`, `@AutoConfigureMockMvc` |
| Integration Test (Repository) | **Spring Boot Test** | `@DataJpaTest` |
| Integration Test (Full flow) | **Spring Boot Test** | `@SpringBootTest(webEnvironment = RANDOM_PORT)` |
| Database giả lập cho Integration Test | **Testcontainers** | `@Testcontainers`, `@Container` (PostgreSQL image) |

**Tại sao chọn Testcontainers thay vì H2 in-memory?** PostgreSQL có nhiều đặc trưng riêng (kiểu `TIMESTAMPTZ`, `CHECK` constraint phức tạp, hàm `now()`, cú pháp `SUBSTRING ... FROM ...` dùng trong sinh `asset.code` — xem `07-BUSINESS-RULES.md` mục 5) mà H2 mô phỏng **không chính xác hoàn toàn**. Testcontainers chạy **PostgreSQL thật** trong Docker container, đảm bảo test phản ánh đúng hành vi production.

## 3. Phạm vi Test — 2 tầng

### 3.1 Unit Test

**Phạm vi:** Service layer (business logic), Mapper, Util.

**Nguyên tắc:** **Mock toàn bộ Repository và external service** (Cloudinary, SendGrid) — Unit Test **không** chạm DB thật, **không** gọi network thật, chạy nhanh, cô lập hoàn toàn logic cần kiểm tra.

**Ví dụ mục tiêu Unit Test:**

- `RequestServiceImpl.approve()` — đúng logic scope check (Manager chỉ duyệt phòng mình)?
- `RequestServiceImpl.fulfill()` — đúng side-effect tạo `asset_assignment_history`, cập nhật `asset.status`?
- `AssetServiceImpl` — sinh `asset.code` đúng format?
- `AuditServiceImpl.complete()` — đúng logic tự động tạo discrepancy `MISSING` cho asset chưa quét?
- `AssetMapper`, `RequestMapper`... — map đúng field giữa Entity ↔ DTO?

### 3.2 Integration Test

**Phạm vi:** `Controller → Service → Repository → DB thật` (qua Testcontainers).

**Nguyên tắc:** Tập trung vào **luồng quan trọng** (không cần integration test cho mọi CRUD đơn giản — những API CRUD chuẩn đã được Spring Data JPA đảm bảo hoạt động đúng, rủi ro thấp).

**Luồng integration test ưu tiên:**

1. Login → nhận Access Token + Refresh Token cookie đúng.
2. Request workflow đầy đủ: tạo → duyệt → fulfill (xuyên suốt 3 role, kiểm tra side-effect DB thật).
3. Audit complete: tạo session → scan → complete → kiểm tra discrepancy `MISSING` được tạo đúng trong DB.
4. RBAC: gọi API với token sai role/sai scope → đúng `403 Forbidden`.

## 4. Test Coverage — Ưu tiên theo Business Logic

**Không đặt mục tiêu % coverage cứng** (VD: không bắt buộc "80% line coverage"). Thay vào đó, checklist các nhóm logic **bắt buộc phải có test** (Unit hoặc Integration, tuỳ phù hợp):

| Nhóm logic | Mức độ ưu tiên | Loại test phù hợp |
|---|---|---|
| Workflow duyệt request (state machine đầy đủ — `07-BUSINESS-RULES.md` mục 1) | 🔴 Cao | Unit (mỗi transition) + Integration (luồng end-to-end) |
| RBAC theo chi nhánh/phòng ban (`06-AUTHENTICATION.md` mục 5) | 🔴 Cao | Integration (gọi API thật với JWT giả lập từng role) |
| Soft delete (không cascade, filter `deleted_at`) | 🟡 Trung bình | Integration (`@DataJpaTest`) |
| Audit/Discrepancy logic (`07-BUSINESS-RULES.md` mục 4) | 🔴 Cao | Unit (logic phát hiện MISSING) + Integration (luồng đầy đủ) |
| JWT auth flow (login, refresh, revoke) | 🔴 Cao | Integration |
| Asset code generation (`07-BUSINESS-RULES.md` mục 5) | 🟡 Trung bình | Unit |
| Validation rule cơ bản (`@NotNull`, `@Size`...) | 🟢 Thấp | Đã được Jakarta Bean Validation đảm bảo — chỉ cần vài test mẫu xác nhận `GlobalExceptionHandler` map đúng |
| CRUD endpoint chuẩn không có business logic phức tạp | 🟢 Thấp | Không bắt buộc test riêng — rủi ro thấp |

## 5. Unit Test — Ví dụ chi tiết

**Ví dụ: Test `RequestServiceImpl.approve()` — đúng scope check theo phòng ban**

```java
@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock private RequestRepository requestRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private RequestMapper requestMapper;
    @InjectMocks private RequestServiceImpl requestService;

    @Test
    void approve_shouldThrowForbidden_whenManagerOutsideDepartmentScope() {
        // Arrange
        Long requestId = 1L;
        Long managerId = 99L; // Manager này KHÔNG phụ trách phòng ban của requester
        Request request = Request.builder().id(requestId).employeeId(5L).status(RequestStatus.PENDING).build();
        Employee requester = Employee.builder().id(5L).departmentId(10L).build();
        Department department = Department.builder().id(10L).managerId(1L).build(); // managerId khác managerId=99

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(requester));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));

        // Act & Assert
        assertThrows(ForbiddenException.class, () -> requestService.approve(requestId, managerId));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void approve_shouldThrowConflict_whenRequestAlreadyProcessed() {
        Long requestId = 1L;
        Request request = Request.builder().id(requestId).status(RequestStatus.FULFILLED).build();
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        // Giả định scope check pass trước đó (mock đầy đủ chain), tập trung test riêng nhánh status check
        // ... setup employeeRepository/departmentRepository mock tương ứng cho pass scope check

        assertThrows(BusinessException.class, () -> requestService.approve(requestId, 1L));
    }
}
```

**Ví dụ: Test sinh Asset Code đúng format**

```java
@Test
void generateAssetCode_shouldFollowCorrectFormat() {
    when(assetRepository.findNextSequence(1L, 2L)).thenReturn(7);

    String code = assetService.generateAssetCode("HN", "LAP", 1L, 2L);

    assertEquals("HN-LAP-0007", code);
}
```

## 6. Integration Test — Testcontainers

**Cấu hình base class dùng chung (gợi ý — `src/test/java/.../AbstractIntegrationTest.java`):**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("itam_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Flyway tự động chạy migration thật khi context khởi động — không cần cấu hình thêm
    }

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
}
```

> **Quan trọng:** Testcontainers tự động chạy **toàn bộ chuỗi migration Flyway thật** (`V1__init_roles.sql` đến `V19__seed_demo_data.sql` — xem `05-DATABASE.md` mục 9) khi Spring context khởi động cho test, đảm bảo schema test **luôn đồng bộ tuyệt đối** với schema production — không có rủi ro "test pass nhưng production fail" do lệch schema.

**Ví dụ Integration Test — luồng Request đầy đủ 3 role:**

```java
class RequestWorkflowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void fullRequestWorkflow_employeeCreateManagerApproveItStaffFulfill() throws Exception {
        // 1. Employee tạo request
        String employeeToken = loginAndGetToken("employee@itam.local", "Password123");
        MvcResult createResult = mockMvc.perform(post("/api/v1/requests")
                .header("Authorization", "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "type": "ASSIGN", "assetId": 1, "note": "Cần laptop" }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andReturn();

        Long requestId = extractRequestId(createResult);

        // 2. Manager duyệt
        String managerToken = loginAndGetToken("manager@itam.local", "Password123");
        mockMvc.perform(post("/api/v1/requests/{id}/approve", requestId)
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("APPROVED"));

        // 3. IT Staff fulfill
        String itStaffToken = loginAndGetToken("itstaff@itam.local", "Password123");
        mockMvc.perform(post("/api/v1/requests/{id}/fulfill", requestId)
                .header("Authorization", "Bearer " + itStaffToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("FULFILLED"));

        // 4. Kiểm tra side-effect trên Asset (DB thật qua Testcontainers)
        mockMvc.perform(get("/api/v1/assets/1")
                .header("Authorization", "Bearer " + itStaffToken))
            .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
            .andExpect(jsonPath("$.data.assignedTo").value(/* employee id */ 5));
    }
}
```

## 7. Test Data Strategy

**Pattern:** **Test Data Builder** — code Java tạo data trực tiếp trong test class (không dùng file SQL/JSON fixture rời rạc khó maintain).

```java
// Gợi ý Test Data Builder cho Asset
public class AssetTestDataBuilder {
    private String code = "HN-LAP-0001";
    private String name = "Test Laptop";
    private AssetStatus status = AssetStatus.AVAILABLE;
    private Long branchId = 1L;
    private Long categoryId = 1L;
    private BigDecimal value = new BigDecimal("20000000");
    private LocalDate purchaseDate = LocalDate.now().minusMonths(1);

    public static AssetTestDataBuilder anAsset() {
        return new AssetTestDataBuilder();
    }

    public AssetTestDataBuilder withStatus(AssetStatus status) {
        this.status = status;
        return this;
    }

    public AssetTestDataBuilder withAssignedTo(Long employeeId) {
        this.assignedTo = employeeId;
        return this;
    }

    public Asset build() {
        return Asset.builder()
            .code(code).name(name).status(status)
            .branchId(branchId).categoryId(categoryId)
            .value(value).purchaseDate(purchaseDate)
            .build();
    }
}

// Sử dụng trong test
Asset testAsset = AssetTestDataBuilder.anAsset()
    .withStatus(AssetStatus.ASSIGNED)
    .withAssignedTo(5L)
    .build();
```

> Kết hợp với **Flyway migration thật chạy tự động** (mục 6), Test Data Builder chỉ cần tạo thêm data **bổ sung** cho từng test case cụ thể — data nền tảng (roles, demo branch/employee mẫu) đã có sẵn từ `V19__seed_demo_data.sql`.

## 8. E2E Testing (Frontend — Playwright)

### 8.1 Phạm vi — 4 luồng happy path chính

| # | Luồng E2E | Mô tả |
|---|---|---|
| 1 | Login → xem dashboard | Đăng nhập thành công, redirect đúng trang, hiển thị thông tin user đúng role |
| 2 | Request 3-role workflow | Employee tạo request → Manager duyệt → IT Staff fulfill — xuyên suốt cả 3 role, dùng 3 session đăng nhập khác nhau trong cùng 1 test |
| 3 | Tạo Asset mới | IT Staff tạo asset mới → xác nhận xuất hiện đúng trong danh sách, đúng `code` tự sinh |
| 4 | Audit lifecycle | Tạo audit session → quét QR (giả lập input mã code thay vì camera thật) → complete → xem discrepancy phát sinh |

**Ví dụ Playwright test (luồng #2 — rút gọn):**

```ts
// e2e/request-workflow.spec.ts
import { test, expect } from '@playwright/test';

test('full request workflow across 3 roles', async ({ browser }) => {
  // Employee context
  const employeeContext = await browser.newContext();
  const employeePage = await employeeContext.newPage();
  await employeePage.goto('/login');
  await employeePage.fill('#email', 'employee@itam.local');
  await employeePage.fill('#password', 'Password123');
  await employeePage.click('button[type="submit"]');
  await employeePage.goto('/assets');
  await employeePage.click('text=Dell Latitude 5440');
  await employeePage.click('text=Yêu cầu cấp phát');
  await expect(employeePage.locator('text=PENDING')).toBeVisible();

  // Manager context (session riêng biệt)
  const managerContext = await browser.newContext();
  const managerPage = await managerContext.newPage();
  await managerPage.goto('/login');
  await managerPage.fill('#email', 'manager@itam.local');
  await managerPage.fill('#password', 'Password123');
  await managerPage.click('button[type="submit"]');
  await managerPage.goto('/requests');
  await managerPage.click('text=Duyệt');
  await expect(managerPage.locator('text=APPROVED')).toBeVisible();

  // IT Staff context
  const itStaffContext = await browser.newContext();
  const itStaffPage = await itStaffContext.newPage();
  await itStaffPage.goto('/login');
  await itStaffPage.fill('#email', 'itstaff@itam.local');
  await itStaffPage.fill('#password', 'Password123');
  await itStaffPage.click('button[type="submit"]');
  await itStaffPage.goto('/requests');
  await itStaffPage.click('text=Hoàn tất cấp phát');
  await expect(itStaffPage.locator('text=FULFILLED')).toBeVisible();
});
```

### 8.2 Vị trí trong CI/CD

**Quan trọng:** E2E test chạy **riêng biệt**, **không** gắn vào pipeline build/deploy chính (xem `11-DEPLOYMENT.md`) — tránh làm chậm hoặc phức tạp hoá CI/CD chính. E2E phù hợp chạy theo lịch định kỳ (nightly) hoặc thủ công trước khi release lớn, không bắt buộc mỗi lần push code.

## 9. CI/CD Test Gate

| Trigger | Test chạy | Hành vi nếu fail |
|---|---|---|
| Pull Request → `main` | Unit Test + Integration Test (Testcontainers) | **Chặn merge** — fail thì PR không thể merge |
| Push/Merge → `main` | Unit Test + Integration Test | **Dừng pipeline ngay**, không build Docker image, không deploy lên Render/Vercel |
| (Riêng biệt, không tự động trong pipeline chính) | E2E (Playwright) | Không chặn deploy — chạy độc lập theo lịch/thủ công |

→ Chi tiết pipeline đầy đủ: `11-DEPLOYMENT.md`.

## 10. Quy tắc đặt tên Test

| Loại | Convention | Ví dụ |
|---|---|---|
| Test method (JUnit 5) | `<methodUnderTest>_should<ExpectedBehavior>_when<Condition>` | `approve_shouldThrowForbidden_whenManagerOutsideDepartmentScope` |
| Test class | `<ClassUnderTest>Test` (Unit) / `<Feature>IntegrationTest` (Integration) | `RequestServiceImplTest`, `RequestWorkflowIntegrationTest` |
| E2E spec file | `<feature-kebab-case>.spec.ts` | `request-workflow.spec.ts` |

> Quy ước này **không được nghiên cứu gốc đặc tả tường minh** — là best practice bổ sung nhất quán với naming convention chung đã chốt (`03-CODING-STANDARDS.md`), giúp test case tự giải thích mục đích mà không cần đọc thêm comment.

## 11. TODO / Open Questions

Không có điểm nào trong Chủ đề Testing được đánh dấu cần xác nhận thêm trong nghiên cứu gốc. Quy tắc đặt tên test (mục 10) và một số ví dụ chi tiết implementation là suy luận hợp lý bổ sung dựa trên stack đã chốt (JUnit 5, Testcontainers, Playwright) — nếu team có quy ước khác, cập nhật trực tiếp file này.

---

*Xem tiếp: `11-DEPLOYMENT.md` để biết chi tiết CI/CD pipeline, secrets management và môi trường triển khai.*