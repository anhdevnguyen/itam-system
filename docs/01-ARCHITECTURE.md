# 01 — ARCHITECTURE

> Kiến trúc tổng thể hệ thống ITAM: phong cách kiến trúc, ranh giới module, luồng dữ liệu, luồng xác thực, và sơ đồ triển khai.

## Mục lục

1. [Kiến trúc tổng thể](#1-kiến-trúc-tổng-thể)
2. [Module Boundary — Backend](#2-module-boundary--backend)
3. [Cross-cutting Concerns](#3-cross-cutting-concerns)
4. [Frontend Architecture](#4-frontend-architecture)
5. [Authentication Flow (JWT)](#5-authentication-flow-jwt)
6. [Background Jobs](#6-background-jobs)
7. [API Response Format](#7-api-response-format)
8. [Multi-branch Data Isolation](#8-multi-branch-data-isolation)
9. [Luồng dữ liệu chính (Data Flow)](#9-luồng-dữ-liệu-chính-data-flow)
10. [Deployment Architecture](#10-deployment-architecture)
11. [Quyết định kiến trúc & Lý do (ADR rút gọn)](#11-quyết-định-kiến-trúc--lý-do-adr-rút-gọn)
12. [TODO / Open Questions](#12-todo--open-questions)

---

## 1. Kiến trúc tổng thể

| Quyết định | Lựa chọn |
|---|---|
| Kiến trúc hệ thống | **Monolith** (Spring Boot) |
| Kiến trúc tầng (layer) | **Layered Architecture**: `Controller → Service → Repository` |
| Tổ chức mã nguồn | **Package-by-feature** (không phải package-by-layer ở cấp gốc) |

**Lý do chọn Monolith:** Quy mô dự án (50–100 nhân viên, 200–300 thiết bị) không đòi hỏi microservice. Monolith giúp **dễ deploy lên Render**, giảm độ phức tạp vận hành (không cần service mesh, message broker, distributed tracing), phù hợp với mục tiêu "chi phí thấp, dễ bảo trì".

**Lý do chọn package-by-feature:** Mỗi nghiệp vụ (`asset`, `request`, `audit`...) đóng gói trọn vẹn `controller/service/repository/dto/entity/mapper/exception` riêng, giúp:
- Dễ định vị code liên quan đến 1 tính năng.
- Giảm coupling giữa các domain không liên quan.
- Dễ tách thành module/service riêng trong tương lai nếu cần scale (dù không phải mục tiêu hiện tại).

```
┌─────────────────────────────────────────────────────┐
│                     CLIENT (Browser)                  │
│        React + Vite + TypeScript (Vercel)             │
└───────────────────────┬─────────────────────────────┘
                         │ HTTPS / REST JSON
                         ▼
┌─────────────────────────────────────────────────────┐
│              SPRING BOOT MONOLITH (Render)             │
│  ┌─────────────────────────────────────────────────┐ │
│  │  Controller Layer  (REST endpoints, @Valid)       │ │
│  ├─────────────────────────────────────────────────┤ │
│  │  Service Layer     (business logic, transaction)  │ │
│  ├─────────────────────────────────────────────────┤ │
│  │  Repository Layer  (Spring Data JPA)               │ │
│  └─────────────────────────────────────────────────┘ │
└───────────────────────┬─────────────────────────────┘
                         │ JDBC
                         ▼
┌─────────────────────────────────────────────────────┐
│            PostgreSQL (Neon Cloud)                     │
└─────────────────────────────────────────────────────┘
```

## 2. Module Boundary — Backend

Backend tổ chức theo **6 feature package** + 1 package `common` dùng chung, dưới gốc `com.vanh.itam`:

```
com.vanh.itam
├── auth          (đăng nhập, JWT, RBAC)
├── employee      (nhân viên, chi nhánh, phòng ban)
├── asset         (danh mục thiết bị, QR code generation)
├── request       (yêu cầu cấp phát/thu hồi + workflow duyệt)
├── maintenance   (bảo hành/bảo trì)
├── audit         (kiểm kê định kỳ, quét QR, discrepancy report)
└── common        (cross-cutting: config, exception, response, util, base entity)
```

> Lưu ý: `employee` package bao gồm cả `Branch` và `Department` entity (chi nhánh, phòng ban) vì các entity này gắn chặt với domain quản lý nhân sự/tổ chức, không đủ lớn để tách package riêng ở MVP.

Mỗi feature package (trừ `common`) tuân theo cấu trúc con thống nhất:

```
asset/
├── controller/    AssetController
├── service/       AssetService (interface) + AssetServiceImpl
├── repository/    AssetRepository
├── dto/           CreateAssetRequest, UpdateAssetRequest, AssetResponse
├── entity/        Asset
├── mapper/        AssetMapper (MapStruct)
└── exception/     AssetNotFoundException, AssetNotAvailableException...
```

→ Chi tiết cây thư mục đầy đủ: `02-FOLDER-STRUCTURE.md`. Chi tiết naming convention: `03-CODING-STANDARDS.md`.

### Bảng ánh xạ module ↔ nghiệp vụ

| Package | Trách nhiệm chính | Entity chính |
|---|---|---|
| `auth` | Login, refresh token, logout, JWT issuance/validation | `RefreshToken` |
| `employee` | CRUD nhân viên, chi nhánh, phòng ban, reset password | `Employee`, `Branch`, `Department` |
| `asset` | CRUD thiết bị, danh mục (category), sinh QR code, upload ảnh | `Asset`, `Category`, `AssetImage`, `AssetAssignmentHistory` |
| `request` | Workflow tạo/duyệt/từ chối/fulfill/hủy yêu cầu | `Request` |
| `maintenance` | Lịch sử bảo hành/bảo trì | `MaintenanceRecord` |
| `audit` | Phiên kiểm kê, lượt quét QR, discrepancy | `AuditSession`, `AuditScan`, `Discrepancy` |

## 3. Cross-cutting Concerns

| Hạng mục | Giải pháp |
|---|---|
| Entity ↔ DTO Mapping | **MapStruct** (compile-time code generation, không viết mapping tay) |
| Exception Handling tập trung | `GlobalExceptionHandler` (`@RestControllerAdvice`) — bắt mọi exception từ các feature, map sang `ApiResponse` chuẩn (xem mục 7 và `09-ERROR-CODES.md`) |
| Giao tiếp giữa module | **Service gọi trực tiếp Service của module khác** (Direct method call, KHÔNG dùng event bus/message queue) |
| Base Entity | `BaseEntity` (abstract `@MappedSuperclass`): `id`, `createdAt`, `updatedAt`, `deletedAt` |
| Response Wrapper | `ApiResponse<T>` — bọc mọi response thành công/thất bại theo format thống nhất (mục 7) |
| Validation | Jakarta Bean Validation tại tầng Controller (`@Valid` trên DTO request) |
| Config | `SecurityConfig`, `CorsConfig`, `OpenApiConfig`, `SchedulerConfig` trong `common/config/` |

**Lý do chọn Direct Service Call thay vì Event-driven:** Với quy mô Monolith nhỏ, dùng event bus (Spring Application Events, message queue...) sẽ làm tăng độ phức tạp không cần thiết (khó trace luồng, khó debug) mà không mang lại lợi ích thực sự ở quy mô này. Nguyên tắc: tránh phức tạp hoá sớm (avoid premature complexity).

> Ví dụ: Khi `RequestService.fulfill()` được gọi, nó gọi trực tiếp `AssetService.updateAssignment(...)` để cập nhật `assigned_to` trên `Asset`, thay vì publish 1 event `RequestFulfilledEvent` rồi để `AssetService` lắng nghe.

## 4. Frontend Architecture

**Phong cách:** Feature-based (giống Backend) — mỗi feature gồm `components/`, `hooks/`, `pages/`, `types/` riêng.

```
frontend/src/
├── auth/
├── employees/
├── assets/
├── requests/
├── maintenance/
├── audit/          (mỗi feature: components/ hooks/ pages/ types/)
├── shared/          (Button, Modal, Table dùng chung + useAuth, useApi...)
├── lib/              (Axios instance, API client gốc)
└── routes/           (React Router config)
```

| Hạng mục | Lựa chọn |
|---|---|
| State Management | React Context / `useState` thuần (MVP — chưa cần Redux/Zustand) |
| HTTP Client | Axios (qua instance dùng chung trong `lib/`) |
| QR Scanning | `html5-qrcode` |
| Routing | React Router |

→ Chi tiết cây thư mục đầy đủ (kể cả phân biệt `frontend/src/assets/` static files vs domain "Asset" thiết bị): `02-FOLDER-STRUCTURE.md`.

## 5. Authentication Flow (JWT)

**Phương án:** Access Token trả về trong response body; Refresh Token lưu httpOnly cookie + bản ghi DB.

```
┌────────────┐                                  ┌────────────┐
│   Client    │                                  │   Backend   │
│  (React)    │                                  │ (Spring Boot)│
└─────┬──────┘                                  └─────┬──────┘
      │  POST /api/v1/auth/login {email, password}     │
      ├─────────────────────────────────────────────► │
      │                                                  │  Verify credentials (BCrypt)
      │                                                  │  Generate Access Token (JWT, 30 min)
      │                                                  │  Generate Refresh Token (30 days)
      │                                                  │  → Lưu refresh_tokens (DB)
      │  200 OK                                          │
      │  Body: { accessToken, user }                     │
      │  Set-Cookie: refreshToken=... (httpOnly, Secure) │
      │ ◄───────────────────────────────────────────── │
      │                                                  │
      │  Lưu accessToken vào React state (in-memory)     │
      │  KHÔNG lưu localStorage/sessionStorage            │
      │                                                  │
      │  GET /api/v1/assets                              │
      │  Header: Authorization: Bearer <accessToken>      │
      ├─────────────────────────────────────────────► │
      │                                                  │  Validate JWT signature + expiry
      │  200 OK { data }                                  │
      │ ◄───────────────────────────────────────────── │
      │                                                  │
      │  ... 30 phút sau, Access Token hết hạn ...        │
      │                                                  │
      │  POST /api/v1/auth/refresh                        │
      │  Cookie: refreshToken=... (tự động gửi)            │
      ├─────────────────────────────────────────────► │
      │                                                  │  Đọc refreshToken từ cookie
      │                                                  │  Đối chiếu DB (chưa revoke, chưa hết hạn)
      │  200 OK { accessToken mới }                        │
      │ ◄───────────────────────────────────────────── │
```

| Token | Thời hạn | Nơi lưu | Mục đích |
|---|---|---|---|
| **Access Token** | 30 phút | In-memory (React state) | Gửi kèm mọi request qua header `Authorization: Bearer <token>` |
| **Refresh Token** | 30 ngày | httpOnly cookie + bảng `refresh_tokens` (PostgreSQL) | Cấp lại Access Token mới khi hết hạn; hỗ trợ **revoke** (logout, đổi mật khẩu, nhân viên nghỉ việc) |

**Tại sao Access Token KHÔNG lưu localStorage?** Để chống XSS — JavaScript độc hại không thể đọc token nếu chỉ tồn tại trong memory (React state), mất khi refresh trang (chấp nhận được vì có Refresh Token tự động cấp lại).

**Tại sao Refresh Token lưu cả DB (không chỉ JWT stateless)?** Để hỗ trợ **revoke chủ động** — nếu chỉ dùng JWT thuần (stateless), không thể vô hiệu hoá 1 token cụ thể trước khi nó tự hết hạn (VD: khi nhân viên nghỉ việc, đổi mật khẩu, hoặc logout).

→ Chi tiết RBAC, Authorization Matrix, Password Policy: `06-AUTHENTICATION.md`.

## 6. Background Jobs

| Job | Công cụ | Tần suất | Mục đích |
|---|---|---|---|
| Nhắc nhở kỳ kiểm kê | Spring Scheduler (`@Scheduled`) | Định kỳ quý/năm | Nhắc IT Staff tạo Audit Session mới |
| Auto-expire Audit Session | Spring Scheduler (`@Scheduled`) | Quét định kỳ (gợi ý: mỗi giờ hoặc mỗi ngày) | Tự động `complete` các Audit Session quá hạn **3 ngày**, tự tạo discrepancy cho asset chưa quét (xem `07-BUSINESS-RULES.md`) |

> `SchedulerConfig` (trong `common/config/`) khai báo các `@Scheduled` job. Email gửi qua SendGrid được thực thi **bất đồng bộ** (`@Async`), không phải background job định kỳ mà là tác vụ nền theo sự kiện (event-triggered async task).

## 7. API Response Format

Toàn bộ API response (thành công lẫn lỗi) bọc trong cấu trúc thống nhất `success / data / errors / meta`, lấy cảm hứng đơn giản hoá từ JSON:API.

**Success Response:**

```json
{
  "success": true,
  "data": { "...": "..." },
  "meta": {
    "timestamp": "2026-06-30T10:00:00Z",
    "pagination": {
      "page": 1,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8
    }
  }
}
```

> `meta.pagination` chỉ xuất hiện ở endpoint danh sách (`GET` collection). Endpoint chi tiết/tạo/sửa chỉ trả `meta.timestamp`.

**Error Response:**

```json
{
  "success": false,
  "errors": [
    { "code": "VALIDATION_ERROR", "field": "email", "message": "Email is required" }
  ],
  "meta": {
    "timestamp": "2026-06-30T10:00:00Z"
  }
}
```

> `errors` luôn là **mảng** kể cả khi chỉ có 1 lỗi — đặc biệt quan trọng với validation error, hệ thống trả **TẤT CẢ** lỗi validation cùng lúc (xem `09-ERROR-CODES.md`).

**Java implementation pattern (gợi ý):**

```java
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private List<ApiError> errors;
    private Meta meta;

    public static <T> ApiResponse<T> success(T data) { ... }
    public static <T> ApiResponse<T> success(T data, Pagination pagination) { ... }
    public static ApiResponse<Void> error(List<ApiError> errors) { ... }
}
```

→ `ApiResponse` đặt tại `common/response/`. Toàn bộ `Controller` trả về `ResponseEntity<ApiResponse<T>>`.

## 8. Multi-branch Data Isolation

Dù MVP chỉ vận hành 1 chi nhánh, kiến trúc bắt buộc có sẵn `branch_id` để sẵn sàng mở rộng:

```
                    ┌───────────────────────────┐
                    │     ADMIN (Trung tâm)       │
                    │  Read-only toàn hệ thống    │
                    │  + Override khi cần          │
                    └─────────────┬──────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
│  Branch: HN        │   │  Branch: HCM       │   │  Branch: DN        │
│  IT_STAFF: full CRUD│   │  IT_STAFF: full CRUD│   │  IT_STAFF: full CRUD│
│  (chỉ data của HN)  │   │  (chỉ data của HCM) │   │  (chỉ data của DN)  │
└──────────────────┘   └──────────────────┘   └──────────────────┘
```

**Cơ chế kỹ thuật:**
- Mọi entity gắn chi nhánh (`Asset`, `Employee`...) có cột `branch_id` (FK → `branches`).
- Tầng `Service` áp dụng filter `branch_id` dựa trên `branch_id` của user đang đăng nhập (lấy từ JWT claims) đối với role `IT_STAFF`/`MANAGER`/`EMPLOYEE`.
- Role `ADMIN` **bỏ qua filter `branch_id`** ở tầng đọc dữ liệu (xem toàn hệ thống); ở tầng ghi dữ liệu, FE yêu cầu xác nhận thao tác override rõ ràng (xem `06-AUTHENTICATION.md` mục Admin Override).

## 9. Luồng dữ liệu chính (Data Flow)

**Luồng cấp phát thiết bị (request lifecycle):**

```
Employee                Manager              IT Staff              System
   │                       │                     │                    │
   │ POST /requests        │                     │                    │
   ├──────────────────────►│                     │                    │
   │   status=PENDING       │                     │                    │
   │                       │ POST /requests/{id}/approve                │
   │                       ├────────────────────►│                    │
   │                       │  status=APPROVED      │                    │
   │                       │                     │ POST /requests/{id}/fulfill
   │                       │                     ├───────────────────►│
   │                       │                     │  status=FULFILLED   │
   │                       │                     │                     │ → Tạo asset_assignment_history
   │                       │                     │                     │ → asset.status = ASSIGNED
   │                       │                     │                     │ → asset.assigned_to = employee_id
   │                       │                     │                     │ → Email + In-app notification
```

**Luồng kiểm kê (audit lifecycle):** xem chi tiết sơ đồ trạng thái tại `07-BUSINESS-RULES.md` mục Audit Rules.

## 10. Deployment Architecture

```
┌─────────────────────────── BACKEND FLOW ───────────────────────────┐
│                                                                       │
│  GitHub  →  GitHub Actions   →  Docker Image  →  GHCR  →  Render     │
│  (push)     (Build & Test)      (multi-stage)    (registry) (deploy) │
│                                                                       │
│                                                          │            │
│                                                          ▼            │
│                                                   Neon PostgreSQL      │
└───────────────────────────────────────────────────────────────────┘

┌─────────────────────────── FRONTEND FLOW ───────────────────────────┐
│                                                                        │
│  GitHub  →  Vercel (auto build & deploy)  →  Gọi API → Render Backend │
│                                                                        │
└────────────────────────────────────────────────────────────────────┘
```

| Hạng mục | Dịch vụ |
|---|---|
| Source Code | GitHub |
| CI/CD | GitHub Actions |
| Containerization | Docker |
| Container Registry | GitHub Container Registry (GHCR) |
| Backend Hosting | Render |
| Database Hosting | Neon |
| Frontend Hosting | Vercel |

→ Chi tiết pipeline CI/CD đầy đủ (trigger, steps, secrets): `11-DEPLOYMENT.md`.

## 11. Quyết định kiến trúc & Lý do (ADR rút gọn)

| # | Quyết định | Lý do | Đánh đổi (trade-off) chấp nhận |
|---|---|---|---|
| 1 | Monolith thay vì Microservice | Quy mô nhỏ, chi phí thấp, deploy đơn giản | Khó scale độc lập từng module sau này (chấp nhận được ở quy mô hiện tại) |
| 2 | Package-by-feature | Dễ định vị code, giảm coupling | Một số entity dùng chung (VD: `Branch`) phải gán vào 1 package "chủ" (`employee`) |
| 3 | Service gọi trực tiếp Service khác | Đơn giản, dễ trace, đủ dùng cho Monolith nhỏ | Coupling trực tiếp giữa các Service — chấp nhận vì cùng 1 process, cùng transaction boundary |
| 4 | Access Token in-memory, Refresh Token httpOnly cookie + DB | Cân bằng bảo mật (chống XSS) và khả năng revoke | Access Token mất khi refresh trang (được bù bằng cơ chế refresh tự động) |
| 5 | Polling thay vì WebSocket cho notification | Đơn giản, không cần hạ tầng realtime | Độ trễ thông báo tối đa 30-60s (chấp nhận được, không phải hệ thống real-time-critical) |

## 12. TODO / Open Questions

> TODO: Need confirmation — Chính sách **rotate Refresh Token** khi sử dụng (mỗi lần `/auth/refresh` có cấp Refresh Token mới và thu hồi token cũ hay không?) chưa được đặc tả trong nghiên cứu gốc. Khuyến nghị best practice: rotate để giảm rủi ro replay attack, nhưng cần Tech Lead xác nhận trước khi triển khai.

> TODO: Need confirmation — Lựa chọn kiểu Primary Key (`BIGINT IDENTITY` vs `UUID`) cho toàn bộ entity. Tài liệu này mặc định dùng `BIGINT GENERATED ALWAYS AS IDENTITY` (đơn giản, đủ dùng cho quy mô MVP) — xem chi tiết và lý do tại `05-DATABASE.md`.

---

*Xem tiếp: `02-FOLDER-STRUCTURE.md` để biết cấu trúc thư mục chi tiết triển khai kiến trúc này.*