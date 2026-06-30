# 06 — AUTHENTICATION & AUTHORIZATION

> Đặc tả đầy đủ cơ chế xác thực (Authentication) và phân quyền (Authorization) của hệ thống ITAM: JWT flow, RBAC, Authorization Matrix chi tiết, Password Policy, Session/Device Management.

## Mục lục

1. [Tổng quan phương án](#1-tổng-quan-phương-án)
2. [JWT — Cấu trúc & Vòng đời Token](#2-jwt--cấu-trúc--vòng-đời-token)
3. [Password Policy](#3-password-policy)
4. [RBAC — Mô hình phân quyền](#4-rbac--mô-hình-phân-quyền)
5. [Authorization Matrix chi tiết](#5-authorization-matrix-chi-tiết)
6. [Admin Override — Định nghĩa kỹ thuật](#6-admin-override--định-nghĩa-kỹ-thuật)
7. [Session / Device Management](#7-session--device-management)
8. [Spring Security Implementation Pattern](#8-spring-security-implementation-pattern)
9. [Security Rules bổ sung](#9-security-rules-bổ-sung)
10. [Luồng đăng nhập đầu tiên & đổi mật khẩu bắt buộc](#10-luồng-đăng-nhập-đầu-tiên--đổi-mật-khẩu-bắt-buộc)
11. [TODO / Open Questions](#11-todo--open-questions)

---

## 1. Tổng quan phương án

| Hạng mục | Lựa chọn |
|---|---|
| Phương thức xác thực | **JWT tự xây dựng** (KHÔNG dùng SSO/OAuth bên thứ 3) |
| Kiểu JWT | Stateless cho Access Token, kết hợp lưu DB cho Refresh Token (hybrid — hỗ trợ revoke) |
| Tài khoản | Chỉ tạo thủ công qua Admin/IT Staff (`POST /api/v1/employees`) — **không có** `register` công khai |
| Phân quyền | **RBAC bán cố định** (Option A) — permission logic hardcode trong code qua Spring Security, **không có** bảng `permissions`/`role_permissions` riêng |

> Lý do chọn RBAC bán cố định thay vì RBAC động (bảng `permissions` + `role_permissions`): hệ thống chỉ có **4 role cố định**, không có nhu cầu admin tự định nghĩa role/permission mới qua UI. RBAC động sẽ làm tăng độ phức tạp không cần thiết ở quy mô MVP — nhất quán với nguyên tắc "tránh phức tạp hoá sớm" đã áp dụng xuyên suốt (`01-ARCHITECTURE.md` mục 11).

→ Chi tiết luồng login/refresh đầy đủ kèm sequence diagram: `01-ARCHITECTURE.md` mục 5. Tài liệu này tập trung vào **chi tiết RBAC, Authorization Matrix, Password Policy** chưa được trình bày sâu ở đó.

## 2. JWT — Cấu trúc & Vòng đời Token

### 2.1 Access Token

| Thuộc tính | Giá trị |
|---|---|
| Thời hạn | **30 phút** |
| Nơi lưu (Frontend) | In-memory (React state) — **KHÔNG** localStorage/sessionStorage |
| Truyền tải | Header `Authorization: Bearer <token>` |
| Thuật toán ký | `HS256` (HMAC, secret key đối xứng — đơn giản, phù hợp Monolith) |

**Claims đề xuất trong payload:**

```json
{
  "sub": "5",
  "email": "it.staff@itam.local",
  "role": "IT_STAFF",
  "branchId": 1,
  "iat": 1751270400,
  "exp": 1751272200
}
```

> `sub` = `employee.id` (dạng string theo chuẩn JWT). `role` và `branchId` được nhúng trực tiếp vào claims để Spring Security có thể authorize **mà không cần query DB ở mỗi request** — đánh đổi: nếu role/branch của nhân viên bị đổi giữa chừng, thay đổi chỉ có hiệu lực sau khi Access Token hết hạn (tối đa 30 phút) hoặc sau khi Refresh Token được dùng để cấp Access Token mới. Đây là đánh đổi chấp nhận được ở quy mô MVP.

### 2.2 Refresh Token

| Thuộc tính | Giá trị |
|---|---|
| Thời hạn | **30 ngày** |
| Nơi lưu (Frontend) | httpOnly cookie (`Secure`, `SameSite=Lax`) |
| Nơi lưu (Backend) | Bảng `refresh_tokens` (PostgreSQL) — chỉ lưu **hash** (SHA-256), không lưu plaintext |
| Giá trị thật | Random string đủ entropy (VD: UUID v4 hoặc 256-bit random, KHÔNG phải JWT có thể tự giải mã) |

> **Tại sao Refresh Token không phải JWT?** Refresh Token chỉ đóng vai trò "khoá tra cứu" (lookup key) vào bảng `refresh_tokens` — bản chất là **opaque token**. Việc này cho phép revoke tức thì (xoá record DB) mà không cần cơ chế blacklist JWT phức tạp (vốn chỉ cần thiết nếu Refresh Token cũng là JWT tự chứa thông tin).

### 2.3 Vòng đời & Revoke

```
┌─────────────┐   login    ┌──────────────────┐
│  Chưa đăng nhập │ ─────────► │ Access + Refresh   │
└─────────────┘            │ Token được cấp      │
                            └─────────┬─────────┘
                                      │
              ┌───────────────────────┼───────────────────────┐
              ▼                       ▼                       ▼
      ┌──────────────┐      ┌──────────────────┐    ┌──────────────────┐
      │ Access Token   │      │ /auth/refresh       │    │ /auth/logout       │
      │ hết hạn (30p)  │      │ (dùng Refresh Token) │    │ (revoke thủ công)   │
      └──────┬───────┘      └─────────┬──────────┘    └─────────┬──────────┘
             │                         │                          │
             ▼                         ▼                          ▼
      FE tự động gọi          Access Token mới được cấp     DELETE record trong
      /auth/refresh            (Refresh Token GIỮ NGUYÊN,    refresh_tokens (hard
                                không rotate — xem TODO       delete) + xoá cookie
                                mục 11)
```

**Các trường hợp Refresh Token bị revoke (xoá khỏi DB):**

| Trường hợp | Hành động |
|---|---|
| Employee chủ động logout | Xoá **đúng 1** record (refresh token của thiết bị hiện tại) |
| Admin reset password cho employee | Xoá **toàn bộ** record của employee đó (buộc đăng nhập lại trên mọi thiết bị) |
| Employee tự đổi mật khẩu | Xoá **toàn bộ** record của employee đó, **trừ** thiết bị đang thực hiện đổi mật khẩu (giữ phiên hiện tại) |
| Employee bị soft-delete (nghỉ việc) | Xoá **toàn bộ** record của employee đó ngay lập tức |
| Refresh Token hết hạn tự nhiên (30 ngày) | Không cần xoá chủ động — `expires_at < now()` khiến token bị từ chối ở `/auth/refresh`; dọn dẹp định kỳ qua job (xem `11-DEPLOYMENT.md`) |

## 3. Password Policy

| Quy tắc | Giá trị |
|---|---|
| Độ dài tối thiểu | **8 ký tự** |
| Bắt buộc chứa | Chữ hoa (A-Z) + chữ thường (a-z) + số (0-9) |
| Ký tự đặc biệt | **Không bắt buộc** (khuyến khích nhưng không validate) |
| Thuật toán hash | **BCrypt** (cost factor mặc định Spring Security: 10) |
| Lưu trữ | Cột `employees.password_hash` — **không bao giờ** trả về plaintext hay hash qua API |

**Regex validation tham khảo (Backend, Jakarta Bean Validation custom annotation hoặc kiểm tra thủ công trong Service):**

```java
// Tối thiểu 8 ký tự, ít nhất 1 hoa, 1 thường, 1 số
private static final String PASSWORD_PATTERN =
    "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$";
```

**Validation message (Tiếng Việt, theo `03-CODING-STANDARDS.md` mục 6):**

```java
if (!password.matches(PASSWORD_PATTERN)) {
    throw new ValidationException(
        "AUTH_PASSWORD_POLICY_VIOLATION",
        "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số"
    );
}
```

### Mật khẩu tạm thời (Temporary Password)

- Khi tạo employee mới (`POST /api/v1/employees`) hoặc reset password (`POST /api/v1/employees/{id}/reset-password`), hệ thống **tự sinh ngẫu nhiên** mật khẩu tạm (đảm bảo tuân thủ Password Policy ở trên), hash bằng BCrypt trước khi lưu.
- Cờ `must_change_password = true` được set mặc định.
- Mật khẩu tạm được trả về:
  1. Trong response body (`temporaryPassword`) cho Admin/IT Staff đọc trực tiếp.
  2. Gửi qua **Email** (SendGrid) tới hộp thư nhân viên — xem `07-BUSINESS-RULES.md` mục Notification System.
- Khi `must_change_password = true`, **mọi** API ngoại trừ `GET /employees/me`, `PUT /employees/me/change-password`, và `POST /auth/logout` đều bị chặn ở tầng Backend (trả `403 Forbidden`, mã lỗi `AUTH_MUST_CHANGE_PASSWORD`) — buộc nhân viên đổi mật khẩu trước khi dùng hệ thống.

**Ví dụ sinh mật khẩu tạm (gợi ý implementation):**

```java
public String generateTemporaryPassword() {
    // Đảm bảo có đủ chữ hoa, chữ thường, số — tuân thủ Password Policy
    String upper = RandomStringUtils.random(2, "ABCDEFGHJKLMNPQRSTUVWXYZ");
    String lower = RandomStringUtils.random(3, "abcdefghijkmnpqrstuvwxyz");
    String digits = RandomStringUtils.random(3, "23456789");
    return shuffle(upper + lower + digits); // VD kết quả: "Tmp7xKq2"
}
```

## 4. RBAC — Mô hình phân quyền

### 4.1 4 Role cố định

| Role code | Tên hiển thị | Phạm vi dữ liệu |
|---|---|---|
| `ADMIN` | Quản trị viên (IT Manager — Trung tâm) | Toàn hệ thống (mọi chi nhánh) |
| `IT_STAFF` | Nhân viên IT chi nhánh | Chi nhánh của bản thân (`employee.branch_id`) |
| `MANAGER` | Trưởng phòng | Phòng ban của bản thân (`employee.department_id`, suy ra từ `departments.manager_id = employee.id`) |
| `EMPLOYEE` | Nhân viên | Chỉ dữ liệu liên quan trực tiếp đến bản thân |

### 4.2 Nguyên tắc phân quyền 2 chiều: Role × Scope

Mỗi action không chỉ kiểm tra **role** mà còn kiểm tra **scope** (phạm vi dữ liệu):

```
┌─────────────────────────────────────────────────────────┐
│  Bước 1: Role có được phép thực hiện ACTION này không?     │
│  (VD: chỉ MANAGER mới được gọi /requests/{id}/approve)      │
└─────────────────────────┬─────────────────────────────┘
                          │ Pass
                          ▼
┌─────────────────────────────────────────────────────────┐
│  Bước 2: Resource có thuộc SCOPE của user không?            │
│  (VD: Manager chỉ duyệt request của nhân viên PHÒNG MÌNH)    │
└─────────────────────────┬─────────────────────────────┘
                          │ Pass
                          ▼
                    ✅ Cho phép thực hiện
```

**Bước 1** triển khai qua Spring Security method-level annotation (`@PreAuthorize`). **Bước 2** triển khai qua **kiểm tra thủ công trong Service layer** (không thể biểu diễn đầy đủ bằng annotation đơn giản vì phụ thuộc dữ liệu runtime).

> Sai phạm vi quyền ở Bước 2 (đúng role nhưng sai scope) trả về **`403 Forbidden`**, không phải `404 Not Found` — nhất quán với mapping đã định nghĩa ở `09-ERROR-CODES.md`.

## 5. Authorization Matrix chi tiết

> Ký hiệu: ✅ = full quyền trong scope | 👁 = chỉ xem (read-only) | ❌ = không có quyền | **Override** = Admin có full quyền thực tế (xem mục 6)

### 5.1 Module Assets

| Action | ADMIN | IT_STAFF | MANAGER | EMPLOYEE |
|---|---|---|---|---|
| Xem danh sách/chi tiết asset | 👁 Toàn hệ thống | ✅ Chi nhánh mình | 👁 Chi nhánh/phòng mình | 👁 Chỉ asset mình đang giữ |
| Tạo asset mới | **Override** | ✅ Chi nhánh mình | ❌ | ❌ |
| Sửa asset | **Override** | ✅ Chi nhánh mình | ❌ | ❌ |
| Soft-delete asset | **Override** | ✅ Chi nhánh mình | ❌ | ❌ |
| Xem QR code | 👁 Toàn hệ thống | ✅ Chi nhánh mình | 👁 Chi nhánh/phòng mình | 👁 Asset mình giữ |
| Xem lịch sử cấp phát | 👁 Toàn hệ thống | ✅ Chi nhánh mình | 👁 Chi nhánh/phòng mình | 👁 Lịch sử của bản thân |
| Force-return | **Override** | ✅ Chi nhánh mình | ❌ | ❌ |
| Upload ảnh asset | **Override** | ✅ Chi nhánh mình | ❌ | ❌ |

### 5.2 Module Requests (Workflow)

| Action | ADMIN | IT_STAFF | MANAGER | EMPLOYEE |
|---|---|---|---|---|
| Tạo request (`ASSIGN`/`RETURN`) | ❌ | ❌ | ❌ | ✅ Cho chính mình |
| Xem danh sách request | 👁 Toàn hệ thống | 👁 Chi nhánh mình | 👁 Của nhân viên phòng mình | 👁 Của chính mình |
| Duyệt (`approve`) | ❌ | ❌ | ✅ Nhân viên phòng mình | ❌ |
| Từ chối (`reject`) | ❌ | ❌ | ✅ Nhân viên phòng mình | ❌ |
| Fulfill (hoàn tất) | **Override** | ✅ Chi nhánh mình | ❌ | ❌ |
| Hủy (`cancel`) | ❌ | ❌ | ❌ | ✅ Request của chính mình (chỉ khi `PENDING`) |

> **Lưu ý quan trọng:** `MANAGER` chỉ duyệt được request nếu **người tạo request (`employee_id`) thuộc phòng ban mà Manager đó phụ trách** — xác định qua `departments.manager_id = manager's employee.id` VÀ `requesting_employee.department_id = department.id`. Đây là kiểm tra Bước 2 (scope) bắt buộc trong `RequestServiceImpl.approve()`/`reject()`.

### 5.3 Module Employees

| Action | ADMIN | IT_STAFF | MANAGER | EMPLOYEE |
|---|---|---|---|---|
| Xem danh sách/chi tiết employee | ✅ Toàn hệ thống | ✅ Chi nhánh mình | 👁 Phòng mình | ❌ (chỉ xem hồ sơ mình qua `/me`) |
| Tạo employee mới | ✅ Mọi role | ✅ Chi nhánh mình, **không tạo được role `ADMIN`** | ❌ | ❌ |
| Sửa employee | ✅ Toàn hệ thống | ✅ Chi nhánh mình | ❌ | ✅ Chỉ hồ sơ cá nhân (`/me`) — giới hạn field cho phép sửa |
| Soft-delete employee | ✅ Toàn hệ thống | ✅ Chi nhánh mình | ❌ | ❌ |
| Reset password | ✅ Toàn hệ thống | ❌ | ❌ | ❌ |

> **Ràng buộc bổ sung:** `IT_STAFF` **không được** tạo/sửa employee có `role = ADMIN`, kể cả trong chi nhánh mình — kiểm tra tường minh trong `EmployeeServiceImpl.create()`/`update()`, trả lỗi `403 Forbidden` (mã `EMPLOYEE_CANNOT_ASSIGN_ADMIN_ROLE`) nếu vi phạm.

### 5.4 Module Branches & Departments

| Action | ADMIN | IT_STAFF | MANAGER | EMPLOYEE |
|---|---|---|---|---|
| Xem branches | 👁 Toàn hệ thống | 👁 Chi nhánh mình | 👁 Chi nhánh mình | 👁 Chi nhánh mình |
| Tạo/sửa/xoá branch | ✅ | ❌ | ❌ | ❌ |
| Xem departments | 👁 Toàn hệ thống | 👁 Chi nhánh mình | 👁 Phòng mình | 👁 Phòng mình |
| Tạo/sửa/xoá department | ✅ Toàn hệ thống | ✅ Chi nhánh mình | ❌ | ❌ |

### 5.5 Module Maintenance

| Action | ADMIN | IT_STAFF | MANAGER | EMPLOYEE |
|---|---|---|---|---|
| Xem maintenance record | 👁 Toàn hệ thống | ✅ Chi nhánh mình | 👁 Chi nhánh/phòng mình | 👁 Của asset mình đang/từng giữ |
| Tạo/sửa/xoá maintenance | **Override** | ✅ Chi nhánh mình | ❌ | ❌ |

### 5.6 Module Audit & Discrepancies

| Action | ADMIN | IT_STAFF | MANAGER | EMPLOYEE |
|---|---|---|---|---|
| Tạo audit session | **Override** | ✅ Chi nhánh mình | ❌ | ❌ |
| Thực hiện scan QR | **Override** | ✅ Chi nhánh mình | ❌ | ❌ |
| Complete audit session | **Override** | ✅ Chi nhánh mình | ❌ | ❌ |
| Xem audit session/discrepancy | 👁 Toàn hệ thống | ✅ Chi nhánh mình | 👁 Chi nhánh mình | ❌ |
| Resolve discrepancy | **Override** | ✅ Chi nhánh mình | ❌ | ❌ |

### 5.7 Module Categories

| Action | ADMIN | IT_STAFF | MANAGER | EMPLOYEE |
|---|---|---|---|---|
| Xem categories | 👁 | 👁 | 👁 | 👁 |
| Tạo/sửa/xoá category | ✅ | ❌ | ❌ | ❌ |

> `categories` dùng chung toàn hệ thống (không gắn `branch_id`), nên chỉ `ADMIN` quản lý — nhất quán với `05-DATABASE.md` mục 3 (ERD: "categories KHÔNG thuộc branch").

### 5.8 Module Notifications

| Action | ADMIN | IT_STAFF | MANAGER | EMPLOYEE |
|---|---|---|---|---|
| Xem/đánh dấu đã đọc notification của bản thân | ✅ | ✅ | ✅ | ✅ |

> Notification luôn chỉ thuộc về chính người dùng đang đăng nhập (`notifications.employee_id = current_user.id`) — không có khái niệm "xem notification người khác" cho bất kỳ role nào, kể cả Admin.

## 6. Admin Override — Định nghĩa kỹ thuật

**Backend:** `ADMIN` có **full quyền thực tế** trên mọi resource thuộc mọi chi nhánh — tầng Service **không áp dụng filter `branch_id`** đối với role `ADMIN` (xem `01-ARCHITECTURE.md` mục 8). Về mặt kỹ thuật, Backend **không phân biệt** "Admin xem" và "Admin sửa" — cả hai đều được phép nếu method có `@PreAuthorize("hasRole('ADMIN')")` hoặc tương đương.

**Frontend:** Để tránh thao tác nhầm trên dữ liệu chi nhánh khác, Frontend áp dụng UX 2 bước:

1. Khi Admin xem resource (asset/employee/request...) thuộc chi nhánh, giao diện mặc định hiển thị ở **chế độ read-only** (input/form bị disable, không có nút Sửa/Xoá hiển thị trực tiếp).
2. Admin phải bấm nút tường minh **"Chỉnh sửa với quyền Admin"** (hoặc tương đương) để mở khoá form chỉnh sửa — hành động này **chỉ là UX warning**, không phải một bước xác thực bổ sung ở Backend (Backend đã cho phép Admin sửa ngay từ đầu).

```
┌──────────────────────────────────────────┐
│  [Admin xem Asset thuộc chi nhánh HCM]      │
│                                              │
│  Tên: Dell Latitude 5440        [readonly]   │
│  Trạng thái: ASSIGNED            [readonly]   │
│                                              │
│  ⚠️ Bạn đang xem dữ liệu chi nhánh khác.       │
│     [ Chỉnh sửa với quyền Admin ]              │
└──────────────────────────────────────────┘
                    │ click
                    ▼
┌──────────────────────────────────────────┐
│  Form được mở khoá, có thể sửa trực tiếp     │
│  [ Lưu thay đổi ]   [ Huỷ ]                    │
└──────────────────────────────────────────┘
```

> Đây là **UX safeguard**, không phải security boundary — security thật sự nằm hoàn toàn ở Backend (`@PreAuthorize` + scope check). Không được implement override logic chỉ ở Frontend mà thiếu kiểm tra tương ứng ở Backend.

## 7. Session / Device Management

- **Multi-device login được hỗ trợ đầy đủ**: 1 `employee` có thể có **nhiều Refresh Token** đồng thời (1 token = 1 thiết bị/phiên đăng nhập), nhờ quan hệ 1-n giữa `employees` và `refresh_tokens` (xem `05-DATABASE.md` mục 5.6).
- Mỗi lần login thành công tạo **1 record mới** trong `refresh_tokens`, không ghi đè record cũ của thiết bị khác.
- `device_label` lưu thông tin gợi nhớ (VD: rút gọn từ `User-Agent` header — "Chrome on Windows", "Safari on iPhone") để **tương lai** hỗ trợ tính năng "xem danh sách thiết bị đang đăng nhập" (chưa có UI cho tính năng này ở MVP, nhưng cột dữ liệu đã sẵn sàng).
- **Logout hiện tại chỉ revoke đúng 1 token** (của thiết bị đang thực hiện logout) — **không** tự động đăng xuất các thiết bị khác.

```
employee_id = 5
┌─────────────────────────────────────────────────────────┐
│ refresh_tokens                                              │
├────┬─────────────┬──────────────────┬────────────────────┤
│ id │ employee_id  │ device_label        │ expires_at           │
├────┼─────────────┼──────────────────┼────────────────────┤
│ 11 │ 5            │ Chrome on Windows    │ 2026-07-30          │ ← phiên Desktop
│ 14 │ 5            │ Safari on iPhone     │ 2026-08-02          │ ← phiên Mobile
└────┴─────────────┴──────────────────┴────────────────────┘

Logout từ Desktop → DELETE record id=11 → record id=14 (Mobile) KHÔNG bị ảnh hưởng
```

> Tính năng **"logout toàn bộ thiết bị"** (logout-all-devices — xoá toàn bộ record theo `employee_id` theo yêu cầu chủ động của chính nhân viên, phân biệt với trường hợp Admin reset password đã liệt kê ở mục 2.3) **chưa có** ở MVP — đánh dấu **TODO: Need confirmation** (xem mục 11).

## 8. Spring Security Implementation Pattern

**Cấu hình filter chain (gợi ý, đặt tại `common/config/SecurityConfig.java`):**

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // bật @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // stateless JWT, không cần CSRF token cho API
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Method-level authorization (Bước 1 — Role check):**

```java
@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class RequestController {

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<RequestResponse>> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        // Bước 2 (scope check) thực hiện BÊN TRONG Service, không phải ở Controller
        RequestResponse response = requestService.approve(id, currentUser.getEmployeeId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

**Scope check (Bước 2 — trong Service layer):**

```java
@Override
@Transactional
public RequestResponse approve(Long requestId, Long managerId) {
    Request request = requestRepository.findById(requestId)
        .orElseThrow(() -> new RequestNotFoundException(requestId));

    Employee requester = employeeRepository.findById(request.getEmployeeId())
        .orElseThrow(() -> new EmployeeNotFoundException(request.getEmployeeId()));

    // Scope check: Manager chỉ duyệt được request của nhân viên PHÒNG MÌNH phụ trách
    Department department = departmentRepository.findById(requester.getDepartmentId())
        .orElseThrow(() -> new DepartmentNotFoundException(requester.getDepartmentId()));

    if (!department.getManagerId().equals(managerId)) {
        log.warn("Manager {} attempted to approve request {} outside their department scope",
            managerId, requestId);
        throw new ForbiddenException("AUTH_OUT_OF_SCOPE", "Bạn không có quyền duyệt yêu cầu này");
    }

    if (request.getStatus() != RequestStatus.PENDING) {
        throw new BusinessException("REQUEST_ALREADY_PROCESSED", "Yêu cầu đã được xử lý trước đó");
    }

    request.setStatus(RequestStatus.APPROVED);
    request.setApprovedBy(managerId);
    request.setApprovedAt(Instant.now());
    requestRepository.save(request);

    log.info("Request approved: requestId={}, managerId={}", requestId, managerId);
    return requestMapper.toResponse(request);
}
```

**Branch-scope filter cho `IT_STAFF`/`MANAGER`/`EMPLOYEE` (gợi ý pattern dùng chung):**

```java
// Áp dụng trong các method GET danh sách của AssetService, RequestService...
private Long resolveBranchFilter(CustomUserDetails currentUser) {
    if (currentUser.getRole() == Role.ADMIN) {
        return null; // null = không filter, xem toàn hệ thống
    }
    return currentUser.getBranchId(); // IT_STAFF/MANAGER/EMPLOYEE luôn bị giới hạn branch mình
}
```

## 9. Security Rules bổ sung

Các nguyên tắc sau **không được nghiên cứu gốc đặc tả chi tiết kỹ thuật** nhưng là best practice chuẩn áp dụng nhất quán với JWT + Spring Security đã chọn:

- **JWT secret key** không hardcode trong `application.yml` commit lên Git — luôn dùng `${JWT_SECRET}` placeholder, giá trị thật nằm trong Render Environment Variables (xem `11-DEPLOYMENT.md`).
- **CORS** cấu hình qua `CorsConfig`, allowed origins đọc từ biến môi trường (`${CORS_ALLOWED_ORIGINS}`) để linh hoạt theo domain Vercel thực tế, không hardcode domain cụ thể trong code.
- **Rate limiting** cho `/auth/login` (~5 lần/phút/IP) — chi tiết kỹ thuật implementation xem `04-API.md` mục 17 (còn TODO).
- **Không** trả thông tin chi tiết phân biệt "email không tồn tại" vs "sai mật khẩu" trong message lỗi đăng nhập (gộp chung `AUTH_INVALID_CREDENTIALS` — "Email hoặc mật khẩu không đúng") để tránh lộ thông tin email nào đã đăng ký trong hệ thống (user enumeration).
- **Refresh Token cookie attributes** bắt buộc đầy đủ: `HttpOnly`, `Secure` (chỉ gửi qua HTTPS), `SameSite=Lax` (cân bằng giữa bảo mật CSRF và trải nghiệm cross-site redirect cơ bản).
- Toàn bộ field nhạy cảm (`password_hash`, `token_hash`) **không bao giờ** xuất hiện trong bất kỳ DTO Response nào — đảm bảo qua nguyên tắc DTO tách biệt Entity (`03-CODING-STANDARDS.md` mục 7).

## 10. Luồng đăng nhập đầu tiên & đổi mật khẩu bắt buộc

```
┌──────────────┐
│ Admin tạo       │
│ employee mới     │ → must_change_password = true, mật khẩu tạm gửi Email + hiện trên UI Admin
└───────┬──────┘
        │
        ▼
┌──────────────────────────┐
│ Nhân viên login lần đầu      │
│ POST /auth/login (mật khẩu tạm) │
└───────┬──────────────────┘
        │ 200 OK — response.data.user.mustChangePassword = true
        ▼
┌──────────────────────────────────────────┐
│ Frontend redirect bắt buộc tới trang        │
│ "Đổi mật khẩu" — chặn truy cập mọi route khác │
└───────┬──────────────────────────────────┘
        │ PUT /api/v1/employees/me/change-password
        │ { oldPassword, newPassword }
        ▼
┌──────────────────────────────────────────┐
│ Backend: validate Password Policy mới,       │
│ set must_change_password = false,             │
│ revoke toàn bộ refresh_tokens CŨ (trừ phiên   │
│ hiện tại — xem mục 2.3)                       │
└──────────────────────────────────────────┘
        │
        ▼
┌──────────────────────────┐
│ Truy cập hệ thống bình thường │
└──────────────────────────┘
```

> Backend **enforce** chặn `must_change_password = true` ở tầng Filter/Interceptor (không chỉ dựa vào Frontend redirect) — đảm bảo an toàn kể cả khi gọi API trực tiếp (Postman, script...).

## 11. TODO / Open Questions

> TODO: Need confirmation — **Logout toàn bộ thiết bị (logout-all-devices)**: tính năng cho phép nhân viên chủ động revoke toàn bộ Refresh Token của chính mình (VD: khi nghi ngờ bị lộ tài khoản, dùng máy công cộng quên logout) **chưa có** endpoint trong `04-API.md`. Đề xuất bổ sung `POST /api/v1/auth/logout-all` nếu cần triển khai — cần Tech Lead xác nhận.

> TODO: Need confirmation — **Chính sách rotate Refresh Token**: hiện tại mục 2.3 mô tả Refresh Token **giữ nguyên** (không cấp lại) mỗi lần gọi `/auth/refresh`, chỉ cấp Access Token mới. Đây là lựa chọn **đơn giản hơn** nhưng có rủi ro replay attack cao hơn so với phương án rotate (mỗi lần refresh đều cấp Refresh Token mới + thu hồi token cũ). Khuyến nghị best practice là rotate, nhưng cần Tech Lead xác nhận trước khi triển khai (đã ghi nhận từ `01-ARCHITECTURE.md` mục 12 và `00-OVERVIEW.md` mục 12).

> TODO: Need confirmation — **Cơ chế dọn dẹp Refresh Token hết hạn**: bảng `refresh_tokens` không tự xoá record khi `expires_at < now()` (chỉ bị từ chối logic, không bị xoá vật lý) — cần xác nhận có cần thêm 1 `@Scheduled` job dọn dẹp định kỳ (tương tự Audit auto-expire job) hay chấp nhận để bảng phình dần ở quy mô MVP (200-300 thiết bị, 50-100 nhân viên → số lượng record nhỏ, có thể chưa cần tối ưu sớm).

---

*Xem tiếp: `07-BUSINESS-RULES.md` để biết toàn bộ quy tắc nghiệp vụ, workflow và edge case.*