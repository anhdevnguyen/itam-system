# 04 — API

> Đặc tả đầy đủ REST API của hệ thống ITAM: versioning, pagination, danh sách resource, request/response mẫu cho từng nhóm endpoint.

## Mục lục

1. [Versioning & Base URL](#1-versioning--base-url)
2. [Pagination, Sorting, Filtering](#2-pagination-sorting-filtering)
3. [Danh sách Resource](#3-danh-sách-resource)
4. [Authentication Endpoints](#4-authentication-endpoints)
5. [CRUD Pattern chuẩn](#5-crud-pattern-chuẩn)
6. [Soft Delete Endpoints](#6-soft-delete-endpoints)
7. [Resource: Employees](#7-resource-employees)
8. [Resource: Branches](#8-resource-branches)
9. [Resource: Departments](#9-resource-departments)
10. [Resource: Categories](#10-resource-categories)
11. [Resource: Assets](#11-resource-assets)
12. [Resource: Requests (Workflow)](#12-resource-requests-workflow)
13. [Resource: Maintenance](#13-resource-maintenance)
14. [Resource: Audits & Discrepancies](#14-resource-audits--discrepancies)
15. [Resource: Notifications](#15-resource-notifications)
16. [File Upload](#16-file-upload)
17. [Rate Limiting](#17-rate-limiting)
18. [TODO / Open Questions](#18-todo--open-questions)

---

## 1. Versioning & Base URL

| Hạng mục | Giá trị |
|---|---|
| Chiến lược versioning | **URL path versioning** |
| Pattern | `/api/v1/{resource}` |
| Base URL (Production, ví dụ) | `https://<render-domain>/api/v1/...` |

→ Toàn bộ response wrap trong `ApiResponse` (`success/data/errors/meta`) — chi tiết format tại `01-ARCHITECTURE.md` mục 7.

## 2. Pagination, Sorting, Filtering

**Pagination — Page-based:**

```
GET /api/v1/assets?page=0&size=20&sort=createdAt,desc
```

| Param | Mặc định | Ghi chú |
|---|---|---|
| `page` | `0` | 0-indexed |
| `size` | `20` | Số record/trang |
| `sort` | `createdAt,desc` | Hỗ trợ multi-sort: `?sort=status,asc&sort=createdAt,desc` |

**Filtering — trực tiếp theo field:**

```
GET /api/v1/assets?status=ASSIGNED&branchId=1&category=LAPTOP
```

Mỗi resource định nghĩa tập field filter hợp lệ riêng (xem từng mục resource bên dưới).

**Response pagination** nằm trong `meta.pagination`:

```json
{
  "success": true,
  "data": [ /* mảng kết quả trang hiện tại */ ],
  "meta": {
    "timestamp": "2026-06-30T10:00:00Z",
    "pagination": { "page": 0, "size": 20, "totalElements": 150, "totalPages": 8 }
  }
}
```

## 3. Danh sách Resource

```
/api/v1/auth
/api/v1/employees
/api/v1/branches
/api/v1/departments
/api/v1/categories
/api/v1/assets
/api/v1/requests
/api/v1/maintenance
/api/v1/audits
/api/v1/notifications
```

> **Ghi chú:** `departments` và `categories` là 2 resource cần CRUD đầy đủ nhưng không nằm trong danh sách "7 resource chính" ban đầu của nghiên cứu gốc (do được xác nhận là bảng độc lập ở các chủ đề sau — `departments` ở Chủ đề Database, `categories` ở Chủ đề Business Rules). Tài liệu này bổ sung 2 resource trên theo **đúng CRUD Pattern chuẩn** (mục 5) để đảm bảo tính nhất quán và đầy đủ của API, vì cả hai đều cần thao tác tạo/sửa/xoá độc lập (Admin quản lý `categories`; Admin/IT Staff quản lý `departments` theo chi nhánh).

## 4. Authentication Endpoints

| Endpoint | Method | Mô tả |
|---|---|---|
| `/api/v1/auth/login` | `POST` | `{email, password}` → Access Token (body) + Refresh Token (httpOnly cookie) |
| `/api/v1/auth/refresh` | `POST` | Đọc Refresh Token từ cookie → Access Token mới |
| `/api/v1/auth/logout` | `POST` | Xóa Refresh Token (cookie + DB record) |
| `/api/v1/employees/{id}/reset-password` | `POST` | **Chỉ Admin gọi** — tự sinh mật khẩu tạm thời ngẫu nhiên → trả về cho Admin → nhân viên bắt buộc đổi mật khẩu ở lần đăng nhập đầu (`mustChangePassword = true`) |

> ❌ **Không có** `POST /auth/register` và **không có** `forgot-password` tự động qua email. Tài khoản **chỉ Admin/IT Staff tạo thủ công** qua `POST /api/v1/employees`.

**`POST /api/v1/auth/login`**

Request:
```json
{
  "email": "it.staff@itam.local",
  "password": "Password123"
}
```

Response `200 OK`:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOi...",
    "expiresIn": 1800,
    "user": {
      "id": 5,
      "email": "it.staff@itam.local",
      "fullName": "Nguyễn Văn A",
      "role": "IT_STAFF",
      "branchId": 1,
      "mustChangePassword": false
    }
  },
  "meta": { "timestamp": "2026-06-30T10:00:00Z" }
}
```
> `Set-Cookie: refreshToken=...; HttpOnly; Secure; SameSite=Lax; Max-Age=2592000` được gửi kèm header response (không xuất hiện trong body).

Response `401 Unauthorized` (sai email/password):
```json
{
  "success": false,
  "errors": [{ "code": "AUTH_INVALID_CREDENTIALS", "message": "Email hoặc mật khẩu không đúng" }],
  "meta": { "timestamp": "2026-06-30T10:00:00Z" }
}
```

**`POST /api/v1/auth/refresh`** — không cần body, đọc cookie tự động.

Response `200 OK`:
```json
{
  "success": true,
  "data": { "accessToken": "eyJhbGciOi...", "expiresIn": 1800 },
  "meta": { "timestamp": "2026-06-30T10:00:00Z" }
}
```

**`POST /api/v1/employees/{id}/reset-password`**

Response `200 OK`:
```json
{
  "success": true,
  "data": { "temporaryPassword": "Tmp7xKq2" },
  "meta": { "timestamp": "2026-06-30T10:00:00Z" }
}
```
> Mật khẩu tạm cũng được gửi qua **Email** tới nhân viên (xem `07-BUSINESS-RULES.md` mục Notification System). `temporaryPassword` trả trong response để Admin có thể đọc/copy trực tiếp.

## 5. CRUD Pattern chuẩn

Áp dụng **đồng nhất cho mọi resource** (trừ các endpoint workflow đặc thù ở mục 12, 14):

```
GET    /api/v1/{resource}          - danh sách (pagination/filter/sort)
GET    /api/v1/{resource}/{id}     - chi tiết
POST   /api/v1/{resource}          - tạo mới
PUT    /api/v1/{resource}/{id}     - cập nhật toàn bộ
DELETE /api/v1/{resource}/{id}     - soft delete
```

**Ví dụ Response chi tiết (`GET /api/v1/assets/{id}`):**

```json
{
  "success": true,
  "data": {
    "id": 42,
    "code": "HN-LAP-0001",
    "name": "Dell Latitude 5440",
    "categoryId": 1,
    "categoryName": "Laptop",
    "branchId": 1,
    "status": "ASSIGNED",
    "assignedTo": 5,
    "assignedToName": "Nguyễn Văn A",
    "purchaseDate": "2025-03-10",
    "value": 22000000,
    "createdAt": "2025-03-10T08:00:00Z",
    "updatedAt": "2026-01-15T09:30:00Z"
  },
  "meta": { "timestamp": "2026-06-30T10:00:00Z" }
}
```

## 6. Soft Delete Endpoints

Áp dụng cho **toàn bộ resource** (cột `deletedAt`, kế thừa `BaseEntity` — xem `05-DATABASE.md`):

| Endpoint | Method | Mô tả |
|---|---|---|
| `/api/v1/{resource}/{id}` | `DELETE` | Soft delete — set `deletedAt = now()` |
| `/api/v1/{resource}?includeDeleted=true` | `GET` | Xem cả record đã xoá trong danh sách |
| `/api/v1/{resource}/{id}/restore` | `POST` | Khôi phục record đã xoá (`deletedAt = null`) |

Mặc định, `GET` danh sách **tự động loại trừ** record đã xoá (`WHERE deleted_at IS NULL` ngầm định ở tầng Repository/Service).

> Ghi chú: `roles` (seed data cố định) và `refresh_tokens`/`audit_scans` (bảng log/lưu vết) **không áp dụng** soft delete pattern này — xem lý do tại `05-DATABASE.md` mục Soft Delete & FK Behavior.

## 7. Resource: Employees

```
GET    /api/v1/employees                     - danh sách (filter: branchId, departmentId, role)
GET    /api/v1/employees/{id}                - chi tiết
POST   /api/v1/employees                     - tạo mới (mật khẩu tự sinh, mustChangePassword=true)
PUT    /api/v1/employees/{id}                - cập nhật
DELETE /api/v1/employees/{id}                - soft delete (kèm cảnh báo nếu đang giữ thiết bị — 07-BUSINESS-RULES.md)
POST   /api/v1/employees/{id}/reset-password - reset mật khẩu (chỉ Admin)
GET    /api/v1/employees/me                  - thông tin tài khoản đang đăng nhập
PUT    /api/v1/employees/me                  - tự cập nhật hồ sơ cá nhân (Employee)
```

**`POST /api/v1/employees`** Request:
```json
{
  "fullName": "Trần Thị B",
  "email": "tranthib@company.com",
  "roleId": 4,
  "branchId": 1,
  "departmentId": 2
}
```
Response `201 Created` — trả `temporaryPassword` tương tự reset-password.

## 8. Resource: Branches

```
GET    /api/v1/branches            - danh sách
GET    /api/v1/branches/{id}       - chi tiết
POST   /api/v1/branches            - tạo mới (chỉ Admin)
PUT    /api/v1/branches/{id}       - cập nhật (chỉ Admin)
DELETE /api/v1/branches/{id}       - soft delete (chỉ Admin, KHÔNG cascade xoá employees/assets bên trong)
```

Request `POST /api/v1/branches`:
```json
{ "code": "HN", "name": "Chi nhánh Hà Nội", "address": "..." }
```
> `code` bắt buộc, unique (xem `07-BUSINESS-RULES.md` mục Validation Rules).

## 9. Resource: Departments

```
GET    /api/v1/departments                - danh sách (filter: branchId)
GET    /api/v1/departments/{id}            - chi tiết
POST   /api/v1/departments                 - tạo mới
PUT    /api/v1/departments/{id}            - cập nhật (gán/đổi Manager phụ trách)
DELETE /api/v1/departments/{id}            - soft delete
```

Request `POST /api/v1/departments`:
```json
{ "name": "Phòng Kỹ thuật", "branchId": 1, "managerId": 8 }
```

## 10. Resource: Categories

```
GET    /api/v1/categories          - danh sách (dùng cho dropdown khi tạo Asset)
GET    /api/v1/categories/{id}     - chi tiết
POST   /api/v1/categories          - tạo mới (chỉ Admin)
PUT    /api/v1/categories/{id}     - cập nhật
DELETE /api/v1/categories/{id}     - soft delete
```

> `categories` tách bảng riêng (không hardcode enum) để linh hoạt mở rộng — bao gồm cả License phần mềm ở Phase 2 (xem `00-OVERVIEW.md`).

## 11. Resource: Assets

```
GET    /api/v1/assets                          - danh sách (filter: status, branchId, category, assignedTo)
GET    /api/v1/assets/{id}                     - chi tiết
POST   /api/v1/assets                          - tạo mới (auto-generate code, xem 07-BUSINESS-RULES.md)
PUT    /api/v1/assets/{id}                     - cập nhật
DELETE /api/v1/assets/{id}                     - soft delete
GET    /api/v1/assets/{id}/qr-code             - lấy ảnh QR code đã sinh cho thiết bị
GET    /api/v1/assets/{id}/assignment-history  - lịch sử cấp phát của thiết bị
POST   /api/v1/assets/{id}/force-return        - IT Staff chủ động thu hồi (bỏ qua workflow, bắt buộc kèm lý do)
POST   /api/v1/assets/{id}/images              - upload ảnh (xem mục 16)
```

Request `POST /api/v1/assets`:
```json
{
  "name": "Dell Latitude 5440",
  "categoryId": 1,
  "branchId": 1,
  "purchaseDate": "2025-03-10",
  "value": 22000000
}
```
> `code` **không** nằm trong request — hệ thống tự sinh theo format `<BRANCH_CODE>-<CATEGORY_CODE>-<SEQUENCE>` (VD: `HN-LAP-0001`).

Request `POST /api/v1/assets/{id}/force-return`:
```json
{ "reason": "Nhân viên nghỉ việc, thu hồi gấp" }
```

## 12. Resource: Requests (Workflow)

```
POST /api/v1/requests                  - Employee tạo yêu cầu (ASSIGN hoặc RETURN)
GET  /api/v1/requests                  - danh sách (filter: status, employeeId, branchId)
GET  /api/v1/requests/{id}             - chi tiết
POST /api/v1/requests/{id}/approve     - Manager duyệt
POST /api/v1/requests/{id}/reject      - Manager từ chối (kèm lý do, bắt buộc)
POST /api/v1/requests/{id}/fulfill     - IT Staff hoàn tất cấp phát/thu hồi
POST /api/v1/requests/{id}/cancel      - Employee tự hủy (chỉ khi chưa duyệt, status=PENDING)
```

> Không có `PUT` chuẩn cho `requests` — trạng thái (`status`) chỉ thay đổi qua các action endpoint trên (workflow state machine), không cho phép sửa field tự do. `DELETE /api/v1/requests/{id}` (soft delete) và `POST /api/v1/requests/{id}/restore` **vẫn áp dụng** theo đúng quy tắc soft delete chung (mục 6) — dùng cho trường hợp Admin cần dọn dẹp request tạo nhầm, không phải một bước trong workflow nghiệp vụ thông thường. Chi tiết state machine: `07-BUSINESS-RULES.md`.

Request `POST /api/v1/requests`:
```json
{ "type": "ASSIGN", "assetId": 42, "note": "Cần laptop để làm việc" }
```

Request `POST /api/v1/requests/{id}/reject`:
```json
{ "rejectionReason": "Thiết bị đã được phân bổ cho dự án khác" }
```

Response `409 Conflict` (request đã xử lý — VD: duyệt 2 lần):
```json
{
  "success": false,
  "errors": [{ "code": "REQUEST_ALREADY_PROCESSED", "message": "Yêu cầu đã được xử lý trước đó" }],
  "meta": { "timestamp": "2026-06-30T10:00:00Z" }
}
```

## 13. Resource: Maintenance

```
GET    /api/v1/maintenance             - danh sách (filter: assetId, status, type)
GET    /api/v1/maintenance/{id}        - chi tiết
POST   /api/v1/maintenance             - tạo bản ghi bảo trì mới
PUT    /api/v1/maintenance/{id}        - cập nhật (VD: chuyển status)
DELETE /api/v1/maintenance/{id}        - soft delete
```

Request `POST /api/v1/maintenance`:
```json
{
  "assetId": 42,
  "type": "REPAIR",
  "description": "Màn hình bị nhiễu",
  "scheduledDate": "2026-07-05"
}
```
> Khi `status` chuyển sang `IN_PROGRESS` (qua `PUT`), `asset.status` **tự động đồng bộ** sang `IN_MAINTENANCE` (business logic phía Service, xem `07-BUSINESS-RULES.md`).

## 14. Resource: Audits & Discrepancies

```
POST /api/v1/audits                              - Tạo phiên kiểm kê mới (phạm vi 1 chi nhánh)
GET  /api/v1/audits                              - danh sách phiên kiểm kê
GET  /api/v1/audits/{id}                         - chi tiết phiên (kèm tiến độ quét)
POST /api/v1/audits/{id}/scan                    - Ghi nhận 1 lần quét QR
POST /api/v1/audits/{id}/complete                - Hoàn tất phiên → tự tạo discrepancy report
GET  /api/v1/audits/{id}/discrepancies           - Xem danh sách sai lệch của phiên
POST /api/v1/audits/discrepancies/{id}/resolve   - Xử lý xong 1 discrepancy
```

Request `POST /api/v1/audits`:
```json
{ "branchId": 1, "note": "Kiểm kê quý 3/2026" }
```

Request `POST /api/v1/audits/{id}/scan`:
```json
{ "assetCode": "HN-LAP-0001", "scannedLocation": "Tầng 3 - Phòng Kỹ thuật" }
```

Request `POST /api/v1/audits/discrepancies/{id}/resolve`:
```json
{ "action": "CONFIRM_LOST" }
```
> `action` nhận giá trị `CONFIRM_LOST` (asset status → `LOST`) hoặc `FOUND` (giữ nguyên status cũ) — chỉ áp dụng cho discrepancy `type=MISSING`. Xem chi tiết tại `07-BUSINESS-RULES.md` mục Audit Rules.

## 15. Resource: Notifications

```
GET  /api/v1/notifications                  - danh sách thông báo của user đang đăng nhập
GET  /api/v1/notifications/unread-count     - đếm số thông báo chưa đọc (FE polling 30-60s)
POST /api/v1/notifications/{id}/read        - đánh dấu đã đọc 1 thông báo
```

> Cơ chế **polling đơn giản** — không dùng WebSocket. Xem `07-BUSINESS-RULES.md` mục Notification System.

## 16. File Upload

```
POST /api/v1/assets/{id}/images   - upload ảnh (multipart/form-data)
```

| Giới hạn | Giá trị |
|---|---|
| Kích thước tối đa/ảnh | 5MB |
| Số lượng ảnh tối đa/thiết bị | 5 ảnh |
| Lưu trữ | Cloudinary |

```bash
curl -X POST https://<render-domain>/api/v1/assets/42/images \
  -H "Authorization: Bearer <token>" \
  -F "file=@invoice.jpg"
```

Response `201 Created`:
```json
{
  "success": true,
  "data": { "id": 7, "assetId": 42, "url": "https://res.cloudinary.com/.../invoice.jpg" },
  "meta": { "timestamp": "2026-06-30T10:00:00Z" }
}
```

## 17. Rate Limiting

- **Bảo vệ cơ bản** áp dụng cho endpoint nhạy cảm: `POST /api/v1/auth/login` giới hạn **~5 lần/phút/IP** chống brute-force.
- **Không** áp dụng rate limit phức tạp (theo user, theo tier...) cho toàn hệ thống ở MVP.
- Response khi vượt giới hạn: `429 Too Many Requests` (gợi ý mã lỗi `AUTH_RATE_LIMIT_EXCEEDED` — xem `09-ERROR-CODES.md`).

## 18. TODO / Open Questions

> TODO: Need confirmation — `departments` và `categories` được bổ sung vào danh sách resource API dựa trên suy luận hợp lý từ thiết kế Database/Business Rules (2 chủ đề được chốt sau Chủ đề API gốc). Cần Tech Lead xác nhận lại endpoint pattern cho 2 resource này là chính xác như mong muốn.

> TODO: Need confirmation — Cơ chế rate limiting cụ thể (thư viện sử dụng: Bucket4j, Resilience4j, hay cấu hình tại tầng Render/Cloudflare) chưa được đặc tả kỹ thuật chi tiết trong nghiên cứu gốc, hiện chỉ xác nhận **yêu cầu nghiệp vụ** (giới hạn ~5 lần/phút cho login).

---

*Xem tiếp: `05-DATABASE.md` để biết schema database đầy đủ.*