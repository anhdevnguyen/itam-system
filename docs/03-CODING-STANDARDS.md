# 03 — CODING STANDARDS

> Quy ước đặt tên, logging, comment và ngôn ngữ hệ thống áp dụng cho toàn bộ codebase ITAM. AI Coding Agent phải tuân thủ nghiêm ngặt các quy ước này khi sinh code mới.

## Mục lục

1. [Naming Convention chung (Backend - Java)](#1-naming-convention-chung-backend---java)
2. [Naming theo loại Class (Backend)](#2-naming-theo-loại-class-backend)
3. [Naming Convention (Frontend - TypeScript/React)](#3-naming-convention-frontend---typescriptreact)
4. [Logging](#4-logging)
5. [Comment Convention](#5-comment-convention)
6. [Ngôn ngữ hệ thống](#6-ngôn-ngữ-hệ-thống)
7. [Best Practices bổ sung](#7-best-practices-bổ-sung)
8. [TODO / Open Questions](#8-todo--open-questions)

---

## 1. Naming Convention chung (Backend - Java)

| Loại | Convention | Ví dụ |
|---|---|---|
| Class / Interface | `PascalCase` | `AssetService`, `AssetController` |
| Method / Variable | `camelCase` | `getAssetById`, `assetName` |
| Constant | `UPPER_SNAKE_CASE` | `MAX_FILE_SIZE` |
| Package | `lowercase` (không gạch dưới, không camelCase) | `com.vanh.itam.asset` |
| DB table / column | `snake_case` | `asset_id`, `created_at` |

## 2. Naming theo loại Class (Backend)

| Loại | Convention | Ví dụ |
|---|---|---|
| Controller | `XxxController` | `AssetController` |
| Service | **Interface + Impl tách riêng** | `AssetService` (interface) + `AssetServiceImpl` |
| Repository | `XxxRepository` | `AssetRepository extends JpaRepository<Asset, Long>` |
| Entity | Số ít, không suffix | `Asset`, `Employee` (KHÔNG đặt `AssetEntity`) |
| DTO Request | `XxxRequest` | `CreateAssetRequest`, `UpdateAssetRequest` |
| DTO Response | `XxxResponse` | `AssetResponse` |
| Mapper | `XxxMapper` (MapStruct) | `AssetMapper` |
| Exception | `XxxException` / `XxxNotFoundException` | `AssetNotFoundException` |

**Ví dụ đầy đủ — Service Interface + Impl:**

```java
// AssetService.java
public interface AssetService {
    AssetResponse create(CreateAssetRequest request);
    AssetResponse getById(Long id);
    Page<AssetResponse> getAll(AssetFilter filter, Pageable pageable);
    AssetResponse update(Long id, UpdateAssetRequest request);
    void softDelete(Long id);
    void restore(Long id);
}

// AssetServiceImpl.java
@Service
@RequiredArgsConstructor
@Slf4j
public class AssetServiceImpl implements AssetService {
    private final AssetRepository assetRepository;
    private final AssetMapper assetMapper;

    @Override
    public AssetResponse create(CreateAssetRequest request) {
        log.info("Creating new asset: code={}", request.getCode());
        Asset asset = assetMapper.toEntity(request);
        Asset saved = assetRepository.save(asset);
        return assetMapper.toResponse(saved);
    }
    // ...
}
```

**Ví dụ Exception:**

```java
public class AssetNotFoundException extends ResourceNotFoundException {
    public AssetNotFoundException(Long id) {
        super("ASSET_NOT_FOUND", "Không tìm thấy thiết bị với ID: " + id);
    }
}
```

## 3. Naming Convention (Frontend - TypeScript/React)

| Loại | Convention | Ví dụ |
|---|---|---|
| Component | `PascalCase` | `AssetList.tsx` |
| Hook | `camelCase`, prefix `use` | `useAssets.ts` |
| Type / Interface | `PascalCase` | `Asset`, `AssetResponse` |
| File thường (util/service) | `camelCase` | `apiClient.ts` |
| Folder | `camelCase` | `assetRequests/` |

**Ví dụ Component:**

```tsx
// AssetList.tsx
interface AssetListProps {
  branchId: number;
}

export function AssetList({ branchId }: AssetListProps) {
  const { assets, isLoading } = useAssets(branchId);

  if (isLoading) return <SkeletonLoader rows={5} />;
  if (assets.length === 0) return <EmptyState message="Không có dữ liệu" />;

  return (
    <Table>
      {assets.map((asset) => (
        <AssetRow key={asset.id} asset={asset} />
      ))}
    </Table>
  );
}
```

**Ví dụ Hook:**

```ts
// useAssets.ts
export function useAssets(branchId: number) {
  const [assets, setAssets] = useState<Asset[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    apiClient
      .get<ApiResponse<Asset[]>>(`/api/v1/assets?branchId=${branchId}`)
      .then((res) => setAssets(res.data.data))
      .finally(() => setIsLoading(false));
  }, [branchId]);

  return { assets, isLoading };
}
```

**Ví dụ Type:**

```ts
// asset.types.ts
export interface Asset {
  id: number;
  code: string;
  name: string;
  status: AssetStatus;
  branchId: number;
  assignedTo: number | null;
}

export type AssetStatus =
  | 'AVAILABLE'
  | 'ASSIGNED'
  | 'IN_MAINTENANCE'
  | 'BROKEN'
  | 'DISPOSED'
  | 'LOST';
```

## 4. Logging

**Công cụ:** SLF4J + Lombok `@Slf4j` (Backend). Output dạng **JSON structured logging** ở production (xem `11-DEPLOYMENT.md`).

| Level | Khi dùng | Ví dụ |
|---|---|---|
| `ERROR` | Lỗi hệ thống không mong muốn | Lỗi DB, lỗi gọi Cloudinary thất bại |
| `WARN` | Bất thường nhưng vẫn xử lý được | Refresh token hết hạn, discrepancy khi quét QR |
| `INFO` | Business event quan trọng (audit trail) | Tạo yêu cầu, duyệt yêu cầu, cấp phát thành công, đăng nhập |
| `DEBUG` | Chi tiết kỹ thuật, tắt ở production | Input/output method, query params |

**Ví dụ áp dụng:**

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    @Override
    public RequestResponse approve(Long requestId, Long managerId) {
        log.debug("approve() called: requestId={}, managerId={}", requestId, managerId);

        Request request = requestRepository.findById(requestId)
            .orElseThrow(() -> new RequestNotFoundException(requestId));

        request.setStatus(RequestStatus.APPROVED);
        requestRepository.save(request);

        log.info("Request approved: requestId={}, managerId={}, employeeId={}",
            requestId, managerId, request.getEmployeeId());

        return requestMapper.toResponse(request);
    }
}
```

```java
} catch (CloudinaryException e) {
    log.error("Failed to upload image to Cloudinary: assetId={}", assetId, e);
    throw new BusinessException("ASSET_IMAGE_UPLOAD_FAILED", "Tải ảnh thất bại, vui lòng thử lại");
}
```

> **Quy tắc liên kết với Exception Handling:** `BusinessException` (lỗi nghiệp vụ có chủ đích, VD: `AssetNotAvailableException`) log ở mức `WARN`; lỗi hệ thống thực sự (DB fail, `NullPointerException`, lỗi gọi 3rd-party) log ở mức `ERROR`. Chi tiết: `09-ERROR-CODES.md`.

## 5. Comment Convention

- **Chỉ comment khi logic phức tạp** — không bắt buộc Javadoc cho toàn bộ class/method.
- Ưu tiên đặt tên method/biến rõ ràng, tự giải thích (self-documenting code) thay vì comment thừa.
- Khi cần comment, giải thích **"tại sao"** (why) thay vì **"cái gì"** (what) — code đã tự nói lên "cái gì".

```java
// ✅ Comment hợp lý — giải thích lý do nghiệp vụ không hiển nhiên
// Asset đang có request PENDING/APPROVED coi như đã giữ chỗ,
// không cho phép request mới dù status DB vẫn là AVAILABLE
if (requestRepository.existsActiveRequestForAsset(assetId)) {
    throw new AssetNotAvailableException(assetId);
}

// ❌ Comment thừa — không cần thiết, code đã tự giải thích
// Lấy asset theo id
Asset asset = assetRepository.findById(id);
```

## 6. Ngôn ngữ hệ thống

| Phạm vi | Ngôn ngữ |
|---|---|
| UI labels, button, menu | **Tiếng Việt** |
| Validation message | **Tiếng Việt** |
| Error message (hiển thị cho người dùng) | **Tiếng Việt** |
| Tên class/method/biến (code) | **Tiếng Anh** (chuẩn lập trình quốc tế) |
| Commit message | **Tiếng Anh** (Conventional Commits — xem `12-CONTRIBUTING.md`) |
| Comment trong code | Tiếng Việt hoặc Tiếng Anh đều chấp nhận, ưu tiên nhất quán trong cùng 1 file |

**Quan trọng:** MVP **chưa cần i18n/đa ngôn ngữ** — hardcode Tiếng Việt trực tiếp trong code (validation message, error message). Tuy nhiên, để **không chặn khả năng mở rộng i18n sau này**, khuyến nghị:

- Tập trung toàn bộ message Tiếng Việt vào 1 nơi dễ thay thế sau này (VD: properties file `messages_vi.properties` dù chưa có `messages_en.properties`), thay vì hardcode string rải rác khắp Service/Controller.
- Error code (`ASSET_NOT_FOUND`...) luôn bằng **tiếng Anh UPPER_SNAKE_CASE**, chỉ phần `message` hiển thị là Tiếng Việt — giúp tách biệt "mã lỗi cho máy" và "message cho người", là nền tảng i18n tự nhiên về sau.

**Ví dụ:**

```java
public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String errorCode, String message) {
        super(errorCode, HttpStatus.NOT_FOUND, message);
    }
}

throw new ResourceNotFoundException("ASSET_NOT_FOUND", "Không tìm thấy thiết bị");
```

## 7. Best Practices bổ sung

Các nguyên tắc sau **không được nghiên cứu gốc đặc tả riêng** nhưng là best practice tiêu chuẩn áp dụng nhất quán với stack đã chọn (Spring Boot + React/TS):

- **DTO tách biệt hoàn toàn khỏi Entity** — không bao giờ trả `Entity` trực tiếp ra Controller; luôn qua `Mapper` để map sang `Response` DTO, tránh lộ field nội bộ (`deletedAt`, mật khẩu hash...) và tránh vòng lặp serialize khi có quan hệ 2 chiều.
- **Validation tại tầng DTO** bằng Jakarta Bean Validation annotation (`@NotNull`, `@Size`, `@Email`...) — không validate thủ công trong Service trừ khi là business rule phức tạp (VD: "asset đang có request PENDING không được tạo request mới").
- **Transaction boundary ở tầng Service** — dùng `@Transactional` trên method Service thực hiện nhiều thao tác ghi DB liên quan (VD: `fulfill()` vừa update `Request`, vừa tạo `AssetAssignmentHistory`, vừa update `Asset`).
- **TypeScript strict mode** bật trong `tsconfig.json` — không dùng `any` tuỳ tiện, ưu tiên type rõ ràng cho mọi response API.
- **Không inline magic string/number** — dùng Enum (Backend) hoặc union type literal (Frontend) cho các giá trị cố định như `AssetStatus`, `RequestStatus`.

## 8. TODO / Open Questions

Không có điểm nào trong chủ đề Coding Convention được đánh dấu cần xác nhận thêm trong nghiên cứu gốc. Mục 7 (Best Practices bổ sung) là suy luận hợp lý dựa trên stack đã chốt — nếu team có quy ước khác, cập nhật trực tiếp file này.

---

*Xem tiếp: `04-API.md` để biết chi tiết toàn bộ API endpoints.*