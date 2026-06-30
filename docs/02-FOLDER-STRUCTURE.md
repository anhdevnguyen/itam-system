# 02 — FOLDER STRUCTURE

> Cấu trúc thư mục đầy đủ của mono-repo `itam-system`. AI Coding Agent phải tuân thủ chính xác cấu trúc này khi tạo file mới.

## Mục lục

1. [Repo Strategy](#1-repo-strategy)
2. [Cây thư mục Root](#2-cây-thư-mục-root)
3. [Backend — chi tiết](#3-backend--chi-tiết)
4. [Frontend — chi tiết](#4-frontend--chi-tiết)
5. [Database Migration](#5-database-migration)
6. [Docker (Local Dev)](#6-docker-local-dev)
7. [Quy tắc đặt file mới](#7-quy-tắc-đặt-file-mới)
8. [TODO / Open Questions](#8-todo--open-questions)

---

## 1. Repo Strategy

**Mono-repo** — 1 repository GitHub duy nhất (`itam-system`) chứa cả `backend/` và `frontend/`. Không tách 2 repo riêng.

**Lý do:** Dự án quy mô nhỏ/vừa, 1 mono-repo giúp đơn giản hoá quản lý version, dễ tham chiếu thay đổi BE/FE đồng thời trong cùng 1 PR, phù hợp với CI/CD pipeline đơn giản đã thiết kế (`11-DEPLOYMENT.md`).

## 2. Cây thư mục Root

```
itam-system/
├── backend/                    # Spring Boot app
├── frontend/                   # React (Vite) app
├── docs/                       # 13 file tài liệu (bộ tài liệu này)
├── docker-compose.yml          # Backend + PostgreSQL local cho dev
├── .github/
│   └── workflows/              # GitHub Actions CI/CD
└── README.md
```

| Thư mục/File | Mô tả |
|---|---|
| `backend/` | Toàn bộ source code Spring Boot |
| `frontend/` | Toàn bộ source code React + Vite |
| `docs/` | Bộ tài liệu kỹ thuật (file bạn đang đọc) |
| `docker-compose.yml` | Khởi chạy Backend + PostgreSQL cục bộ phục vụ dev/test trước khi deploy |
| `.github/workflows/` | Định nghĩa pipeline GitHub Actions (xem `11-DEPLOYMENT.md`) |
| `README.md` | Giới thiệu nhanh dự án, hướng dẫn chạy local |

## 3. Backend — chi tiết

**Package gốc:** `com.vanh.itam`

```
backend/
├── src/main/java/com/vanh/itam/
│   ├── auth/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── mapper/
│   │   └── exception/
│   ├── employee/        (cùng cấu trúc con: controller/service/repository/dto/entity/mapper/exception)
│   ├── asset/
│   ├── request/
│   ├── maintenance/
│   ├── audit/
│   └── common/
│       ├── config/      (SecurityConfig, CorsConfig, OpenApiConfig, SchedulerConfig)
│       ├── exception/   (GlobalExceptionHandler, base exceptions)
│       ├── response/    (ApiResponse wrapper)
│       ├── util/        (JwtUtil, QrCodeGenerator...)
│       └── entity/      (BaseEntity: id, createdAt, updatedAt, deletedAt)
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/    (Flyway scripts: V1__init.sql, V2__..., ...)
├── src/test/java/...
├── Dockerfile            (multi-stage: Maven build → JRE runtime)
└── pom.xml
```

### Chi tiết từng feature package

Tất cả 6 feature package (`auth`, `employee`, `asset`, `request`, `maintenance`, `audit`) tuân theo **cùng 1 cấu trúc con** nhất quán:

```
<feature>/
├── controller/    <Feature>Controller.java
├── service/
│   ├── <Feature>Service.java          (interface)
│   └── <Feature>ServiceImpl.java       (implementation)
├── repository/    <Feature>Repository.java   (extends JpaRepository)
├── dto/
│   ├── request/    Create<Feature>Request.java, Update<Feature>Request.java
│   └── response/   <Feature>Response.java
├── entity/         <Feature>.java (entity chính, có thể nhiều entity nếu domain phức tạp)
├── mapper/         <Feature>Mapper.java        (interface, MapStruct generate impl)
└── exception/      <Feature>NotFoundException.java, <Feature>...Exception.java
```

**Ví dụ cụ thể — package `asset`:**

```
asset/
├── controller/
│   └── AssetController.java
├── service/
│   ├── AssetService.java
│   └── AssetServiceImpl.java
├── repository/
│   ├── AssetRepository.java
│   └── CategoryRepository.java
├── dto/
│   ├── request/
│   │   ├── CreateAssetRequest.java
│   │   └── UpdateAssetRequest.java
│   └── response/
│       └── AssetResponse.java
├── entity/
│   ├── Asset.java
│   ├── Category.java
│   ├── AssetImage.java
│   └── AssetAssignmentHistory.java
├── mapper/
│   └── AssetMapper.java
└── exception/
    ├── AssetNotFoundException.java
    └── AssetNotAvailableException.java
```

### `common/` package

```
common/
├── config/
│   ├── SecurityConfig.java       # Spring Security, JWT filter chain
│   ├── CorsConfig.java           # CORS qua environment variable
│   ├── OpenApiConfig.java        # Springdoc OpenAPI/Swagger config
│   └── SchedulerConfig.java      # @EnableScheduling, cron job config
├── exception/
│   ├── BaseException.java        # abstract — errorCode + httpStatus
│   ├── BusinessException.java
│   ├── ResourceNotFoundException.java
│   ├── ValidationException.java
│   ├── UnauthorizedException.java
│   ├── ForbiddenException.java
│   └── GlobalExceptionHandler.java   # @RestControllerAdvice
├── response/
│   ├── ApiResponse.java
│   ├── ApiError.java
│   ├── Meta.java
│   └── Pagination.java
├── util/
│   ├── JwtUtil.java
│   └── QrCodeGenerator.java      # ZXing wrapper
└── entity/
    └── BaseEntity.java           # @MappedSuperclass: id, createdAt, updatedAt, deletedAt
```

### `src/main/resources/`

```
src/main/resources/
├── application.yml               # config chung, dùng ${ENV_VAR} placeholder cho secret
└── db/migration/
    ├── V1__init_schema.sql
    ├── V2__seed_roles.sql
    ├── V3__seed_demo_data.sql
    └── ...                       # tiếp tục đánh số tăng dần cho mỗi thay đổi schema
```

> **Quy tắc:** KHÔNG dùng `spring.jpa.hibernate.ddl-auto: update` cho production. Mọi thay đổi schema đi qua Flyway migration script mới (xem `05-DATABASE.md`).

## 4. Frontend — chi tiết

```
frontend/
├── src/
│   ├── auth/
│   ├── employees/
│   ├── assets/
│   ├── requests/
│   ├── maintenance/
│   ├── audit/              (mỗi feature: components/ hooks/ pages/ types/)
│   ├── shared/              (Button, Modal, Table... + useAuth, useApi...)
│   ├── lib/                 (Axios instance, API client gốc)
│   ├── routes/              (React Router config)
│   ├── assets/              (⚠️ static FE assets: hình ảnh/icon/font — KHÔNG liên quan "Asset thiết bị" nghiệp vụ)
│   ├── App.tsx
│   └── main.tsx
├── index.html
├── vite.config.ts
├── tailwind.config.js
├── tsconfig.json
└── package.json
```

> ⚠️ **Lưu ý quan trọng cho AI Coding Agent:** `frontend/src/assets/` là thư mục **chuẩn của Vite** chứa static file (hình ảnh, icon, font) — **hoàn toàn khác** với domain nghiệp vụ "Asset" (thiết bị IT) nằm ở `frontend/src/assets-management/` hoặc tương đương. Để tránh nhầm lẫn tên, khuyến nghị đặt tên thư mục feature nghiệp vụ là `frontend/src/assets/` **CHỈ KHI** không xung đột; nếu xung đột, dùng `device-assets/` cho domain nghiệp vụ và giữ `assets/` cho static file Vite mặc định. → **TODO: Need confirmation** — xác nhận tên thư mục cuối cùng để tránh xung đột giữa 2 khái niệm "asset" trùng tên.

### Chi tiết 1 feature folder (ví dụ `requests/`)

```
requests/
├── components/
│   ├── RequestList.tsx
│   ├── RequestForm.tsx
│   └── RequestStatusBadge.tsx
├── hooks/
│   └── useRequests.ts
├── pages/
│   ├── RequestsPage.tsx
│   └── RequestDetailPage.tsx
└── types/
    └── request.types.ts
```

### `shared/`

```
shared/
├── components/
│   ├── Button.tsx
│   ├── Modal.tsx
│   ├── Table.tsx
│   ├── Pagination.tsx
│   ├── SkeletonLoader.tsx
│   └── EmptyState.tsx
└── hooks/
    ├── useAuth.ts
    └── useApi.ts
```

### `lib/`

```
lib/
├── apiClient.ts         # Axios instance (baseURL, interceptor đính kèm Authorization header)
└── queryParams.ts        # Helper build query string (pagination/filter/sort)
```

## 5. Database Migration

| Hạng mục | Lựa chọn |
|---|---|
| Công cụ | **Flyway** |
| Vị trí script | `backend/src/main/resources/db/migration/` |
| Quy tắc đặt tên | `V<số_thứ_tự>__<mô_tả_snake_case>.sql` (VD: `V1__init_schema.sql`) |
| Cấm | Hibernate `ddl-auto: update` cho production |

→ Chi tiết schema, danh sách bảng, naming convention: `05-DATABASE.md`.

## 6. Docker (Local Dev)

| File | Vị trí | Mục đích |
|---|---|---|
| `docker-compose.yml` | Root | Chạy **Backend + PostgreSQL** cục bộ để dev/test trước khi deploy lên Render/Neon |
| `Dockerfile` | `backend/Dockerfile` | Multi-stage build (Maven build stage → JRE runtime stage), dùng để build image đẩy lên GHCR khi deploy thật |

**Ví dụ `docker-compose.yml` (tham khảo):**

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: itam_dev
      POSTGRES_USER: itam
      POSTGRES_PASSWORD: itam_local_dev
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  backend:
    build: ./backend
    depends_on:
      - postgres
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/itam_dev
      SPRING_DATASOURCE_USERNAME: itam
      SPRING_DATASOURCE_PASSWORD: itam_local_dev
    ports:
      - "8080:8080"

volumes:
  pgdata:
```

> Đây là cấu hình **tham khảo cho local dev only** — secret thật cho Production nằm trong Render/GitHub Actions Environment Variables, không commit vào `docker-compose.yml` (xem `11-DEPLOYMENT.md`).

## 7. Quy tắc đặt file mới

Khi AI Coding Agent cần tạo file mới, áp dụng quy tắc sau:

1. **Backend:** Xác định feature liên quan (`auth`/`employee`/`asset`/`request`/`maintenance`/`audit`) → đặt file vào đúng package con (`controller`/`service`/`repository`/`dto`/`entity`/`mapper`/`exception`) của feature đó. Nếu là cross-cutting (dùng chung ≥2 feature), đặt vào `common/`.
2. **Frontend:** Xác định feature liên quan → đặt vào đúng thư mục con (`components`/`hooks`/`pages`/`types`). Nếu là component/hook dùng chung ≥2 feature, đặt vào `shared/`.
3. **Không** tạo package/thư mục mới ngoài cấu trúc đã định nghĩa mà không có lý do rõ ràng — nếu cần, ghi chú lý do và đề xuất trong PR description.
4. **Migration SQL mới:** luôn tăng số thứ tự tiếp theo trong `db/migration/`, không sửa lại migration cũ đã chạy.

## 8. TODO / Open Questions

> TODO: Need confirmation — Tên thư mục frontend cho domain nghiệp vụ "Asset" (thiết bị) cần xác nhận để tránh trùng tên với `frontend/src/assets/` (static Vite assets mặc định). Đề xuất: `device-assets/` hoặc giữ `assets/` nếu team chấp nhận quy ước phân biệt qua ngữ cảnh import path.

---

*Xem tiếp: `03-CODING-STANDARDS.md` để biết quy tắc đặt tên class/biến/file chi tiết.*