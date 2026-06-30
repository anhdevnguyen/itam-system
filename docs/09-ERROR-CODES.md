# 09 — ERROR CODES

> Danh sách đầy đủ mã lỗi (error code) theo từng module, HTTP status mapping, exception hierarchy và quy tắc logging tương ứng. Đây là tài liệu tham chiếu chéo (cross-reference) tổng hợp lại toàn bộ exception đã được đề cập rải rác ở các file khác (`05-DATABASE.md`, `06-AUTHENTICATION.md`, `07-BUSINESS-RULES.md`).

## Mục lục

1. [Quy ước Error Code](#1-quy-ước-error-code)
2. [HTTP Status Mapping](#2-http-status-mapping)
3. [Exception Hierarchy](#3-exception-hierarchy)
4. [GlobalExceptionHandler Implementation](#4-globalexceptionhandler-implementation)
5. [Validation Error — Format đặc biệt](#5-validation-error--format-đặc-biệt)
6. [Danh sách Error Code đầy đủ theo Module](#6-danh-sách-error-code-đầy-đủ-theo-module)
7. [Logging theo Exception Type](#7-logging-theo-exception-type)
8. [TODO / Open Questions](#8-todo--open-questions)

---

## 1. Quy ước Error Code

| Quy tắc | Mô tả |
|---|---|
| Format | `<MODULE>_<ERROR_TYPE>` |
| Naming | `UPPER_SNAKE_CASE` |
| Ngôn ngữ | Luôn **Tiếng Anh** (đảm bảo nhất quán cho máy/log/Frontend mapping), chỉ phần `message` đi kèm là **Tiếng Việt** hiển thị cho người dùng (xem `03-CODING-STANDARDS.md` mục 6) |
| Module prefix | Tên viết tắt rõ ràng theo feature package: `AUTH`, `ASSET`, `REQUEST`, `EMPLOYEE`, `MAINTENANCE`, `AUDIT`, `DISCREPANCY`, `BRANCH`, `DEPARTMENT`, `CATEGORY`, `VALIDATION` |

**Ví dụ:** `AUTH_INVALID_CREDENTIALS`, `ASSET_NOT_FOUND`, `ASSET_NOT_AVAILABLE`, `REQUEST_ALREADY_PROCESSED`, `EMPLOYEE_EMAIL_DUPLICATE`, `VALIDATION_ERROR`.

## 2. HTTP Status Mapping

| Tình huống | HTTP Status | Khi nào áp dụng |
|---|---|---|
| Thành công (GET/PUT) | **200 OK** | Đọc/cập nhật thành công |
| Tạo mới thành công (POST) | **201 Created** | Tạo resource mới (employee, asset, request...) |
| Xoá thành công (soft delete) | **204 No Content** | `DELETE /{resource}/{id}` thành công — **không có body** |
| Validation lỗi | **400 Bad Request** | Input không hợp lệ (sai format, thiếu field bắt buộc, vi phạm `@NotNull`/`@Size`...) |
| Chưa đăng nhập / token hết hạn | **401 Unauthorized** | Thiếu/sai/hết hạn Access Token |
| Sai phạm vi quyền (đúng role, sai scope) | **403 Forbidden** | VD: Manager cố duyệt request của phòng ban khác |
| Không tìm thấy resource | **404 Not Found** | ID không tồn tại trong DB (hoặc đã bị soft-delete và không truy vấn `includeDeleted`) |
| Conflict | **409 Conflict** | Email trùng, request đã xử lý, action không hợp lệ với trạng thái hiện tại |
| Lỗi server không mong muốn | **500 Internal Server Error** | Lỗi hệ thống (DB down, NullPointerException, lỗi 3rd-party không xử lý được) |

> **Nguyên tắc chuẩn REST nghiêm ngặt:** Mọi response lỗi đều đi qua `GlobalExceptionHandler`, không có Controller nào tự `try-catch` rồi trả `ResponseEntity` thủ công riêng lẻ — đảm bảo format `ApiResponse` (`01-ARCHITECTURE.md` mục 7) **luôn nhất quán** trên toàn hệ thống.

## 3. Exception Hierarchy

```
BaseException (abstract)
│   - errorCode: String
│   - httpStatus: HttpStatus
│   - message: String (Tiếng Việt)
│
├── BusinessException              (lỗi nghiệp vụ có chủ đích — log WARN)
│   ├── AssetNotAvailableException
│   ├── RequestAlreadyProcessedException
│   ├── AssetNotAssignedException
│   ├── AuditSessionAlreadyCompletedException
│   ├── DiscrepancyAlreadyResolvedException
│   └── ... (các exception nghiệp vụ khác)
│
├── ResourceNotFoundException      (404 — log WARN)
│   ├── AssetNotFoundException
│   ├── EmployeeNotFoundException
│   ├── RequestNotFoundException
│   ├── BranchNotFoundException
│   ├── DepartmentNotFoundException
│   ├── CategoryNotFoundException
│   ├── MaintenanceRecordNotFoundException
│   ├── AuditSessionNotFoundException
│   └── DiscrepancyNotFoundException
│
├── ValidationException            (400 — log WARN)
│
├── UnauthorizedException          (401 — log WARN)
│   └── InvalidCredentialsException
│
└── ForbiddenException             (403 — log WARN)
    └── OutOfScopeException
```

**`BaseException` (gợi ý implementation, đặt tại `common/exception/`):**

```java
@Getter
public abstract class BaseException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;

    protected BaseException(String errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
```

**`ResourceNotFoundException` (base cho mọi `XxxNotFoundException`):**

```java
public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String errorCode, String message) {
        super(errorCode, HttpStatus.NOT_FOUND, message);
    }
}

// Ví dụ cụ thể hoá theo từng feature (đặt trong package exception/ của feature đó)
public class AssetNotFoundException extends ResourceNotFoundException {
    public AssetNotFoundException(Long id) {
        super("ASSET_NOT_FOUND", "Không tìm thấy thiết bị với ID: " + id);
    }
}
```

**`BusinessException` (base cho mọi lỗi nghiệp vụ tuỳ biến mã lỗi/message):**

```java
public class BusinessException extends BaseException {
    public BusinessException(String errorCode, String message) {
        super(errorCode, HttpStatus.CONFLICT, message); // mặc định 409, có thể override constructor nếu cần status khác
    }

    public BusinessException(String errorCode, HttpStatus httpStatus, String message) {
        super(errorCode, httpStatus, message);
    }
}

// Ví dụ cụ thể hoá
public class AssetNotAvailableException extends BusinessException {
    public AssetNotAvailableException(Long assetId) {
        super("ASSET_NOT_AVAILABLE", "Thiết bị ID " + assetId + " hiện không khả dụng để cấp phát");
    }
}
```

## 4. GlobalExceptionHandler Implementation

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex) {
        if (ex instanceof BusinessException || ex instanceof ResourceNotFoundException
                || ex instanceof ValidationException || ex instanceof UnauthorizedException
                || ex instanceof ForbiddenException) {
            log.warn("Business/Client error: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        }

        ApiError error = new ApiError(ex.getErrorCode(), null, ex.getMessage());
        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(ApiResponse.error(List.of(error)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<ApiError> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new ApiError("VALIDATION_ERROR", fe.getField(), fe.getDefaultMessage()))
            .toList();

        log.warn("Validation failed: fieldsCount={}, fields={}",
            errors.size(), errors.stream().map(ApiError::getField).toList());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(errors));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ApiError error = new ApiError("AUTH_FORBIDDEN", null, "Bạn không có quyền thực hiện hành động này");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(List.of(error)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        log.error("Unexpected system error", ex);
        ApiError error = new ApiError("INTERNAL_SERVER_ERROR", null, "Đã có lỗi xảy ra, vui lòng thử lại sau");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(List.of(error)));
    }
}
```

> **Quan trọng:** Exception hệ thống không mong muốn (`Exception.class` catch-all) **không bao giờ** lộ chi tiết kỹ thuật (stack trace, tên class, thông tin nội bộ) ra ngoài response cho client — chỉ log đầy đủ ở `ERROR` level phía server, response trả về message Tiếng Việt chung chung an toàn.

## 5. Validation Error — Format đặc biệt

**Nguyên tắc:** Trả về **TẤT CẢ** lỗi validation phát hiện được trong **cùng 1 lần submit**, không dừng lại ở lỗi đầu tiên — giúp Frontend hiển thị hết lỗi để người dùng sửa **1 lần duy nhất** thay vì submit nhiều lần.

**Ví dụ response khi tạo Employee với nhiều lỗi cùng lúc:**

```json
{
  "success": false,
  "errors": [
    { "code": "VALIDATION_ERROR", "field": "email", "message": "Email không đúng định dạng" },
    { "code": "VALIDATION_ERROR", "field": "fullName", "message": "Họ tên không được để trống" },
    { "code": "VALIDATION_ERROR", "field": "roleId", "message": "Vai trò là bắt buộc" }
  ],
  "meta": { "timestamp": "2026-06-30T10:00:00Z" }
}
```

**DTO tương ứng (gợi ý, dùng Jakarta Bean Validation):**

```java
public class CreateEmployeeRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotNull(message = "Vai trò là bắt buộc")
    private Long roleId;

    @NotNull(message = "Chi nhánh là bắt buộc")
    private Long branchId;

    private Long departmentId; // nullable — Admin/IT Staff trung tâm có thể không gắn department
}
```

## 6. Danh sách Error Code đầy đủ theo Module

> ⚠️ **Ghi chú nguồn:** Danh sách dưới đây tổng hợp lại (1) các mã lỗi đã được đề cập **tường minh** trong nghiên cứu gốc (`AUTH_INVALID_CREDENTIALS`, `ASSET_NOT_FOUND`, `ASSET_NOT_AVAILABLE`, `REQUEST_ALREADY_PROCESSED`, `EMPLOYEE_EMAIL_DUPLICATE`, `VALIDATION_ERROR`...), và (2) các mã lỗi **suy luận hợp lý bổ sung** cần thiết để đảm bảo mọi exception đã đặc tả ở các file khác (`05`-`07`) đều có mã lỗi tương ứng đầy đủ, nhất quán format `<MODULE>_<ERROR_TYPE>`. Mã lỗi nhóm (2) được đánh dấu *(suy luận)*.

### 6.1 Module AUTH

| Error Code | HTTP Status | Message (Tiếng Việt) | Ghi chú |
|---|---|---|---|
| `AUTH_INVALID_CREDENTIALS` | 401 | Email hoặc mật khẩu không đúng | Gộp chung 2 trường hợp (email không tồn tại / sai mật khẩu) — chống user enumeration, xem `06-AUTHENTICATION.md` mục 9 |
| `AUTH_TOKEN_EXPIRED` | 401 | Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại | *(suy luận)* Access Token hết hạn |
| `AUTH_TOKEN_INVALID` | 401 | Token không hợp lệ | *(suy luận)* JWT signature sai/malformed |
| `AUTH_REFRESH_TOKEN_INVALID` | 401 | Refresh token không hợp lệ hoặc đã hết hạn | *(suy luận)* Refresh token không tìm thấy trong DB / đã revoke / `expires_at` quá hạn |
| `AUTH_MUST_CHANGE_PASSWORD` | 403 | Bạn cần đổi mật khẩu trước khi tiếp tục sử dụng hệ thống | Xem `06-AUTHENTICATION.md` mục 10 |
| `AUTH_FORBIDDEN` | 403 | Bạn không có quyền thực hiện hành động này | *(suy luận)* Dùng chung cho `AccessDeniedException` |
| `AUTH_OUT_OF_SCOPE` | 403 | Bạn không có quyền truy cập dữ liệu này | Đúng role, sai scope — VD: Manager ngoài phòng ban (`06-AUTHENTICATION.md` mục 8) |
| `AUTH_PASSWORD_POLICY_VIOLATION` | 400 | Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số | Xem `06-AUTHENTICATION.md` mục 3 |
| `AUTH_OLD_PASSWORD_INCORRECT` | 400 | Mật khẩu hiện tại không đúng | *(suy luận)* Khi đổi mật khẩu, `oldPassword` không khớp |
| `AUTH_RATE_LIMIT_EXCEEDED` | 429 | Quá nhiều lần thử, vui lòng thử lại sau ít phút | Giới hạn `/auth/login` ~5 lần/phút/IP (`04-API.md` mục 17) |

### 6.2 Module EMPLOYEE

| Error Code | HTTP Status | Message (Tiếng Việt) | Ghi chú |
|---|---|---|---|
| `EMPLOYEE_NOT_FOUND` | 404 | Không tìm thấy nhân viên | |
| `EMPLOYEE_EMAIL_DUPLICATE` | 409 | Email đã được sử dụng bởi nhân viên khác | `employees.email` UNIQUE constraint violation |
| `EMPLOYEE_CANNOT_ASSIGN_ADMIN_ROLE` | 403 | IT Staff không được phép tạo hoặc gán vai trò Admin | Xem `06-AUTHENTICATION.md` mục 5.3 |
| `EMPLOYEE_HAS_ASSIGNED_ASSETS` | 200 *(cảnh báo, không chặn)* | Nhân viên này đang giữ N thiết bị, vui lòng thu hồi qua chức năng Force-return | Không phải lỗi chặn — chỉ cảnh báo kèm trong response thành công, xem `07-BUSINESS-RULES.md` mục 7 |
| `EMPLOYEE_SELF_DELETE_FORBIDDEN` | 403 *(suy luận)* | Bạn không thể tự xoá tài khoản của chính mình | *(suy luận)* Best practice ngăn Admin tự soft-delete chính mình gây mất quyền truy cập hệ thống |

### 6.3 Module BRANCH

| Error Code | HTTP Status | Message (Tiếng Việt) | Ghi chú |
|---|---|---|---|
| `BRANCH_NOT_FOUND` | 404 | Không tìm thấy chi nhánh | |
| `BRANCH_CODE_DUPLICATE` | 409 | Mã chi nhánh đã tồn tại | `branches.code` UNIQUE constraint violation |
| `BRANCH_HAS_ACTIVE_DEPENDENCIES` | 409 *(suy luận)* | Không thể xoá chi nhánh vì vẫn còn nhân viên/thiết bị đang hoạt động | Xem `05-DATABASE.md` mục 7 và `07-BUSINESS-RULES.md` mục 9 — best practice bổ sung, cần Tech Lead xác nhận |

### 6.4 Module DEPARTMENT

| Error Code | HTTP Status | Message (Tiếng Việt) | Ghi chú |
|---|---|---|---|
| `DEPARTMENT_NOT_FOUND` | 404 | Không tìm thấy phòng ban | |
| `DEPARTMENT_HAS_ACTIVE_EMPLOYEES` | 409 *(suy luận)* | Không thể xoá phòng ban vì vẫn còn nhân viên đang hoạt động | Xem `07-BUSINESS-RULES.md` mục 9 |

### 6.5 Module CATEGORY

| Error Code | HTTP Status | Message (Tiếng Việt) | Ghi chú |
|---|---|---|---|
| `CATEGORY_NOT_FOUND` | 404 | Không tìm thấy danh mục thiết bị | |
| `CATEGORY_CODE_DUPLICATE` | 409 | Mã danh mục đã tồn tại | `categories.code` UNIQUE constraint violation |
| `CATEGORY_HAS_ACTIVE_ASSETS` | 409 *(suy luận)* | Không thể xoá danh mục vì vẫn còn thiết bị đang sử dụng | Xem `07-BUSINESS-RULES.md` mục 9 |

### 6.6 Module ASSET

| Error Code | HTTP Status | Message (Tiếng Việt) | Ghi chú |
|---|---|---|---|
| `ASSET_NOT_FOUND` | 404 | Không tìm thấy thiết bị | |
| `ASSET_NOT_AVAILABLE` | 409 | Thiết bị hiện không khả dụng để cấp phát | Đã có request `PENDING`/`APPROVED` khác hoặc status khác `AVAILABLE` |
| `ASSET_NOT_ASSIGNED` | 409 | Thiết bị hiện không được cấp phát cho ai | Force-return khi `assigned_to IS NULL` |
| `ASSET_CODE_GENERATION_CONFLICT` | 500→**retry**, hoặc 409 nếu hết lượt retry *(suy luận)* | Không thể sinh mã thiết bị, vui lòng thử lại | Race condition khi sinh `code` — xem `07-BUSINESS-RULES.md` mục 5 |
| `ASSET_IMAGE_LIMIT_EXCEEDED` | 400 *(suy luận)* | Mỗi thiết bị chỉ được tối đa 5 ảnh | Giới hạn nghiệp vụ, kiểm tra ở Service (`05-DATABASE.md` mục 5.8) |
| `ASSET_IMAGE_SIZE_EXCEEDED` | 400 *(suy luận)* | Kích thước ảnh vượt quá giới hạn 5MB | |
| `ASSET_IMAGE_UPLOAD_FAILED` | 500 | Tải ảnh thất bại, vui lòng thử lại | Lỗi gọi Cloudinary (ví dụ minh hoạ tại `03-CODING-STANDARDS.md` mục 4) |
| `ASSET_INVALID_VALUE` | 400 | Giá trị thiết bị phải lớn hơn 0 | Validation `value > 0` |
| `ASSET_INVALID_PURCHASE_DATE` | 400 | Ngày mua không được là ngày trong tương lai | Validation `purchase_date <= CURRENT_DATE` |

### 6.7 Module REQUEST

| Error Code | HTTP Status | Message (Tiếng Việt) | Ghi chú |
|---|---|---|---|
| `REQUEST_NOT_FOUND` | 404 | Không tìm thấy yêu cầu | |
| `REQUEST_ALREADY_PROCESSED` | 409 | Yêu cầu đã được xử lý trước đó | Action lên request đã ở terminal state, xem `07-BUSINESS-RULES.md` mục 1.2 |
| `REQUEST_ASSET_NOT_ASSIGNED_TO_YOU` | 403 | Bạn không thể yêu cầu trả thiết bị không phải do mình đang giữ | Tạo request `RETURN` sai chủ sở hữu |
| `REQUEST_REJECTION_REASON_REQUIRED` | 400 | Lý do từ chối là bắt buộc | `@NotBlank` trên `rejectionReason` |
| `REQUEST_CANNOT_CANCEL_NON_PENDING` | 409 *(suy luận)* | Chỉ có thể huỷ yêu cầu khi đang ở trạng thái chờ duyệt | Cancel chỉ hợp lệ khi `status = PENDING` |
| `REQUEST_OUT_OF_DEPARTMENT_SCOPE` | 403 *(suy luận)* | Bạn chỉ được duyệt yêu cầu của nhân viên thuộc phòng ban mình quản lý | Cụ thể hoá của `AUTH_OUT_OF_SCOPE` riêng cho module Request |

### 6.8 Module MAINTENANCE

| Error Code | HTTP Status | Message (Tiếng Việt) | Ghi chú |
|---|---|---|---|
| `MAINTENANCE_NOT_FOUND` | 404 | Không tìm thấy bản ghi bảo trì | |
| `MAINTENANCE_INVALID_STATUS_TRANSITION` | 409 *(suy luận)* | Không thể chuyển sang trạng thái này | Bảo vệ tính nhất quán nếu cần giới hạn chuyển trạng thái maintenance (VD: không cho `COMPLETED → SCHEDULED`) |

### 6.9 Module AUDIT

| Error Code | HTTP Status | Message (Tiếng Việt) | Ghi chú |
|---|---|---|---|
| `AUDIT_SESSION_NOT_FOUND` | 404 | Không tìm thấy phiên kiểm kê | |
| `AUDIT_SESSION_ALREADY_COMPLETED` | 409 | Phiên kiểm kê đã hoàn tất trước đó | Xem `07-BUSINESS-RULES.md` mục 4.4 |
| `AUDIT_SCAN_BRANCH_MISMATCH` | 400 *(suy luận)* | Thiết bị này không thuộc chi nhánh đang kiểm kê | Xem `07-BUSINESS-RULES.md` mục 4.3 — chặn quét nhầm asset khác chi nhánh |
| `AUDIT_SCAN_ASSET_CODE_NOT_FOUND` | 404 *(suy luận)* | Không tìm thấy thiết bị với mã đã quét | QR code quét được không khớp asset nào trong hệ thống |

### 6.10 Module DISCREPANCY

| Error Code | HTTP Status | Message (Tiếng Việt) | Ghi chú |
|---|---|---|---|
| `DISCREPANCY_NOT_FOUND` | 404 | Không tìm thấy báo cáo sai lệch | |
| `DISCREPANCY_ALREADY_RESOLVED` | 409 | Sai lệch này đã được xử lý trước đó | Xem `07-BUSINESS-RULES.md` mục 4.6 |
| `DISCREPANCY_INVALID_ACTION_FOR_TYPE` | 400 *(suy luận)* | Hành động xử lý không hợp lệ cho loại sai lệch này | `action` (`CONFIRM_LOST`/`FOUND`) chỉ áp dụng cho `type=MISSING` |

### 6.11 Module VALIDATION (chung)

| Error Code | HTTP Status | Message (Tiếng Việt) | Ghi chú |
|---|---|---|---|
| `VALIDATION_ERROR` | 400 | *(message cụ thể theo từng field)* | Dùng chung cho mọi lỗi Jakarta Bean Validation — xem mục 5 |

### 6.12 Module SYSTEM (chung)

| Error Code | HTTP Status | Message (Tiếng Việt) | Ghi chú |
|---|---|---|---|
| `INTERNAL_SERVER_ERROR` | 500 | Đã có lỗi xảy ra, vui lòng thử lại sau | Catch-all cho exception không xác định |
| `RESOURCE_NOT_FOUND` | 404 *(suy luận, fallback)* | Không tìm thấy dữ liệu | Dùng nếu cần 1 mã lỗi 404 tổng quát ngoài các `XXX_NOT_FOUND` cụ thể theo module |

## 7. Logging theo Exception Type

> Nhất quán với `03-CODING-STANDARDS.md` mục 4.

| Loại Exception | Log Level | Lý do |
|---|---|---|
| `BusinessException` và các lớp con (`ResourceNotFoundException`, `ValidationException`, `UnauthorizedException`, `ForbiddenException`) | **`WARN`** | Lỗi nghiệp vụ có chủ đích, hệ thống vẫn hoạt động đúng thiết kế — không phải bug |
| Exception hệ thống thực sự (`Exception.class` catch-all, `NullPointerException`, lỗi DB, lỗi gọi Cloudinary/SendGrid không xử lý được) | **`ERROR`** | Cần điều tra, có thể là bug hoặc sự cố hạ tầng |

**Ví dụ phân biệt rõ trong code:**

```java
// WARN — lỗi nghiệp vụ, người dùng thao tác sai/trạng thái không hợp lệ
log.warn("Request approval rejected due to scope mismatch: requestId={}, managerId={}", requestId, managerId);

// ERROR — lỗi hệ thống thực sự, cần điều tra
log.error("Database connection failed while fetching asset list", e);
```

## 8. TODO / Open Questions

> TODO: Need confirmation — Toàn bộ mã lỗi đánh dấu *(suy luận)* trong mục 6 **chưa được liệt kê tường minh** trong nghiên cứu gốc (chỉ có 1 vài ví dụ mẫu như `AUTH_INVALID_CREDENTIALS`, `ASSET_NOT_FOUND`, `ASSET_NOT_AVAILABLE`, `REQUEST_ALREADY_PROCESSED`, `EMPLOYEE_EMAIL_DUPLICATE`, `VALIDATION_ERROR` được đặc tả rõ ràng từ Chủ đề 11). Các mã còn lại được bổ sung theo nguyên tắc nhất quán (mọi exception đã đặc tả ở `05`-`07` đều cần 1 mã lỗi tương ứng) — cần Tech Lead rà soát lại toàn bộ danh sách trước khi triển khai chính thức, có thể cần đổi tên hoặc gộp bớt một số mã cho gọn.

---

*Xem tiếp: `10-TESTING.md` để biết chiến lược kiểm thử, công cụ, coverage và E2E testing.*