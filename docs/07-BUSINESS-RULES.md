# 07 — BUSINESS RULES

> Toàn bộ quy tắc nghiệp vụ, state machine, workflow, validation rule, edge case và hệ thống thông báo của ITAM. Đây là tài liệu **quan trọng nhất** cho AI Coding Agent khi triển khai Service layer — mọi business logic phức tạp không thể hiện rõ qua schema DB hay route API đơn thuần đều được đặc tả chi tiết tại đây.

## Mục lục

1. [Request Workflow — State Machine đầy đủ](#1-request-workflow--state-machine-đầy-đủ)
2. [Request Workflow — Edge Cases](#2-request-workflow--edge-cases)
3. [Maintenance Rules](#3-maintenance-rules)
4. [Audit Rules — State Machine & Lifecycle](#4-audit-rules--state-machine--lifecycle)
5. [Asset Code Generation](#5-asset-code-generation)
6. [Validation Rules tổng hợp](#6-validation-rules-tổng-hợp)
7. [Offboarding Nhân viên](#7-offboarding-nhân-viên)
8. [Notification System](#8-notification-system)
9. [Quy tắc Soft Delete liên ngành](#9-quy-tắc-soft-delete-liên-ngành)
10. [Bảng tổng hợp Business Exception](#10-bảng-tổng-hợp-business-exception)
11. [TODO / Open Questions](#11-todo--open-questions)

---

## 1. Request Workflow — State Machine đầy đủ

### 1.1 Sơ đồ trạng thái (State Diagram)

```
                    ┌─────────────┐
                    │   PENDING     │ ◄── Employee tạo request (POST /requests)
                    └──────┬──────┘
                           │
            ┌──────────────┼──────────────┐
            │ Manager approve │ Manager reject │ Employee cancel
            ▼              │              ▼
     ┌─────────────┐       │       ┌─────────────┐
     │  APPROVED     │       │       │  REJECTED     │ ◄── (terminal state)
     └──────┬──────┘       │       └─────────────┘
            │              │
            │ IT Staff      │
            │ fulfill        │
            ▼              ▼
     ┌─────────────┐  ┌─────────────┐
     │  FULFILLED    │  │  CANCELLED    │ ◄── (terminal state, chỉ từ PENDING)
     │ (terminal)    │  └─────────────┘
     └─────────────┘
```

### 1.2 Bảng chuyển trạng thái hợp lệ

| Từ trạng thái | Action | Tới trạng thái | Ai thực hiện | Điều kiện |
|---|---|---|---|---|
| *(chưa tồn tại)* | `POST /requests` | `PENDING` | Employee | Asset phải `AVAILABLE` và không có request `PENDING`/`APPROVED` khác đang giữ chỗ (xem mục 2.1) |
| `PENDING` | `POST /{id}/approve` | `APPROVED` | Manager (đúng phòng ban) | Request chưa được xử lý trước đó |
| `PENDING` | `POST /{id}/reject` | `REJECTED` | Manager (đúng phòng ban) | Bắt buộc kèm `rejectionReason` |
| `PENDING` | `POST /{id}/cancel` | `CANCELLED` | Employee (chính chủ request) | Chỉ khi `status = PENDING` — **không huỷ được khi đã `APPROVED`** |
| `APPROVED` | `POST /{id}/fulfill` | `FULFILLED` | IT Staff (chi nhánh tương ứng) | — |
| `REJECTED`, `FULFILLED`, `CANCELLED` | *(bất kỳ action nào)* | — | — | **Terminal state** — mọi action tiếp theo trả lỗi `409 Conflict` (`REQUEST_ALREADY_PROCESSED`) |

> **Quan trọng:** `APPROVED` **không thể** chuyển trực tiếp về `REJECTED` hay `CANCELLED` — một khi Manager đã duyệt, chỉ còn đường đi tới `FULFILLED` (do IT Staff thực hiện). Nếu cần "huỷ" một request đã duyệt nhưng chưa fulfill, quy trình hiện tại **không có action riêng** cho trường hợp này — xem TODO mục 11.

### 1.3 Side-effect khi `fulfill()` — Loại `ASSIGN`

Khi `RequestServiceImpl.fulfill()` được gọi với `request.type = ASSIGN` và `request.status = APPROVED`:

```
1. Validate: asset.status phải đang AVAILABLE (double-check, phòng trường hợp race condition)
2. Tạo mới 1 dòng asset_assignment_history:
   - asset_id = request.asset_id
   - employee_id = request.employee_id
   - request_id = request.id
   - assigned_at = now()
   - returned_at = NULL
3. Cập nhật asset:
   - status = ASSIGNED
   - assigned_to = request.employee_id
4. Cập nhật request:
   - status = FULFILLED
   - fulfilled_by = current IT Staff employee_id
   - fulfilled_at = now()
5. Gửi notification (Email + In-app) tới Employee — xem mục 8
```

### 1.4 Side-effect khi `fulfill()` — Loại `RETURN`

Khi `RequestServiceImpl.fulfill()` được gọi với `request.type = RETURN` và `request.status = APPROVED`:

```
1. Validate: asset.assigned_to phải đúng bằng request.employee_id (toàn vẹn dữ liệu)
2. Tìm dòng asset_assignment_history đang mở (asset_id khớp, returned_at IS NULL)
   → Cập nhật returned_at = now()
3. Cập nhật asset:
   - status = AVAILABLE
   - assigned_to = NULL
4. Cập nhật request:
   - status = FULFILLED
   - fulfilled_by = current IT Staff employee_id
   - fulfilled_at = now()
5. Gửi notification (Email + In-app) tới Employee
```

> Toàn bộ block trên thực hiện trong **1 transaction duy nhất** (`@Transactional` ở tầng Service) — nếu bất kỳ bước nào lỗi, rollback toàn bộ, không để dữ liệu nửa vời (VD: `asset.status` đã đổi nhưng `request.status` chưa cập nhật).

### 1.5 Quy tắc tạo Request — Validate theo Type

| Type | Điều kiện asset hợp lệ |
|---|---|
| `ASSIGN` | `asset.status = AVAILABLE` **VÀ** asset không có request `PENDING`/`APPROVED` nào khác đang mở (xem mục 2.1) |
| `RETURN` | `asset.assigned_to = employee_id` (Employee **chỉ được chọn từ thiết bị chính mình đang giữ**) — Service validate tường minh, không tin tưởng `assetId` client gửi lên một cách mù quáng |

```java
// Trích đoạn validate khi tạo request RETURN
if (request.getType() == RequestType.RETURN) {
    Asset asset = assetRepository.findById(request.getAssetId())
        .orElseThrow(() -> new AssetNotFoundException(request.getAssetId()));
    if (!Objects.equals(asset.getAssignedTo(), currentEmployeeId)) {
        throw new BusinessException(
            "REQUEST_ASSET_NOT_ASSIGNED_TO_YOU",
            "Bạn không thể yêu cầu trả thiết bị không phải do mình đang giữ"
        );
    }
}
```

## 2. Request Workflow — Edge Cases

### 2.1 Asset "đã giữ chỗ" bởi request đang chờ xử lý

**Quy tắc:** Asset đang có **bất kỳ** request nào ở trạng thái `PENDING` hoặc `APPROVED` (chưa `fulfill`) được coi như **đã giữ chỗ** — dù `asset.status` trong DB vẫn còn `AVAILABLE` (vì status DB chỉ đổi thật sự khi `fulfill`, xem mục 1.3).

→ Asset này **không được hiển thị** trong danh sách `AVAILABLE` khi Employee khác tạo request `ASSIGN` mới, và Service **chặn tạo request trùng** với lỗi nghiệp vụ rõ ràng:

```java
// AssetNotAvailableException — ném ra khi cố tạo request ASSIGN cho asset đã "giữ chỗ"
if (requestRepository.existsActiveRequestForAsset(assetId)) {
    throw new AssetNotAvailableException(assetId);
}
```

```sql
-- Gợi ý query Repository (existsActiveRequestForAsset)
SELECT EXISTS (
    SELECT 1 FROM requests
    WHERE asset_id = :assetId
      AND status IN ('PENDING', 'APPROVED')
      AND deleted_at IS NULL
)
```

### 2.2 Request bị Reject

- Manager **bắt buộc** nhập `rejectionReason` khi gọi `POST /{id}/reject` — validate `@NotBlank` ở tầng DTO (`RejectRequestRequest`).
- Sau khi 1 request bị reject, Employee **được phép tạo lại request mới** cho **cùng asset đó** (không giới hạn số lần, không yêu cầu lý do request mới phải khác lý do cũ) — miễn là asset vẫn đáp ứng điều kiện ở mục 1.5.
- `REJECTED` là **terminal state** — không có action nào tác động thêm lên chính request đã bị reject đó.

### 2.3 IT Staff chủ động thu hồi (Force-return)

**Mục đích:** Xử lý case khẩn cấp (nhân viên nghỉ việc đột xuất, thiết bị cần thu hồi gấp) **mà không** đi qua workflow chuẩn `Employee tạo request RETURN → Manager duyệt`.

**Endpoint:** `POST /api/v1/assets/{id}/force-return` (xem `04-API.md` mục 11).

**Quy tắc:**
- Chỉ `IT_STAFF` (chi nhánh tương ứng) hoặc `ADMIN` (override) được gọi.
- **Bắt buộc** kèm `reason` trong request body — validate `@NotBlank`.
- **Bỏ qua hoàn toàn** state machine của `requests` — đây là thao tác trực tiếp lên `Asset`, **không tạo bản ghi `requests`** mới.
- Side-effect tương tự fulfill `RETURN` (mục 1.4) nhưng **không gắn `request_id`** vào `asset_assignment_history` (vì không xuất phát từ request nào):

```java
@Override
@Transactional
public AssetResponse forceReturn(Long assetId, String reason, Long itStaffId) {
    Asset asset = assetRepository.findById(assetId)
        .orElseThrow(() -> new AssetNotFoundException(assetId));

    if (asset.getAssignedTo() == null) {
        throw new BusinessException("ASSET_NOT_ASSIGNED", "Thiết bị hiện không được cấp phát cho ai");
    }

    // Đóng dòng lịch sử đang mở
    assetAssignmentHistoryRepository
        .findOpenAssignment(assetId)
        .ifPresent(history -> {
            history.setReturnedAt(Instant.now());
            assetAssignmentHistoryRepository.save(history);
        });

    Long previousHolder = asset.getAssignedTo();
    asset.setStatus(AssetStatus.AVAILABLE);
    asset.setAssignedTo(null);
    assetRepository.save(asset);

    log.info("Force-return executed: assetId={}, previousHolder={}, byItStaff={}, reason={}",
        assetId, previousHolder, itStaffId, reason);

    // Notification cho nhân viên trước đó đang giữ thiết bị
    notificationService.notify(previousHolder, NotificationType.ASSET_FORCE_RETURNED,
        "Thiết bị " + asset.getCode() + " đã được IT thu hồi: " + reason);

    return assetMapper.toResponse(asset);
}
```

### 2.4 Quy tắc "1 request = 1 asset"

Mỗi `request` chỉ tham chiếu **đúng 1** `asset_id` (đã chốt từ thiết kế DB — xem `05-DATABASE.md` mục 5.9). Nếu Employee cần yêu cầu cấp phát **nhiều thiết bị cùng lúc**, Frontend phải tạo **nhiều request riêng biệt** (1 request/asset) — Backend **không hỗ trợ** batch request trong 1 lần gọi API.

## 3. Maintenance Rules

### 3.1 Đồng bộ trạng thái Asset ↔ Maintenance

| Maintenance Status | Asset Status tương ứng |
|---|---|
| `SCHEDULED` | Không đổi `asset.status` (chỉ là lịch dự kiến) |
| `IN_PROGRESS` | **Tự động chuyển** `asset.status = IN_MAINTENANCE` (giữ nguyên `assigned_to` nếu có) |
| `COMPLETED` | **Tự động khôi phục** `asset.status` về trạng thái trước đó: `ASSIGNED` nếu `assigned_to IS NOT NULL`, ngược lại `AVAILABLE` |
| `CANCELLED` | **Tự động khôi phục** `asset.status` tương tự `COMPLETED` (nếu maintenance đã từng ở `IN_PROGRESS` trước khi bị huỷ) |

```java
@Override
@Transactional
public MaintenanceRecordResponse updateStatus(Long maintenanceId, MaintenanceStatus newStatus) {
    MaintenanceRecord record = maintenanceRepository.findById(maintenanceId)
        .orElseThrow(() -> new MaintenanceRecordNotFoundException(maintenanceId));

    record.setStatus(newStatus);
    if (newStatus == MaintenanceStatus.COMPLETED) {
        record.setCompletedDate(LocalDate.now());
    }
    maintenanceRepository.save(record);

    Asset asset = assetRepository.findById(record.getAssetId())
        .orElseThrow(() -> new AssetNotFoundException(record.getAssetId()));

    if (newStatus == MaintenanceStatus.IN_PROGRESS) {
        asset.setStatus(AssetStatus.IN_MAINTENANCE);
    } else if (newStatus == MaintenanceStatus.COMPLETED || newStatus == MaintenanceStatus.CANCELLED) {
        asset.setStatus(asset.getAssignedTo() != null ? AssetStatus.ASSIGNED : AssetStatus.AVAILABLE);
    }
    assetRepository.save(asset);

    log.info("Maintenance status updated: maintenanceId={}, newStatus={}, assetId={}, assetNewStatus={}",
        maintenanceId, newStatus, asset.getId(), asset.getStatus());

    return maintenanceMapper.toResponse(record);
}
```

### 3.2 Bảo trì đột xuất trên Asset đang `ASSIGNED`

**Quy tắc quan trọng:** Nếu một asset đang `ASSIGNED` (có người giữ) cần bảo trì đột xuất (VD: lỗi phần cứng khi đang dùng), IT Staff **tạo `maintenance_record` trực tiếp** mà **KHÔNG** yêu cầu Employee phải tạo request `RETURN` trước.

→ Khi `maintenance_record.status` chuyển sang `IN_PROGRESS`, `asset.status` chuyển `ASSIGNED → IN_MAINTENANCE` nhưng **`asset.assigned_to` giữ nguyên giá trị cũ** (không bị xoá) — phản ánh đúng thực tế nghiệp vụ: nhân viên vẫn là người chịu trách nhiệm thiết bị, chỉ tạm thời thiết bị đang được sửa.

### 3.3 Asset `IN_MAINTENANCE` bị loại khỏi danh sách AVAILABLE

Asset có `status = IN_MAINTENANCE` **không hiển thị** trong danh sách thiết bị `AVAILABLE` (dùng để Employee chọn khi tạo request `ASSIGN` mới) — nhất quán với nguyên tắc chỉ asset thực sự sẵn sàng mới được cấp phát.

## 4. Audit Rules — State Machine & Lifecycle

### 4.1 Sơ đồ trạng thái Audit Session

```
┌──────────────┐   started_at = now()    ┌──────────────┐
│ (chưa tồn tại) │ ───────────────────► │ IN_PROGRESS    │
└──────────────┘   expires_at = +3 ngày  └──────┬───────┘
                                                  │
                          ┌───────────────────────┼───────────────────────┐
                          │ IT Staff gọi              │ Scheduler job phát hiện   │
                          │ POST /{id}/complete         │ expires_at < now()        │
                          ▼                          ▼                          │
                   ┌──────────────┐          ┌──────────────┐                │
                   │  COMPLETED     │          │  COMPLETED     │ ◄──────────────┘
                   │ (thủ công)     │          │ (tự động — xem mục 4.4)         │
                   └──────────────┘          └──────────────┘
```

### 4.2 Phạm vi 1 Audit Session

- 1 `audit_session` = phạm vi **đúng 1 chi nhánh** (`branch_id` bắt buộc khi tạo).
- `IT_STAFF` chỉ tạo được audit session cho **chi nhánh của chính mình**; `ADMIN` có thể tạo cho bất kỳ chi nhánh nào (override).
- Không giới hạn số lượng audit session đồng thời theo chi nhánh trong thiết kế hiện tại, nhưng **về mặt nghiệp vụ thông thường** chỉ nên có 1 session `IN_PROGRESS` tại 1 thời điểm cho mỗi chi nhánh (không có ràng buộc DB cứng cho việc này — xem TODO mục 11 nếu cần enforce).

### 4.3 Luồng quét QR (`POST /{id}/scan`)

```
1. IT Staff quét QR (qua html5-qrcode trên web, camera điện thoại) → lấy được asset.code
2. POST /api/v1/audits/{auditSessionId}/scan { assetCode, scannedLocation }
3. Backend:
   a. Tìm asset theo code → nếu không tồn tại, trả lỗi ASSET_NOT_FOUND
   b. Validate asset.branch_id == audit_session.branch_id (không cho quét nhầm asset chi nhánh khác)
   c. Tạo audit_scans record (audit_session_id, asset_id, scanned_by, scanned_location, scanned_at)
   d. Nếu scanned_location khác với "vị trí kỳ vọng" của asset (nếu hệ thống có lưu vị trí kỳ vọng)
      → CÓ THỂ phát sinh discrepancy LOCATION_MISMATCH ngay tại thời điểm quét (xem ghi chú dưới)
4. Trả về kết quả quét (asset info + cảnh báo nếu có mismatch)
```

> **Ghi chú quan trọng:** Schema hiện tại (`05-DATABASE.md`) **không có cột lưu "vị trí kỳ vọng" cố định** trên bảng `assets` (chỉ có `scanned_location` ghi nhận tại thời điểm quét trong `audit_scans`, và `expected_location`/`actual_location` trên `discrepancies` được điền **khi resolve**, không phải khi scan). Do đó, discrepancy loại `LOCATION_MISMATCH` ở MVP hiện tại **được tạo thủ công bởi IT Staff** nếu họ nhận thấy vị trí không khớp với kỳ vọng thực tế (qua kinh nghiệm/biên bản giấy), **không có cơ chế tự động phát hiện LOCATION_MISMATCH qua hệ thống** — chỉ có `MISSING` mới được **tự động** tạo (xem mục 4.4). Đây là điểm cần làm rõ thêm — xem TODO mục 11.

### 4.4 Hoàn tất Audit Session (`complete()`) — Tự động phát hiện MISSING

```java
@Override
@Transactional
public AuditSessionResponse complete(Long auditSessionId) {
    AuditSession session = auditSessionRepository.findById(auditSessionId)
        .orElseThrow(() -> new AuditSessionNotFoundException(auditSessionId));

    if (session.getStatus() == AuditSessionStatus.COMPLETED) {
        throw new BusinessException("AUDIT_SESSION_ALREADY_COMPLETED", "Phiên kiểm kê đã hoàn tất trước đó");
    }

    // Toàn bộ asset thuộc chi nhánh (chưa soft-delete, không tính DISPOSED)
    List<Asset> branchAssets = assetRepository.findActiveAssetsByBranch(session.getBranchId());

    // Asset đã được quét trong session này
    Set<Long> scannedAssetIds = auditScanRepository.findAssetIdsBySession(auditSessionId);

    // Asset chưa quét → tự động tạo discrepancy MISSING
    for (Asset asset : branchAssets) {
        if (!scannedAssetIds.contains(asset.getId())) {
            Discrepancy discrepancy = Discrepancy.builder()
                .auditSessionId(auditSessionId)
                .assetId(asset.getId())
                .type(DiscrepancyType.MISSING)
                .status(DiscrepancyStatus.OPEN)
                .build();
            discrepancyRepository.save(discrepancy);

            log.warn("Discrepancy MISSING created: auditSessionId={}, assetId={}, assetCode={}",
                auditSessionId, asset.getId(), asset.getCode());

            notificationService.notify(/* IT Staff phụ trách chi nhánh */ ...,
                NotificationType.DISCREPANCY_FOUND,
                "Phát hiện thiết bị " + asset.getCode() + " không quét được trong kiểm kê");
        }
    }

    session.setStatus(AuditSessionStatus.COMPLETED);
    session.setCompletedAt(Instant.now());
    auditSessionRepository.save(session);

    log.info("Audit session completed: id={}, branchId={}, totalAssets={}, missingCount={}",
        auditSessionId, session.getBranchId(), branchAssets.size(),
        branchAssets.size() - scannedAssetIds.size());

    return auditSessionMapper.toResponse(session);
}
```

### 4.5 Auto-expire sau 3 ngày

- Mỗi `audit_session` khi tạo có `expires_at = started_at + 3 ngày` (gán tự động ở tầng Service khi tạo, không phải input từ client).
- Spring Scheduler job (`@Scheduled`, định nghĩa tại `common/config/SchedulerConfig.java`) quét định kỳ (gợi ý: mỗi giờ — xem `01-ARCHITECTURE.md` mục 6) các session thoả `status = IN_PROGRESS AND expires_at < now()`.
- Với mỗi session quá hạn tìm được, job **gọi lại đúng method `complete()`** ở mục 4.4 — đảm bảo logic tự động phát hiện MISSING **nhất quán giữa complete thủ công và complete tự động**, không viết logic trùng lặp.

```java
@Scheduled(cron = "0 0 * * * *") // mỗi giờ, phút 0
@Transactional
public void autoExpireAuditSessions() {
    List<AuditSession> expiredSessions = auditSessionRepository
        .findByStatusAndExpiresAtBefore(AuditSessionStatus.IN_PROGRESS, Instant.now());

    for (AuditSession session : expiredSessions) {
        log.info("Auto-expiring audit session: id={}, branchId={}", session.getId(), session.getBranchId());
        auditService.complete(session.getId());
    }
}
```

### 4.6 Resolve Discrepancy — Hành động thủ công bắt buộc

Discrepancy loại `MISSING` **không tự động** chuyển sang `RESOLVED` — **bắt buộc IT Staff xử lý thủ công** qua `POST /api/v1/audits/discrepancies/{id}/resolve` với 1 trong 2 `action`:

| Action | Hiệu ứng lên `asset.status` | Hiệu ứng lên `discrepancy` |
|---|---|---|
| `CONFIRM_LOST` | Chuyển `asset.status = LOST` | `status = RESOLVED`, `resolution_action = CONFIRM_LOST`, `resolved_by`, `resolved_at` được set |
| `FOUND` | **Giữ nguyên** `asset.status` hiện tại (không đổi) | `status = RESOLVED`, `resolution_action = FOUND`, `resolved_by`, `resolved_at` được set |

> `action` chỉ áp dụng hợp lệ cho discrepancy `type = MISSING`. Discrepancy `type = LOCATION_MISMATCH` hoặc `UNEXPECTED_FOUND` cũng đi qua **cùng 1 endpoint** `resolve` nhưng **không** kích hoạt thay đổi `asset.status` — chỉ đơn thuần đánh dấu `status = RESOLVED` (vì 2 loại này không ảnh hưởng tới tình trạng tồn tại vật lý của asset, chỉ là ghi nhận sai lệch thông tin).

```java
@Override
@Transactional
public DiscrepancyResponse resolve(Long discrepancyId, ResolveDiscrepancyRequest request, Long resolverId) {
    Discrepancy discrepancy = discrepancyRepository.findById(discrepancyId)
        .orElseThrow(() -> new DiscrepancyNotFoundException(discrepancyId));

    if (discrepancy.getStatus() == DiscrepancyStatus.RESOLVED) {
        throw new BusinessException("DISCREPANCY_ALREADY_RESOLVED", "Sai lệch này đã được xử lý trước đó");
    }

    if (discrepancy.getType() == DiscrepancyType.MISSING) {
        if (request.getAction() == ResolutionAction.CONFIRM_LOST) {
            Asset asset = assetRepository.findById(discrepancy.getAssetId())
                .orElseThrow(() -> new AssetNotFoundException(discrepancy.getAssetId()));
            asset.setStatus(AssetStatus.LOST);
            assetRepository.save(asset);
        }
        // action == FOUND → không đổi gì trên Asset
        discrepancy.setResolutionAction(request.getAction());
    }

    discrepancy.setStatus(DiscrepancyStatus.RESOLVED);
    discrepancy.setResolvedBy(resolverId);
    discrepancy.setResolvedAt(Instant.now());
    discrepancyRepository.save(discrepancy);

    log.info("Discrepancy resolved: id={}, type={}, action={}, resolverId={}",
        discrepancyId, discrepancy.getType(), request.getAction(), resolverId);

    return discrepancyMapper.toResponse(discrepancy);
}
```

## 5. Asset Code Generation

**Format:** `<BRANCH_CODE>-<CATEGORY_CODE>-<SEQUENCE>`

**Ví dụ:** `HN-LAP-0001` (Chi nhánh Hà Nội, danh mục Laptop, số thứ tự 0001)

| Thành phần | Nguồn | Ghi chú |
|---|---|---|
| `BRANCH_CODE` | `branches.code` | VD: `HN`, `HCM`, `DN` |
| `CATEGORY_CODE` | `categories.code` | VD: `LAP`, `MON`, `PHN` |
| `SEQUENCE` | Tự sinh, tăng dần, **4 chữ số, zero-padded** | **Tăng riêng theo từng cặp `branch_id` + `category_id`** — KHÔNG dùng chung 1 sequence toàn hệ thống |

**Quy tắc sinh sequence:**

```sql
-- Gợi ý query lấy sequence tiếp theo (Repository)
SELECT COALESCE(MAX(
    CAST(SUBSTRING(code FROM '\d+$') AS INTEGER)
), 0) + 1
FROM assets
WHERE branch_id = :branchId AND category_id = :categoryId
```

> ⚠️ **Lưu ý race condition:** Trong môi trường nhiều IT Staff tạo asset đồng thời, cách tính `MAX(sequence) + 1` thuần tuý có rủi ro **trùng `code`** nếu 2 transaction đọc cùng giá trị `MAX` trước khi cả hai cùng commit. Vì `assets.code` có ràng buộc `UNIQUE` ở DB (`05-DATABASE.md` mục 5.7), trường hợp trùng sẽ khiến transaction sau **fail ở tầng DB** (constraint violation) — Service nên **bắt lỗi này và tự động retry sinh code mới** (tối đa vài lần) thay vì để lỗi 500 lộ ra cho người dùng. Đây là pattern khuyến nghị bổ sung (best practice), không phải đặc tả tường minh từ nghiên cứu gốc.

```java
@Override
@Transactional
public AssetResponse create(CreateAssetRequest request) {
    Branch branch = branchRepository.findById(request.getBranchId())
        .orElseThrow(() -> new BranchNotFoundException(request.getBranchId()));
    Category category = categoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));

    String code = generateAssetCode(branch.getCode(), category.getCode(),
        request.getBranchId(), request.getCategoryId());

    Asset asset = assetMapper.toEntity(request);
    asset.setCode(code);
    asset.setStatus(AssetStatus.AVAILABLE);

    Asset saved = assetRepository.save(asset);
    log.info("Asset created: code={}, branchId={}, categoryId={}", code, branch.getId(), category.getId());
    return assetMapper.toResponse(saved);
}

private String generateAssetCode(String branchCode, String categoryCode, Long branchId, Long categoryId) {
    int nextSequence = assetRepository.findNextSequence(branchId, categoryId);
    return "%s-%s-%04d".formatted(branchCode, categoryCode, nextSequence);
}
```

## 6. Validation Rules tổng hợp

| Field | Bảng | Quy tắc |
|---|---|---|
| `value` | `assets` | Phải `> 0`, đơn vị **VNĐ** (CHECK constraint ở DB + validation ở DTO) |
| `purchase_date` | `assets` | **Không được là ngày tương lai** (`<= CURRENT_DATE`, CHECK constraint ở DB) |
| `code` | `assets` | Tự sinh (xem mục 5) — **client không được gửi field này** khi tạo |
| `code` | `branches` | Bắt buộc nhập tay, **unique**, VD: `HN`, `HCM`, `DN` |
| `code` | `categories` | Bắt buộc nhập tay, **unique** |
| `email` | `employees` | Bắt buộc, **unique**, dùng làm username đăng nhập, format email hợp lệ (`@Email`) |
| `rejection_reason` | `requests` | Bắt buộc khi `reject()` (`@NotBlank` ở DTO `RejectRequestRequest`) |
| `reason` | `force-return` | Bắt buộc (`@NotBlank` ở DTO `ForceReturnRequest`) |

**Validation error trả về:** Toàn bộ lỗi validation trong **cùng 1 request** được gộp và trả về **đồng thời** trong mảng `errors` (không dừng ở lỗi đầu tiên) — chi tiết format và HTTP status mapping: `09-ERROR-CODES.md`.

## 7. Offboarding Nhân viên

Khi Admin/IT Staff thực hiện soft-delete 1 `employee` (`DELETE /api/v1/employees/{id}`):

```
1. Service kiểm tra: employee này có đang assigned_to bất kỳ asset nào không?
   SELECT COUNT(*) FROM assets WHERE assigned_to = :employeeId AND deleted_at IS NULL
2. Nếu COUNT > 0:
   → KHÔNG chặn việc soft-delete (vẫn cho phép xoá employee)
   → NHƯNG trả về cảnh báo rõ ràng trong response (danh sách asset đang bị giữ)
   → IT Staff phải TỰ xử lý thu hồi qua force-return SAU KHI hoặc TRƯỚC KHI xoá employee
3. Soft-delete employee (deleted_at = now())
4. Revoke toàn bộ refresh_tokens của employee này (xem 06-AUTHENTICATION.md mục 2.3)
```

> **Lưu ý:** Hệ thống **không tự động force-return** các asset khi offboarding — đây là quyết định nghiệp vụ có chủ đích (tránh tự động hoá quá mức khi IT Staff cần tự xác nhận tình trạng thực tế từng thiết bị trước khi thu hồi, VD: thiết bị có thể đã hỏng/mất chứ không đơn thuần "trả lại kho"). Hệ thống chỉ đóng vai trò **cảnh báo**, hành động cụ thể (`force-return`) vẫn là thao tác riêng IT Staff chủ động thực hiện.

**Response mẫu khi xoá employee đang giữ thiết bị:**

```json
{
  "success": true,
  "data": {
    "id": 12,
    "deletedAt": "2026-06-30T10:00:00Z",
    "warning": {
      "code": "EMPLOYEE_HAS_ASSIGNED_ASSETS",
      "message": "Nhân viên này đang giữ 2 thiết bị, vui lòng thu hồi qua chức năng Force-return",
      "assignedAssets": [
        { "id": 42, "code": "HN-LAP-0001", "name": "Dell Latitude 5440" },
        { "id": 58, "code": "HN-MON-0003", "name": "Dell UltraSharp 27" }
      ]
    }
  },
  "meta": { "timestamp": "2026-06-30T10:00:00Z" }
}
```

## 8. Notification System

### 8.1 Nguyên tắc phân loại kênh: "Chứng từ vs Thông báo nhanh"

| Sự kiện | Kênh | Lý do |
|---|---|---|
| Request mới được tạo | In-app | Thông báo nhanh nội bộ, Manager đang dùng hệ thống thường xuyên |
| Request được duyệt | In-app | Tương tự — không cần "bằng chứng" qua email |
| Request bị từ chối (kèm lý do) | **Email + In-app** | Cần lưu vết rõ ràng, Employee có thể cần tham khảo lại lý do sau này |
| Request được fulfill | **Email + In-app** | Đóng vai trò "chứng từ bàn giao" thiết bị — cần bằng chứng email |
| Tài khoản mới / Reset password | **Email only** | User **chưa đăng nhập được** nên không thể xem in-app — Email là kênh duy nhất khả thi |
| Discrepancy mới phát hiện | **Email + In-app** | Vấn đề nghiêm trọng (nghi ngờ mất thiết bị), cần đảm bảo IT Staff không bỏ sót |
| Nhắc nhở audit session sắp đến hạn | In-app | Thông báo định kỳ nội bộ, không cần mức độ "chứng từ" |
| Thiết bị bị force-return | In-app *(suy luận hợp lý — xem ghi chú)* | Tương tự nhóm thông báo nhanh nội bộ |

> ⚠️ **Ghi chú:** Sự kiện "Thiết bị bị force-return" (mục 2.3) **không được liệt kê tường minh** trong bảng Notification System gốc (Chủ đề 9, `nghien_cuu.md`) — được suy luận hợp lý dựa theo cùng nguyên tắc phân loại kênh đã áp dụng cho các sự kiện tương tự (không phải "chứng từ" quan trọng như fulfill, chỉ là thông báo nội bộ). Đánh dấu **TODO: Need confirmation** nếu cần điều chỉnh kênh (VD: nâng lên Email + In-app nếu xem đây là tình huống cần lưu vết).

### 8.2 Email Service (SendGrid)

- **Free tier**, nội dung **plain text đơn giản** (không cần HTML template phức tạp ở MVP).
- Gửi **bất đồng bộ** (`@Async`) — không block luồng xử lý chính của Request/Service gọi nó.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final SendGridClient sendGridClient;

    @Async
    public void sendEmail(String toEmail, String subject, String plainTextBody) {
        try {
            sendGridClient.send(toEmail, subject, plainTextBody);
            log.info("Email sent successfully: to={}, subject={}", toEmail, subject);
        } catch (SendGridException e) {
            // Lỗi gửi email KHÔNG được làm fail luồng nghiệp vụ chính (đã chạy async, tách biệt transaction)
            log.error("Failed to send email: to={}, subject={}", toEmail, subject, e);
        }
    }
}
```

> **Quan trọng:** Vì chạy `@Async`, lỗi gửi email (SendGrid downtime, sai cấu hình...) **không được phép** làm rollback transaction nghiệp vụ chính (VD: `fulfill()` request vẫn thành công dù email thông báo gửi thất bại) — chỉ log `ERROR` để theo dõi, không ném exception ngược lên luồng gọi.

### 8.3 In-app Notification

- Bảng `notifications` (xem `05-DATABASE.md` mục 5.15).
- **Cơ chế: Polling đơn giản** — **không dùng WebSocket** (nhất quán với ADR #5 tại `01-ARCHITECTURE.md` mục 11).
- Frontend gọi định kỳ `GET /api/v1/notifications/unread-count` mỗi **30-60 giây** để cập nhật badge số lượng chưa đọc trên UI (chuông thông báo ở Top Navbar — xem `08-UI-UX.md`).
- Độ trễ thông báo tối đa chấp nhận được: **30-60 giây** — hệ thống không phải real-time-critical.

```java
@Override
public void notify(Long employeeId, NotificationType type, String message) {
    Notification notification = Notification.builder()
        .employeeId(employeeId)
        .type(type.name())
        .message(message)
        .isRead(false)
        .build();
    notificationRepository.save(notification);
    log.debug("In-app notification created: employeeId={}, type={}", employeeId, type);
}
```

### 8.4 Bảng tổng hợp `related_entity_id` theo loại Notification

| `NotificationType` | `related_entity_id` trỏ tới |
|---|---|
| `REQUEST_CREATED`, `REQUEST_APPROVED`, `REQUEST_REJECTED`, `REQUEST_FULFILLED` | `requests.id` |
| `DISCREPANCY_FOUND` | `discrepancies.id` |
| `AUDIT_REMINDER` | `audit_sessions.id` (hoặc `NULL` nếu là nhắc nhở chung chưa gắn session cụ thể) |

→ Frontend dùng `related_entity_id` kết hợp `type` để build deep-link điều hướng khi người dùng click vào notification (VD: click vào `REQUEST_APPROVED` → điều hướng tới `RequestDetailPage` của `requests.id` tương ứng).

## 9. Quy tắc Soft Delete liên ngành

Bổ sung chi tiết nghiệp vụ cho nguyên tắc đã nêu ở `05-DATABASE.md` mục 7:

| Entity bị xoá | Có chặn nếu còn liên kết active? | Ghi chú |
|---|---|---|
| `branch` | **Có** (best practice bổ sung — xem `05-DATABASE.md` mục 7, cần Tech Lead xác nhận) | Phải đảm bảo không còn `employees`/`assets` active gắn `branch_id` này |
| `category` | **Có** (suy luận hợp lý theo cùng nguyên tắc) | Không cho xoá nếu còn `assets` active dùng `category_id` này — nếu không, asset cũ sẽ "mồ côi" category |
| `department` | **Có** (suy luận hợp lý theo cùng nguyên tắc) | Không cho xoá nếu còn `employees` active gắn `department_id` này |
| `employee` | **Không chặn**, chỉ cảnh báo (xem mục 7) | Khác biệt có chủ đích — vì offboarding là tình huống nghiệp vụ thường xuyên, không nên bị chặn cứng |
| `asset` | **Không chặn theo thiết kế hiện tại** | Asset có thể bị soft-delete ở bất kỳ trạng thái nào (VD: đã `DISPOSED`) — không có ràng buộc nghiệp vụ nào yêu cầu chặn thêm |

> ⚠️ Toàn bộ dòng có ghi "suy luận hợp lý" trong bảng trên **chưa được xác nhận tường minh** trong nghiên cứu gốc — chỉ riêng dòng `employee` (mục 7) là có đặc tả rõ ràng từ Chủ đề 9. Các dòng còn lại áp dụng nhất quán theo cùng nguyên tắc "an toàn, tránh xoá nhầm hàng loạt" đã nêu ở `05-DATABASE.md`, nhưng cần Tech Lead xác nhận riêng — xem TODO mục 11.

## 10. Bảng tổng hợp Business Exception

| Exception | Mã lỗi | HTTP Status | Khi nào ném |
|---|---|---|---|
| `AssetNotAvailableException` | `ASSET_NOT_AVAILABLE` | 409 | Tạo request `ASSIGN` cho asset đã giữ chỗ/không `AVAILABLE` |
| `AssetNotFoundException` | `ASSET_NOT_FOUND` | 404 | Asset ID không tồn tại |
| `RequestAlreadyProcessedException` | `REQUEST_ALREADY_PROCESSED` | 409 | Action lên request đã ở terminal state |
| `RequestAssetNotAssignedToYouException` | `REQUEST_ASSET_NOT_ASSIGNED_TO_YOU` | 403 | Employee tạo request `RETURN` cho asset không phải mình giữ |
| `EmployeeCannotAssignAdminRoleException` | `EMPLOYEE_CANNOT_ASSIGN_ADMIN_ROLE` | 403 | IT_STAFF cố tạo/sửa employee role `ADMIN` |
| `AuditSessionAlreadyCompletedException` | `AUDIT_SESSION_ALREADY_COMPLETED` | 409 | Gọi `complete()` 2 lần |
| `DiscrepancyAlreadyResolvedException` | `DISCREPANCY_ALREADY_RESOLVED` | 409 | Resolve 1 discrepancy đã `RESOLVED` |
| `AssetNotAssignedException` | `ASSET_NOT_ASSIGNED` | 409 | Force-return asset đang không có người giữ (`assigned_to IS NULL`) |

→ Danh sách đầy đủ tất cả mã lỗi theo từng module (kể cả lỗi hệ thống, validation...): `09-ERROR-CODES.md`.

## 11. TODO / Open Questions

> TODO: Need confirmation — **Auto-approve theo ngưỡng giá trị thấp / trường hợp khẩn cấp**: workflow duyệt hiện tại **bắt buộc qua Manager cho mọi request**, không có cơ chế bỏ qua duyệt (VD: tự động `APPROVED` nếu `asset.value < X VNĐ`, hoặc cờ "khẩn cấp" cho phép IT Staff tự fulfill mà không cần Manager). Đây là điểm mở rộng tiềm năng đã ghi nhận từ `00-OVERVIEW.md` mục 12.

> TODO: Need confirmation — **Huỷ request đã `APPROVED` nhưng chưa `fulfill`**: state machine hiện tại (mục 1.2) không có đường đi từ `APPROVED` quay lại `REJECTED`/`CANCELLED`. Nếu phát sinh nhu cầu nghiệp vụ thực tế (VD: Manager duyệt nhầm, cần thu hồi quyết định trước khi IT Staff fulfill), cần bổ sung action mới (VD: `POST /{id}/revoke-approval`) — hiện chưa có trong đặc tả gốc.

> TODO: Need confirmation — **Cơ chế phát hiện `LOCATION_MISMATCH` tự động**: như phân tích ở mục 4.3, schema hiện tại không lưu "vị trí kỳ vọng" cố định trên `assets`, nên discrepancy loại này hiện chỉ tạo được **thủ công**. Cần xác nhận: có cần bổ sung cột `assets.expected_location` để hỗ trợ so sánh tự động khi quét QR hay không.

> TODO: Need confirmation — **Giới hạn 1 audit session `IN_PROGRESS` cho mỗi chi nhánh**: hiện tại không có ràng buộc kỹ thuật chặn việc tạo nhiều audit session đồng thời cho cùng 1 chi nhánh. Cần xác nhận có nên thêm validate chặn việc này hay để mở (cho phép trường hợp đặc biệt cần 2 đợt kiểm kê song song).

> TODO: Need confirmation — **Quy tắc chặn soft-delete `category`/`department` khi còn liên kết active** (mục 9) là suy luận hợp lý, chưa được xác nhận tường minh từ nghiên cứu gốc — chỉ riêng quy tắc cho `branch` và `employee` là có đặc tả rõ ràng.

> TODO: Need confirmation — **Kênh notification cho sự kiện "Thiết bị bị force-return"** (mục 8.1) là suy luận hợp lý, cần xác nhận lại mức độ ưu tiên (chỉ In-app hay cần thêm Email).

---

*Xem tiếp: `08-UI-UX.md` để biết chi tiết design system, màu sắc, typography và layout.*