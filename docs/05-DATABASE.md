# 05 — DATABASE

> Schema database đầy đủ cho hệ thống ITAM trên PostgreSQL (Neon Cloud). Bao gồm danh sách bảng, quan hệ, DDL, enum, index, chiến lược migration và seed data.

## Mục lục

1. [DBMS & Naming Convention](#1-dbms--naming-convention)
2. [Danh sách bảng (15 bảng)](#2-danh-sách-bảng-15-bảng)
3. [Sơ đồ quan hệ (ERD)](#3-sơ-đồ-quan-hệ-erd)
4. [Enum chính](#4-enum-chính)
5. [DDL chi tiết từng bảng](#5-ddl-chi-tiết-từng-bảng)
6. [Business Logic tự động](#6-business-logic-tự-động)
7. [Soft Delete & FK Behavior](#7-soft-delete--fk-behavior)
8. [Index Strategy](#8-index-strategy)
9. [Migration Strategy (Flyway)](#9-migration-strategy-flyway)
10. [Seed Data](#10-seed-data)
11. [TODO / Open Questions](#11-todo--open-questions)

---

## 1. DBMS & Naming Convention

| Hạng mục | Giá trị |
|---|---|
| DBMS | **PostgreSQL** (qua Neon Cloud, hỗ trợ branching) |
| Naming convention | `snake_case` cho table và column (nhất quán với `03-CODING-STANDARDS.md`) |
| Primary Key | `BIGINT GENERATED ALWAYS AS IDENTITY` — xem lý do và phương án thay thế tại mục 11 (TODO) |
| Migration tool | Flyway (versioned SQL script) — KHÔNG dùng Hibernate `ddl-auto: update` |

## 2. Danh sách bảng (15 bảng)

| # | Bảng | Mô tả | Nguồn |
|---|---|---|---|
| 1 | `roles` | Seed data cố định: ADMIN, IT_STAFF, MANAGER, EMPLOYEE | Chủ đề Database |
| 2 | `branches` | Chi nhánh | Chủ đề Database |
| 3 | `departments` | Phòng ban (thuộc 1 branch, có 1 Manager quản lý) | Chủ đề Database |
| 4 | `employees` | Nhân viên (FK role, branch, department) | Chủ đề Database |
| 5 | `refresh_tokens` | JWT refresh token (lưu DB để revoke được) | Chủ đề Database |
| 6 | `assets` | Thiết bị | Chủ đề Database |
| 7 | `asset_images` | Ảnh thiết bị (1-n, qua Cloudinary) | Chủ đề Database |
| 8 | `asset_assignment_history` | Lịch sử cấp phát (audit trail ai từng giữ thiết bị) | Chủ đề Database |
| 9 | `requests` | Yêu cầu cấp phát/thu hồi + workflow duyệt | Chủ đề Database |
| 10 | `maintenance_records` | Lịch sử bảo hành/bảo trì | Chủ đề Database |
| 11 | `audit_sessions` | Phiên kiểm kê định kỳ | Chủ đề Database |
| 12 | `audit_scans` | Lượt quét QR trong 1 phiên | Chủ đề Database |
| 13 | `discrepancies` | Báo cáo sai lệch phát hiện qua kiểm kê | Chủ đề Database |
| 14 | `categories` | Danh mục loại thiết bị (tách bảng riêng, không hardcode enum) | Bổ sung từ Chủ đề Business Rules |
| 15 | `notifications` | Thông báo in-app | Bổ sung từ Chủ đề Business Rules |

> ❌ **Không có** bảng `permissions`/`role_permissions` riêng — RBAC bán cố định (Option A): permission logic **hardcode trong code** qua Spring Security, bảng `roles` chỉ phục vụ gán & hiển thị. Chi tiết: `06-AUTHENTICATION.md`.
>
> **Ghi chú nguồn:** `categories` và `notifications` không nằm trong danh sách "13 bảng" gốc của Chủ đề Database (vì 2 bảng này được quyết định ở Chủ đề Business Rules, sau khi Chủ đề Database đã chốt). Tài liệu này hợp nhất lại thành schema đầy đủ và nhất quán — 15 bảng.

## 3. Sơ đồ quan hệ (ERD)

```
roles 1───n employees
branches 1───n departments
branches 1───n employees
departments 1───n employees (nullable — Admin/IT Staff trung tâm có thể không gắn department)
departments n───1 employees (manager_id, nullable — Manager phụ trách phòng)

employees 1───n requests (employee_id — người tạo yêu cầu)
employees 1───n requests (approved_by, nullable — Manager duyệt)
employees 1───n requests (fulfilled_by, nullable — IT Staff thực hiện)
employees 1───n refresh_tokens (1-n, hỗ trợ multi-device)
employees 1───n notifications

branches 1───n categories  ❌ KHÔNG — categories KHÔNG thuộc branch (dùng chung toàn hệ thống)
categories 1───n assets

assets n───1 branches
assets 1───1 employees (assigned_to, nullable — hiện tại đang giữ)
assets 1───n asset_images
assets 1───n asset_assignment_history
assets 1───n maintenance_records
assets 1───n audit_scans
assets 1───n discrepancies
assets 1───n requests

requests 1───1 asset_assignment_history (nullable, liên kết khi fulfill)

branches 1───n audit_sessions
audit_sessions 1───n audit_scans
audit_sessions 1───n discrepancies
```

```
┌──────────┐      ┌───────────┐      ┌─────────────┐
│  roles    │──┐   │  branches  │──┬──│ departments  │
└──────────┘  │   └───────────┘  │  └─────────────┘
              │          │        │         │
              ▼          ▼        ▼         ▼
            ┌──────────────────────────────────┐
            │            employees               │
            └──────────────────────────────────┘
              │          │              │
              ▼          ▼              ▼
       ┌────────────┐ ┌──────────┐ ┌──────────────┐
       │refresh_tokens│ │ requests │ │notifications │
       └────────────┘ └──────────┘ └──────────────┘
                          │  ▲
                          ▼  │
┌────────────┐      ┌──────────────┐
│ categories  │──┬──►│   assets      │◄──┬─────────────────────┐
└────────────┘  │   └──────────────┘    │                      │
                 │      │  │  │  │       │                      │
                 │      ▼  ▼  ▼  ▼       │                      │
          ┌──────────────┐ │ │ ┌──────────────────┐  ┌─────────────────┐
          │ asset_images  │ │ │ │maintenance_records│  │audit_scans/      │
          └──────────────┘ │ │ └──────────────────┘  │discrepancies     │
                            │ │                         └─────────────────┘
                            │ ▼                                  ▲
                       ┌──────────────────────┐                  │
                       │asset_assignment_history│                 │
                       └──────────────────────┘                  │
                                                       ┌──────────────┐
                                                       │audit_sessions │──► branches
                                                       └──────────────┘
```

## 4. Enum chính

| Enum | Giá trị | Bảng áp dụng |
|---|---|---|
| **AssetStatus** | `AVAILABLE`, `ASSIGNED`, `IN_MAINTENANCE`, `BROKEN`, `DISPOSED`, `LOST` | `assets.status` |
| **RequestType** | `ASSIGN`, `RETURN` | `requests.type` |
| **RequestStatus** | `PENDING` → `APPROVED` / `REJECTED` → `FULFILLED` / `CANCELLED` | `requests.status` |
| **MaintenanceType** | `WARRANTY`, `REPAIR`, `PERIODIC` | `maintenance_records.type` |
| **MaintenanceStatus** | `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` | `maintenance_records.status` |
| **DiscrepancyType** | `LOCATION_MISMATCH`, `MISSING`, `UNEXPECTED_FOUND` | `discrepancies.type` |
| **DiscrepancyStatus** | `OPEN`, `RESOLVED` | `discrepancies.status` |
| **AuditSessionStatus** | `IN_PROGRESS`, `COMPLETED` | `audit_sessions.status` |
| **NotificationType** | `REQUEST_CREATED`, `REQUEST_APPROVED`, `REQUEST_REJECTED`, `REQUEST_FULFILLED`, `DISCREPANCY_FOUND`, `AUDIT_REMINDER` | `notifications.type` |

> ⚠️ **`AuditSessionStatus` và `NotificationType` không được liệt kê tường minh trong nghiên cứu gốc** (chỉ có Asset/Request/Maintenance/Discrepancy Status/Type được liệt kê tường minh ở Chủ đề Database). Giá trị trên được suy luận hợp lý từ mô tả luồng nghiệp vụ (Chủ đề Business Rules — Audit Rules, Notification System). Đánh dấu **TODO: Need confirmation** nếu cần điều chỉnh tên/giá trị.

**Quy ước lưu trữ:** Enum lưu dưới dạng `VARCHAR` (không dùng PostgreSQL native `ENUM` type, để dễ migration thêm giá trị mới sau này mà không cần `ALTER TYPE`), kèm `CHECK` constraint để đảm bảo toàn vẹn dữ liệu. Phía Java dùng `@Enumerated(EnumType.STRING)`.

## 5. DDL chi tiết từng bảng

> Thứ tự bảng dưới đây tuân theo **thứ tự migration** (mục 9) — tôn trọng phụ thuộc khoá ngoại (FK dependency order). Lưu ý đặc biệt: `departments.manager_id` tham chiếu `employees`, nhưng `employees.department_id` cũng tham chiếu `departments` → **phụ thuộc vòng (circular dependency)**, xử lý bằng cách tạo `departments` trước **không kèm FK `manager_id`**, tạo `employees` sau, rồi `ALTER TABLE` bổ sung FK `manager_id` ở migration kế tiếp.

### 5.1 `roles`

```sql
CREATE TABLE roles (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,   -- ADMIN, IT_STAFF, MANAGER, EMPLOYEE
    name        VARCHAR(100) NOT NULL,           -- Tên hiển thị Tiếng Việt
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
```
> Không có `deleted_at` — seed data cố định, không expose qua API delete.

### 5.2 `branches`

```sql
CREATE TABLE branches (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(10)  NOT NULL UNIQUE,    -- VD: HN, HCM, DN
    name        VARCHAR(100) NOT NULL,
    address     VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);
```

### 5.3 `categories`

```sql
CREATE TABLE categories (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(10)  NOT NULL UNIQUE,    -- VD: LAP (Laptop), MON (Màn hình), PHN (Điện thoại)
    name        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);
```
> `code` dùng làm thành phần `<CATEGORY_CODE>` khi sinh `asset.code` (xem `07-BUSINESS-RULES.md`).

### 5.4 `departments`

```sql
CREATE TABLE departments (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    branch_id    BIGINT       NOT NULL REFERENCES branches(id),
    manager_id   BIGINT,                          -- FK bổ sung sau (xem migration V6)
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ
);
```

### 5.5 `employees`

```sql
CREATE TABLE employees (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email                 VARCHAR(255) NOT NULL UNIQUE,    -- dùng làm username đăng nhập
    password_hash         VARCHAR(255) NOT NULL,            -- BCrypt
    full_name             VARCHAR(150) NOT NULL,
    role_id               BIGINT       NOT NULL REFERENCES roles(id),
    branch_id             BIGINT       NOT NULL REFERENCES branches(id),
    department_id         BIGINT       REFERENCES departments(id),   -- nullable: Admin/IT Staff có thể không gắn phòng ban
    must_change_password  BOOLEAN      NOT NULL DEFAULT true,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at            TIMESTAMPTZ
);

-- Migration kế tiếp (V6): bổ sung FK còn thiếu trên departments
ALTER TABLE departments
    ADD CONSTRAINT fk_departments_manager
    FOREIGN KEY (manager_id) REFERENCES employees(id);
```

### 5.6 `refresh_tokens`

```sql
CREATE TABLE refresh_tokens (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id  BIGINT       NOT NULL REFERENCES employees(id),
    token_hash   VARCHAR(255) NOT NULL UNIQUE,    -- SHA-256 hash của refresh token, KHÔNG lưu plaintext
    device_label VARCHAR(255),                     -- gợi ý: User-Agent rút gọn, hỗ trợ multi-device
    expires_at   TIMESTAMPTZ  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
```
> Không có `deleted_at` — logout thực hiện **hard delete** bản ghi (đúng theo đặc tả gốc: "Xóa Refresh Token (cookie + DB record)"). Hỗ trợ **1-n theo `employee_id`** cho multi-device login.
>
> **Best practice bổ sung:** lưu `token_hash` (SHA-256) thay vì token gốc dạng plaintext — nếu DB bị lộ, kẻ tấn công không thể dùng trực tiếp giá trị trong cột để giả mạo phiên đăng nhập. Đây là khuyến nghị bảo mật chuẩn, không phải yêu cầu tường minh từ nghiên cứu gốc.

### 5.7 `assets`

```sql
CREATE TABLE assets (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code            VARCHAR(50)    NOT NULL UNIQUE,   -- VD: HN-LAP-0001, tự sinh
    name            VARCHAR(200)   NOT NULL,
    category_id     BIGINT         NOT NULL REFERENCES categories(id),
    branch_id       BIGINT         NOT NULL REFERENCES branches(id),
    assigned_to     BIGINT         REFERENCES employees(id),   -- nullable, người đang giữ
    status          VARCHAR(20)    NOT NULL DEFAULT 'AVAILABLE'
                       CHECK (status IN ('AVAILABLE','ASSIGNED','IN_MAINTENANCE','BROKEN','DISPOSED','LOST')),
    purchase_date   DATE           NOT NULL CHECK (purchase_date <= CURRENT_DATE),
    value           NUMERIC(15,2)  NOT NULL CHECK (value > 0),   -- đơn vị VNĐ
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);
```

### 5.8 `asset_images`

```sql
CREATE TABLE asset_images (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    asset_id    BIGINT       NOT NULL REFERENCES assets(id),
    url         VARCHAR(500) NOT NULL,    -- Cloudinary URL
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);
```
> Giới hạn nghiệp vụ (kiểm tra ở tầng Service, không phải DB constraint): tối đa **5 ảnh/thiết bị**, mỗi ảnh ≤ **5MB** (xem `04-API.md` mục File Upload).

### 5.9 `requests`

```sql
CREATE TABLE requests (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type               VARCHAR(10) NOT NULL CHECK (type IN ('ASSIGN','RETURN')),
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING','APPROVED','REJECTED','FULFILLED','CANCELLED')),
    asset_id           BIGINT      NOT NULL REFERENCES assets(id),
    employee_id        BIGINT      NOT NULL REFERENCES employees(id),   -- người tạo yêu cầu
    approved_by        BIGINT      REFERENCES employees(id),             -- Manager duyệt, nullable
    fulfilled_by       BIGINT      REFERENCES employees(id),             -- IT Staff thực hiện, nullable
    note               TEXT,
    rejection_reason   TEXT,                                              -- bắt buộc tại tầng Service khi status=REJECTED
    approved_at        TIMESTAMPTZ,
    fulfilled_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at         TIMESTAMPTZ
);
```

### 5.10 `asset_assignment_history`

```sql
CREATE TABLE asset_assignment_history (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    asset_id      BIGINT      NOT NULL REFERENCES assets(id),
    employee_id   BIGINT      NOT NULL REFERENCES employees(id),   -- người giữ thiết bị trong giai đoạn này
    request_id    BIGINT      REFERENCES requests(id),              -- nullable, liên kết request gây ra assignment
    assigned_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    returned_at   TIMESTAMPTZ,                                       -- NULL = đang giữ
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ
);
```

### 5.11 `maintenance_records`

```sql
CREATE TABLE maintenance_records (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    asset_id         BIGINT      NOT NULL REFERENCES assets(id),
    type             VARCHAR(20) NOT NULL CHECK (type IN ('WARRANTY','REPAIR','PERIODIC')),
    status           VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
                        CHECK (status IN ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED')),
    description      TEXT,
    scheduled_date   DATE,
    completed_date   DATE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ
);
```

### 5.12 `audit_sessions`

```sql
CREATE TABLE audit_sessions (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id     BIGINT      NOT NULL REFERENCES branches(id),   -- 1 session = phạm vi 1 chi nhánh
    status        VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
                     CHECK (status IN ('IN_PROGRESS','COMPLETED')),
    created_by    BIGINT      NOT NULL REFERENCES employees(id),
    note          TEXT,
    started_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ NOT NULL,    -- started_at + 3 ngày, dùng cho job auto-expire
    completed_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ
);
```

### 5.13 `audit_scans`

```sql
CREATE TABLE audit_scans (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    audit_session_id   BIGINT      NOT NULL REFERENCES audit_sessions(id),
    asset_id           BIGINT      NOT NULL REFERENCES assets(id),
    scanned_by         BIGINT      NOT NULL REFERENCES employees(id),
    scanned_location   VARCHAR(255),
    scanned_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at         TIMESTAMPTZ
);
```

### 5.14 `discrepancies`

```sql
CREATE TABLE discrepancies (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    audit_session_id    BIGINT      NOT NULL REFERENCES audit_sessions(id),
    asset_id            BIGINT      NOT NULL REFERENCES assets(id),
    type                VARCHAR(20) NOT NULL
                           CHECK (type IN ('LOCATION_MISMATCH','MISSING','UNEXPECTED_FOUND')),
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                           CHECK (status IN ('OPEN','RESOLVED')),
    expected_location   VARCHAR(255),
    actual_location     VARCHAR(255),
    resolution_action   VARCHAR(20) CHECK (resolution_action IN ('CONFIRM_LOST','FOUND')),
    resolved_by         BIGINT      REFERENCES employees(id),
    resolved_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ
);
```

### 5.15 `notifications`

```sql
CREATE TABLE notifications (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id         BIGINT       NOT NULL REFERENCES employees(id),
    type                VARCHAR(30)  NOT NULL,
    message             VARCHAR(500) NOT NULL,
    is_read             BOOLEAN      NOT NULL DEFAULT false,
    related_entity_id   BIGINT,         -- VD: request_id, discrepancy_id liên quan
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ
);
```

## 6. Business Logic tự động

> ⚠️ **Lưu ý quan trọng:** Mục này mô tả logic **tự động về mặt nghiệp vụ** (tự động xảy ra như một phần của workflow), KHÔNG phải triển khai bằng PostgreSQL Trigger/Stored Procedure. Toàn bộ logic được triển khai tại **tầng Service** (Spring Boot), nhất quán với kiến trúc Layered Architecture đã chọn (`01-ARCHITECTURE.md`) — giúp dễ test (Unit Test mock Repository), dễ trace, dễ debug so với logic ẩn trong DB trigger.

- **`asset_assignment_history`**: `RequestServiceImpl.fulfill()` tự tạo 1 dòng mới khi `request[type=ASSIGN]` được fulfill (`returned_at = NULL`); tự cập nhật `returned_at = now()` của dòng tương ứng khi `request[type=RETURN]` được fulfill.
- **Request `RETURN`**: Employee chỉ được chọn từ thiết bị mình đang giữ (`asset.assigned_to = employee_id`, validate tại Service khi tạo request); khi fulfill → `asset.status` chuyển về `AVAILABLE`, `asset.assigned_to = NULL`.
- **Maintenance `IN_PROGRESS`**: khi `MaintenanceService` cập nhật `maintenance_records.status = IN_PROGRESS`, đồng thời tự động set `asset.status = IN_MAINTENANCE` (giữ nguyên `assigned_to`).
- **Audit complete**: `AuditService.complete()` so sánh danh sách asset thuộc chi nhánh với danh sách đã quét trong session → asset nào chưa quét tự động tạo `discrepancy(type=MISSING, status=OPEN)`.
- **Audit auto-expire**: `@Scheduled` job quét `audit_sessions` có `status=IN_PROGRESS AND expires_at < now()` → gọi lại đúng logic `complete()` ở trên (tự động complete + tự tạo discrepancy cho asset chưa quét).

## 7. Soft Delete & FK Behavior

- **Toàn bộ bảng nghiệp vụ** có cột `deleted_at` (kế thừa `BaseEntity`), **trừ** `roles` (seed cố định) và `refresh_tokens` (hard delete theo đặc tả gốc).
- **Không cascade soft-delete tự động**: xoá `branch` **không** tự động xoá `employees`/`assets` bên trong (an toàn, tránh xoá nhầm hàng loạt). Tầng Service nên **chặn** soft-delete 1 `branch` nếu vẫn còn `employees`/`assets` active liên kết (gợi ý best practice bổ sung, không có trong đặc tả gốc — đánh dấu để Tech Lead xác nhận).
- FK declare ở mức DB (`REFERENCES`) nhưng **không** dùng `ON DELETE CASCADE` cho bất kỳ quan hệ nào — nhất quán với nguyên tắc soft-delete không cascade.

## 8. Index Strategy

| Bảng | Cột được index |
|---|---|
| `assets` | `code` (unique, đã có qua constraint), `status`, `branch_id`, `assigned_to`, `deleted_at` |
| `employees` | `email` (unique, đã có qua constraint), `branch_id`, `department_id`, `deleted_at` |
| `requests` | `employee_id`, `status`, `asset_id` |
| `audit_scans` | `audit_session_id`, `asset_id` |
| `refresh_tokens` | `employee_id`, `token_hash` (unique, đã có qua constraint) |

```sql
CREATE INDEX idx_assets_status      ON assets(status);
CREATE INDEX idx_assets_branch_id   ON assets(branch_id);
CREATE INDEX idx_assets_assigned_to ON assets(assigned_to);
CREATE INDEX idx_assets_deleted_at  ON assets(deleted_at);

CREATE INDEX idx_employees_branch_id     ON employees(branch_id);
CREATE INDEX idx_employees_department_id ON employees(department_id);
CREATE INDEX idx_employees_deleted_at    ON employees(deleted_at);

CREATE INDEX idx_requests_employee_id ON requests(employee_id);
CREATE INDEX idx_requests_status      ON requests(status);
CREATE INDEX idx_requests_asset_id    ON requests(asset_id);

CREATE INDEX idx_audit_scans_session_id ON audit_scans(audit_session_id);
CREATE INDEX idx_audit_scans_asset_id   ON audit_scans(asset_id);

CREATE INDEX idx_refresh_tokens_employee_id ON refresh_tokens(employee_id);
```

## 9. Migration Strategy (Flyway)

| File | Nội dung |
|---|---|
| `V1__init_roles.sql` | Tạo bảng `roles` |
| `V2__init_branches.sql` | Tạo bảng `branches` |
| `V3__init_categories.sql` | Tạo bảng `categories` |
| `V4__init_departments.sql` | Tạo bảng `departments` (chưa có FK `manager_id`) |
| `V5__init_employees.sql` | Tạo bảng `employees` |
| `V6__alter_departments_add_manager_fk.sql` | `ALTER TABLE departments ADD CONSTRAINT fk_departments_manager...` |
| `V7__init_refresh_tokens.sql` | Tạo bảng `refresh_tokens` |
| `V8__init_assets.sql` | Tạo bảng `assets` |
| `V9__init_asset_images.sql` | Tạo bảng `asset_images` |
| `V10__init_requests.sql` | Tạo bảng `requests` |
| `V11__init_asset_assignment_history.sql` | Tạo bảng `asset_assignment_history` |
| `V12__init_maintenance_records.sql` | Tạo bảng `maintenance_records` |
| `V13__init_audit_sessions.sql` | Tạo bảng `audit_sessions` |
| `V14__init_audit_scans.sql` | Tạo bảng `audit_scans` |
| `V15__init_discrepancies.sql` | Tạo bảng `discrepancies` |
| `V16__init_notifications.sql` | Tạo bảng `notifications` |
| `V17__create_indexes.sql` | Toàn bộ `CREATE INDEX` (mục 8) |
| `V18__seed_roles.sql` | Seed 4 role cố định |
| `V19__seed_demo_data.sql` | Seed Admin mặc định + data mẫu demo |

> Testcontainers (xem `10-TESTING.md`) tự động chạy **toàn bộ chuỗi migration thật** này khi Integration Test khởi động, đảm bảo schema test luôn đồng bộ với production.

## 10. Seed Data

| Hạng mục | Nội dung |
|---|---|
| Roles | 4 role cố định: `ADMIN`, `IT_STAFF`, `MANAGER`, `EMPLOYEE` |
| Admin mặc định | `admin@itam.local`, mật khẩu tạm (random, hash BCrypt khi seed), `must_change_password = true` |
| Data demo | 1 branch mẫu (VD: `HN` - Chi nhánh Hà Nội), vài `departments`, vài `employees` mẫu (đủ 4 role), vài `assets` mẫu (đủ các trạng thái: `AVAILABLE`, `ASSIGNED`, `IN_MAINTENANCE`, `BROKEN`) |

**Ví dụ `V18__seed_roles.sql`:**

```sql
INSERT INTO roles (code, name) VALUES
    ('ADMIN', 'Quản trị viên'),
    ('IT_STAFF', 'Nhân viên IT chi nhánh'),
    ('MANAGER', 'Trưởng phòng'),
    ('EMPLOYEE', 'Nhân viên');
```

**Ví dụ trích đoạn `V19__seed_demo_data.sql`:**

```sql
INSERT INTO branches (code, name, address) VALUES
    ('HN', 'Chi nhánh Hà Nội', '123 Đường ABC, Hà Nội');

INSERT INTO categories (code, name) VALUES
    ('LAP', 'Laptop'), ('MON', 'Màn hình'), ('PHN', 'Điện thoại');

-- Admin mặc định — mật khẩu tạm đã hash sẵn (ví dụ minh hoạ, giá trị hash thật sinh khi chạy seed)
INSERT INTO employees (email, password_hash, full_name, role_id, branch_id, must_change_password)
SELECT 'admin@itam.local', '$2a$10$examplebcrypthash...', 'Quản trị hệ thống',
       (SELECT id FROM roles WHERE code = 'ADMIN'),
       (SELECT id FROM branches WHERE code = 'HN'), true;
```

→ Mục đích: **demo ngay sau deploy** mà không cần nhập liệu thủ công.

## 11. TODO / Open Questions

> TODO: Need confirmation — **Kiểu Primary Key**: tài liệu này mặc định `BIGINT GENERATED ALWAYS AS IDENTITY` vì đơn giản, hiệu năng tốt, đủ dùng cho quy mô 200-300 thiết bị / 50-100 nhân viên, và phù hợp tự nhiên với pattern `JpaRepository<Entity, Long>`. **Phương án thay thế:** `UUID` (qua `gen_random_uuid()`), ưu điểm tránh lộ thông tin số lượng record qua URL công khai (VD: `/api/v1/employees/5` lộ rằng công ty có ít nhất 5 nhân viên) — cần Tech Lead xác nhận nếu muốn đổi.

> TODO: Need confirmation — `AuditSessionStatus` (`IN_PROGRESS`/`COMPLETED`) và `NotificationType` (danh sách 6 giá trị) là **suy luận hợp lý**, không được liệt kê tường minh trong nghiên cứu gốc như các enum khác.

> TODO: Need confirmation — Việc **chặn soft-delete `branch`** khi còn `employees`/`assets` active là đề xuất best practice bổ sung, chưa được xác nhận trong nghiên cứu gốc.

---

*Xem tiếp: `06-AUTHENTICATION.md` để biết chi tiết JWT, RBAC và Authorization Matrix.*