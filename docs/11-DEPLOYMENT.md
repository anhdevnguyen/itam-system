# 11 — DEPLOYMENT

> Đặc tả đầy đủ chiến lược triển khai ITAM: secrets management, CI/CD pipeline (GitHub Actions), môi trường, logging production, monitoring/health check, và backup strategy. Đây là tài liệu tham chiếu chính khi AI Coding Agent cần viết/sửa file `.github/workflows/*.yml`, `Dockerfile`, hoặc cấu hình liên quan đến vận hành Production.

## Mục lục

1. [Tổng quan môi trường](#1-tổng-quan-môi-trường)
2. [Secrets Management](#2-secrets-management)
3. [CI/CD Pipeline (GitHub Actions)](#3-cicd-pipeline-github-actions)
4. [Dockerfile (Backend)](#4-dockerfile-backend)
5. [Branch & Deploy Strategy](#5-branch--deploy-strategy)
6. [Logging Production](#6-logging-production)
7. [Monitoring & Health Check](#7-monitoring--health-check)
8. [Backup Strategy](#8-backup-strategy)
9. [Rollback Strategy](#9-rollback-strategy)
10. [Checklist Deploy lần đầu (First Deploy)](#10-checklist-deploy-lần-đầu-first-deploy)
11. [TODO / Open Questions](#11-todo--open-questions)

---

## 1. Tổng quan môi trường

| Hạng mục | Giá trị |
|---|---|
| Số môi trường | **Chỉ 1: Production** (chưa có Staging/Dev riêng ở MVP) |
| Backend Hosting | **Render** (deploy từ Docker Image trên GHCR) |
| Frontend Hosting | **Vercel** (auto build/deploy qua GitHub integration) |
| Database Hosting | **Neon** (PostgreSQL Cloud, hỗ trợ branching) |
| Local Dev | `docker-compose.yml` (Backend + PostgreSQL local) — xem `02-FOLDER-STRUCTURE.md` mục 6 |

> **Lưu ý quan trọng cho AI Coding Agent:** Vì chỉ có 1 môi trường Production, code **không được** chứa logic rẽ nhánh theo `profile` phức tạp kiểu `dev/staging/prod` — chỉ cần phân biệt **Local (docker-compose)** và **Production (Render)** thông qua biến môi trường (`${ENV_VAR}` placeholder trong `application.yml`, xem mục 2). Nếu dự án mở rộng thêm môi trường Staging sau này, đây sẽ là thay đổi kiến trúc cần đánh giá riêng — chưa nằm trong phạm vi tài liệu này.

```
┌─────────────────┐         ┌─────────────────┐
│   Local Dev        │         │   Production       │
│  (docker-compose)   │         │  (Render + Vercel)  │
├─────────────────┤         ├─────────────────┤
│ PostgreSQL local    │         │ Neon PostgreSQL     │
│ Spring Boot (8080)  │         │ Render (Spring Boot) │
│ Vite dev server      │         │ Vercel (React static) │
│ Secret: hardcode     │         │ Secret: ENV variables │
│ trong docker-compose │         │ (Render/Vercel/GitHub) │
└─────────────────┘         └─────────────────┘
```

## 2. Secrets Management

**Nguyên tắc cốt lõi:** **Không hardcode secret** trong code hoặc trong `application.yml` commit lên Git — mọi giá trị nhạy cảm dùng placeholder `${ENV_VAR}`, giá trị thật được tiêm vào lúc build-time (GitHub Actions) hoặc runtime (Render/Vercel) qua biến môi trường nền tảng tương ứng.

### 2.1 Phân vùng theo nơi lưu

| Nơi lưu | Phạm vi sử dụng | Secret cụ thể |
|---|---|---|
| **GitHub Actions Secrets** | CI/CD pipeline (build-time) | `JWT_SECRET` (cho Integration Test), `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` (test — thường không cần vì Testcontainers tự sinh), `GHCR_TOKEN` (đẩy image lên GHCR), `RENDER_DEPLOY_HOOK_URL` |
| **Render Environment Variables** | Backend runtime | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (Neon), `JWT_SECRET`, `SENDGRID_API_KEY`, `CLOUDINARY_CLOUD_NAME`/`CLOUDINARY_API_KEY`/`CLOUDINARY_API_SECRET`, `CORS_ALLOWED_ORIGINS` |
| **Vercel Environment Variables** | Frontend runtime (build-time, inject vào bundle) | `VITE_API_BASE_URL` |

> **Quy ước đặt tên biến môi trường Frontend (Vite):** Bắt buộc prefix `VITE_` để Vite expose biến đó ra `import.meta.env` — biến không có prefix này **sẽ không** xuất hiện trong bundle (tính năng bảo mật mặc định của Vite, tránh lộ secret không mong muốn vào code client-side).

### 2.2 Ví dụ `application.yml` dùng placeholder

```yaml
# backend/src/main/resources/application.yml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  flyway:
    enabled: true
    locations: classpath:db/migration

jwt:
  secret: ${JWT_SECRET}
  access-token-expiration: 1800000   # 30 phút (ms)
  refresh-token-expiration: 2592000000  # 30 ngày (ms)

sendgrid:
  api-key: ${SENDGRID_API_KEY}

cloudinary:
  cloud-name: ${CLOUDINARY_CLOUD_NAME}
  api-key: ${CLOUDINARY_API_KEY}
  api-secret: ${CLOUDINARY_API_SECRET}

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS}   # VD: https://itam-system.vercel.app,https://itam.customdomain.com
```

> **Cấm tuyệt đối:** Commit file `.env` chứa giá trị thật lên Git. Repo nên có file `.env.example` (không chứa giá trị thật, chỉ liệt kê tên biến cần có) để hướng dẫn dev mới setup local — xem `12-CONTRIBUTING.md`.

### 2.3 Bảng tổng hợp toàn bộ biến môi trường cần thiết

| Biến | Nơi cấu hình | Bắt buộc | Ghi chú |
|---|---|---|---|
| `SPRING_DATASOURCE_URL` | Render | ✅ | JDBC URL Neon, dạng `jdbc:postgresql://<host>/<db>?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Render | ✅ | |
| `SPRING_DATASOURCE_PASSWORD` | Render | ✅ | |
| `JWT_SECRET` | Render, GitHub Actions (test) | ✅ | Chuỗi random đủ dài (≥256-bit) cho HS256 |
| `SENDGRID_API_KEY` | Render | ✅ | |
| `CLOUDINARY_CLOUD_NAME` | Render | ✅ | |
| `CLOUDINARY_API_KEY` | Render | ✅ | |
| `CLOUDINARY_API_SECRET` | Render | ✅ | |
| `CORS_ALLOWED_ORIGINS` | Render | ✅ | Danh sách domain phân tách dấu phẩy |
| `GHCR_TOKEN` | GitHub Actions | ✅ | Thường dùng `secrets.GITHUB_TOKEN` mặc định, không cần tạo riêng (xem mục 3.3) |
| `RENDER_DEPLOY_HOOK_URL` | GitHub Actions | ✅ | URL webhook trigger deploy của Render |
| `VITE_API_BASE_URL` | Vercel | ✅ | VD: `https://itam-backend.onrender.com/api/v1` |

## 3. CI/CD Pipeline (GitHub Actions)

### 3.1 Tổng quan 2 Trigger

```
┌───────────────────────────────────────────────────────────────┐
│ Trigger 1: Pull Request → main                                    │
│  → Chạy Unit Test + Integration Test (Testcontainers) ONLY          │
│  → KHÔNG build Docker image, KHÔNG deploy                            │
│  → Mục đích: gate chất lượng trước khi merge                         │
└───────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────┐
│ Trigger 2: Push/Merge → main                                       │
│  Job Backend:                                                       │
│   1. Checkout code                                                  │
│   2. Setup JDK 21                                                   │
│   3. Run Unit Test + Integration Test → FAIL thì dừng pipeline ngay  │
│   4. Build Maven (mvn clean package)                                 │
│   5. Build Docker image (multi-stage)                                │
│   6. Push image → GHCR                                                │
│   7. Trigger Render deploy (gọi Deploy Hook URL)                      │
│                                                                       │
│  Job Frontend:                                                       │
│   → Vercel tự động build/deploy qua GitHub integration sẵn có         │
│   → KHÔNG cần xử lý gì thêm trong GitHub Actions                     │
└───────────────────────────────────────────────────────────────┘
```

### 3.2 Workflow file — Pull Request Gate

**`.github/workflows/pr-test.yml`**

```yaml
name: PR Test Gate

on:
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Run Unit Test + Integration Test
        working-directory: backend
        run: mvn clean verify
        # Testcontainers tự pull PostgreSQL image và chạy Flyway migration thật
        # (xem 10-TESTING.md mục 6) — không cần service container riêng trong workflow
```

> **Quan trọng:** Job này **không** build Docker image, **không** deploy — chỉ chạy `mvn clean verify` (bao gồm cả Unit Test lẫn Integration Test trong cùng 1 lệnh Maven theo lifecycle chuẩn `test` → `verify`). Nếu bất kỳ test nào fail, job fail → GitHub tự động chặn merge nếu branch protection rule yêu cầu check này pass (xem `12-CONTRIBUTING.md` mục Pull Request).

### 3.3 Workflow file — Build & Deploy

**`.github/workflows/deploy.yml`**

```yaml
name: Build and Deploy

on:
  push:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  backend:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Run Unit Test + Integration Test
        working-directory: backend
        run: mvn clean verify
        # FAIL → toàn bộ job dừng tại đây, các step sau (build/push/deploy) KHÔNG chạy

      - name: Build Maven package
        working-directory: backend
        run: mvn clean package -DskipTests
        # -DskipTests vì test đã chạy ở step trên — tránh chạy trùng 2 lần tốn thời gian CI

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
          # secrets.GITHUB_TOKEN là token mặc định GitHub tự cấp cho mỗi workflow run,
          # đã có quyền packages:write nhờ khai báo `permissions` ở trên — KHÔNG cần tạo PAT riêng

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: ./backend
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:latest
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
          # Tag kép: `latest` để Render luôn pull bản mới nhất,
          # `${{ github.sha }}` để có thể truy vết/rollback về đúng commit cụ thể (xem mục 9)

      - name: Trigger Render Deploy
        run: |
          curl -X POST "${{ secrets.RENDER_DEPLOY_HOOK_URL }}"
        # Render Deploy Hook tự động pull image `latest` mới nhất từ GHCR và redeploy
        # Không cần Render API key — Deploy Hook URL tự nó đã là "secret" đủ dùng

  # Job Frontend: KHÔNG cần định nghĩa ở đây.
  # Vercel GitHub Integration tự lắng nghe push lên `main` và build/deploy độc lập,
  # tách biệt hoàn toàn khỏi GitHub Actions của repo này.
```

> **Vì sao không gộp Unit Test + Integration Test trùng lặp giữa PR-gate và Deploy workflow thành 1 job dùng chung (reusable workflow)?** Đây là **tối ưu hoá tiềm năng** chưa được áp dụng ở MVP để giữ pipeline đơn giản, dễ đọc cho dự án quy mô nhỏ/demo — có thể refactor sang `workflow_call` reusable workflow sau nếu pipeline phình to. Không phải yêu cầu tường minh từ nghiên cứu gốc.

### 3.4 Test Gate — Quy tắc dừng pipeline khi fail

| Trigger | Test chạy | Hành vi nếu fail |
|---|---|---|
| Pull Request → `main` | Unit Test + Integration Test | **Chặn merge** — GitHub hiển thị check ❌, không cho bấm "Merge" nếu branch protection bật required check (xem `12-CONTRIBUTING.md`) |
| Push/Merge → `main` | Unit Test + Integration Test | **Dừng job ngay lập tức** tại step test — các step `Build Maven package`, `Build and push Docker image`, `Trigger Render Deploy` phía sau **không được thực thi** (hành vi mặc định của GitHub Actions: step fail → job dừng, trừ khi step có `continue-on-error: true`, vốn **không** dùng ở pipeline này) |

→ Nhất quán hoàn toàn với `10-TESTING.md` mục 9 (CI/CD Test Gate).

## 4. Dockerfile (Backend)

**Multi-stage build** — stage 1 dùng Maven image để build, stage 2 chỉ chứa JRE runtime (image cuối nhỏ gọn, không mang theo toolchain build không cần thiết).

**`backend/Dockerfile`:**

```dockerfile
# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

| Quyết định | Lý do |
|---|---|
| `COPY pom.xml` riêng trước `COPY src` | Tận dụng Docker layer cache — nếu chỉ sửa code (`src/`) mà không đổi dependency (`pom.xml`), bước `mvn dependency:go-offline` không cần chạy lại, build nhanh hơn đáng kể |
| `-DskipTests` trong Dockerfile | Test **đã chạy** ở bước CI riêng (mục 3.3) trước khi tới bước build Docker image — tránh chạy test trùng lặp 2 lần (1 lần ở GitHub Actions step, 1 lần ẩn trong `docker build`) |
| `eclipse-temurin:21-jre-alpine` cho runtime | Alpine base nhỏ gọn (giảm dung lượng image, tăng tốc pull/push GHCR và deploy Render), chỉ cần JRE (không cần JDK đầy đủ) để chạy `.jar` đã build sẵn |

## 5. Branch & Deploy Strategy

| Quy tắc | Giá trị |
|---|---|
| Số môi trường Production | 1 (duy nhất) |
| Merge vào `main` | **Tự động deploy thẳng lên Production** — không cần approval thủ công bổ sung nào ngoài việc PR đã pass test gate và được review (xem `12-CONTRIBUTING.md`) |
| Lý do chọn auto-deploy không cần approval riêng | Phù hợp dự án cá nhân/demo, quy mô team nhỏ — thêm bước approval thủ công (VD: GitHub Environments với required reviewers) sẽ làm chậm vòng lặp phát triển mà không mang lại lợi ích tương xứng ở quy mô này |

> **Lưu ý:** "Không cần approval thủ công cho deploy" **khác** với "không cần review code". PR review trước khi merge **vẫn bắt buộc** theo quy ước team (xem `12-CONTRIBUTING.md` mục Pull Request) — chỉ riêng **hành động deploy sau khi merge** là tự động hoá hoàn toàn, không có bước "Approve to deploy" riêng biệt như mô hình GitHub Environments với protection rule.

## 6. Logging Production

**Định dạng:** **Structured logging (JSON format)** — dùng Logback với encoder JSON (`logstash-logback-encoder`), giúp dễ filter/search trên Render log viewer (vốn hiển thị log dạng text stream, JSON structured giúp tìm theo field cụ thể dễ hơn log dạng câu văn tự do).

**Cấu hình `logback-spring.xml` (gợi ý):**

```xml
<!-- backend/src/main/resources/logback-spring.xml -->
<configuration>
    <springProfile name="default">
        <!-- Local dev: log dạng text thường, dễ đọc trên terminal -->
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss} %-5level [%thread] %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="CONSOLE" />
        </root>
    </springProfile>

    <springProfile name="production">
        <!-- Production (Render): log dạng JSON structured -->
        <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
        </appender>
        <root level="INFO">
            <appender-ref ref="JSON_CONSOLE" />
        </root>
    </springProfile>
</configuration>
```

**Dependency cần thêm (`pom.xml`):**

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

**Ví dụ output JSON structured trên Render:**

```json
{
  "@timestamp": "2026-06-30T10:00:00.123Z",
  "level": "INFO",
  "logger_name": "com.vanh.itam.request.service.RequestServiceImpl",
  "message": "Request approved: requestId=42, managerId=8, employeeId=5",
  "thread_name": "http-nio-8080-exec-3"
}
```

> Quy tắc mức log (`ERROR`/`WARN`/`INFO`/`DEBUG`) áp dụng nhất quán với `03-CODING-STANDARDS.md` mục 4 và `09-ERROR-CODES.md` mục 7 — production chạy ở mức `INFO` (lọc bỏ `DEBUG`), nên `DEBUG` log (input/output method, query params) sẽ **không** xuất hiện trên Render log viewer mặc định.

## 7. Monitoring & Health Check

**Công cụ:** **Spring Boot Actuator** (`/actuator/health`) — Render dùng endpoint này để kiểm tra service còn sống (health check tự động của nền tảng hosting).

**Dependency (`pom.xml`):**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Cấu hình expose endpoint (`application.yml`):**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never   # Không lộ chi tiết kỹ thuật (DB connection string, disk space...) ra public endpoint
```

> **Lý do `show-details: never`:** `/actuator/health` là endpoint **public** (`permitAll()` trong `SecurityConfig`, xem `06-AUTHENTICATION.md` mục 8) để Render gọi được mà không cần auth — nếu để `show-details: always`, endpoint này có thể vô tình lộ thông tin hạ tầng nội bộ (loại DB, trạng thái connection pool...) cho bất kỳ ai truy cập public URL.

**Phạm vi monitoring ở MVP:**

| Công cụ | Trạng thái MVP |
|---|---|
| Spring Boot Actuator (`/actuator/health`) | ✅ Có — bắt buộc cho Render health check |
| Render Dashboard mặc định | ✅ Có — xem log, CPU/RAM usage, deploy history sẵn có từ nền tảng, không cần cấu hình thêm |
| Sentry (error tracking) | ❌ Chưa có ở MVP |
| UptimeRobot (uptime monitoring bên ngoài) | ❌ Chưa có ở MVP |
| APM (Application Performance Monitoring) chuyên sâu | ❌ Chưa có ở MVP |

> **Lý do không thêm công cụ ngoài ở MVP:** Nhất quán với mục tiêu thiết kế xuyên suốt "chi phí thấp, dễ bảo trì" (`00-OVERVIEW.md` mục 2) — Actuator + Render Dashboard mặc định đã đủ để vận hành ở quy mô 50-100 nhân viên/200-300 thiết bị. Bổ sung Sentry/UptimeRobot là hướng mở rộng hợp lý nếu hệ thống lên production thật với traffic cao hơn — đánh dấu **TODO: Need confirmation** nếu cần triển khai sớm hơn dự kiến (xem mục 11).

## 8. Backup Strategy

| Hạng mục | Giá trị |
|---|---|
| Cơ chế backup hiện tại | **Dựa hoàn toàn vào Neon Free Tier** — point-in-time recovery (PITR) mặc định của Neon |
| Giới hạn | Theo đúng giới hạn gói Free của Neon (thời gian lưu trữ point-in-time recovery history giới hạn — **TODO: Need confirmation** xác nhận con số cụ thể tại thời điểm triển khai, vì chính sách free tier của nhà cung cấp cloud có thể thay đổi theo thời gian) |
| Backup riêng bổ sung (cron export SQL dump) | ❌ **Chưa có** ở MVP |

**Lý do chưa cần backup riêng ở MVP:** Neon Cloud (qua cơ chế branching + point-in-time recovery built-in) đã cung cấp mức độ an toàn dữ liệu cơ bản đủ dùng cho giai đoạn demo/MVP, tránh phát sinh thêm chi phí vận hành (cron job, storage cho SQL dump, monitoring riêng cho job backup) không cần thiết ở quy mô hiện tại.

> ⚠️ **TODO: Need confirmation** — Khi dữ liệu hệ thống trở nên quan trọng hơn (VD: hệ thống chính thức đưa vào vận hành thật, không còn là demo/MVP), cần đánh giá lại 2 phương án:
> 1. **Nâng cấp Neon lên Paid tier** — có retention period dài hơn cho point-in-time recovery, nhiều tuỳ chọn backup hơn.
> 2. **Thêm backup thủ công/tự động riêng** — VD: GitHub Actions scheduled job (`on: schedule`, cron hàng ngày/hàng tuần) chạy `pg_dump` xuất ra file SQL, lưu vào storage riêng (S3, Cloudinary, hoặc GitHub Release artifact).
>
> Quyết định cụ thể cần Tech Lead/Product Owner xác nhận dựa trên mức độ quan trọng thực tế của dữ liệu tại thời điểm đó.

**Gợi ý script backup thủ công (tham khảo, CHƯA triển khai ở MVP — chỉ dùng nếu Tech Lead xác nhận cần sớm hơn):**

```bash
#!/bin/bash
# scripts/backup-db.sh (THAM KHẢO — chưa kích hoạt trong CI/CD ở MVP)
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
pg_dump "$SPRING_DATASOURCE_URL" > "backup_itam_${TIMESTAMP}.sql"
# Bước upload lên storage riêng (S3/Cloudinary) cần bổ sung thêm nếu kích hoạt thật
```

## 9. Rollback Strategy

> ⚠️ Mục này **không được đặc tả tường minh** trong nghiên cứu gốc (Chủ đề 13, `nghien_cuu.md`) — nội dung dưới đây là **suy luận hợp lý bổ sung**, tận dụng trực tiếp các cơ chế đã có sẵn từ pipeline (mục 3) mà không cần thêm công cụ/quy trình mới, nhất quán với nguyên tắc "tránh phức tạp hoá sớm" xuyên suốt bộ tài liệu (`01-ARCHITECTURE.md` mục 11). Đánh dấu **TODO: Need confirmation** ở mục 11.

**Backend rollback (qua Render):**

1. Mỗi lần deploy, Docker image được tag kép `latest` **và** `${{ github.sha }}` (xem mục 3.3) — nghĩa là mọi commit từng deploy thành công đều có 1 image tương ứng còn lưu trên GHCR (theo chính sách retention của GHCR, mặc định không tự xoá trừ khi cấu hình riêng).
2. Để rollback, có 2 lựa chọn:
   - **Cách đơn giản nhất:** Dùng tính năng **"Rollback to previous deploy"** có sẵn trên Render Dashboard (Render tự lưu lịch sử các lần deploy gần nhất, có UI bấm rollback trực tiếp) — không cần thao tác Git/Docker thủ công.
   - **Cách thủ công (nếu cần rollback xa hơn lịch sử Render lưu):** Revert commit trên `main` (`git revert <sha>`) → push → pipeline tự động chạy lại từ đầu (test → build → deploy), tạo ra trạng thái Production mới tương đương trạng thái commit cũ.

**Frontend rollback (qua Vercel):** Vercel có UI lưu lịch sử mọi lần deploy theo từng commit, cho phép **"Promote to Production"** bất kỳ deploy nào trong lịch sử với 1 click — không cần thao tác Git riêng.

**Database rollback (Migration):** Flyway **không hỗ trợ rollback tự động** cho migration đã chạy (đúng triết lý Flyway: forward-only). Nếu một migration mới gây lỗi schema ở Production:
- **Không** sửa lại file migration cũ đã chạy (vi phạm quy tắc đã chốt ở `02-FOLDER-STRUCTURE.md` mục 7: "Migration SQL mới: luôn tăng số thứ tự tiếp theo... không sửa lại migration cũ đã chạy").
- Thay vào đó, viết **migration mới** đảo ngược thay đổi (VD: nếu `V20` thêm sai 1 cột, viết `V21__drop_wrong_column.sql` để xoá cột đó).
- Đây là pattern chuẩn của Flyway, nhất quán với chiến lược migration đã chốt tại `05-DATABASE.md` mục 9.

## 10. Checklist Deploy lần đầu (First Deploy)

> Checklist tổng hợp các bước cần thực hiện thủ công **1 lần duy nhất** khi setup dự án lần đầu (không lặp lại ở mỗi lần deploy sau, vì các lần sau đã được tự động hoá hoàn toàn qua pipeline ở mục 3). Mục này tổng hợp lại các bước đã rải rác trong các mục trên thành 1 trình tự rõ ràng, tiện cho người đọc lần đầu setup hạ tầng.

1. **Tạo project trên Neon** → lấy connection string (`SPRING_DATASOURCE_URL`/`USERNAME`/`PASSWORD`).
2. **Tạo Web Service trên Render**, trỏ tới image GHCR (`ghcr.io/<owner>/<repo>:latest`) — cấu hình toàn bộ biến môi trường liệt kê ở mục 2.3 (phần Render) trong Render Dashboard → Environment.
3. **Lấy Render Deploy Hook URL** (Render Dashboard → Settings → Deploy Hook) → lưu vào GitHub Actions Secrets với tên `RENDER_DEPLOY_HOOK_URL`.
4. **Tạo project trên Vercel**, kết nối GitHub repo (chọn thư mục root `frontend/`) → cấu hình biến môi trường `VITE_API_BASE_URL` trỏ tới domain Render vừa tạo ở bước 2.
5. **Cấu hình GitHub Actions Secrets** (`Settings → Secrets and variables → Actions`): `JWT_SECRET` (cho test, nếu Integration Test cần — thường Testcontainers tự sinh DB riêng nên secret DB không bắt buộc ở bước này), `RENDER_DEPLOY_HOOK_URL`.
6. **Cấu hình GitHub Branch Protection** cho `main` (yêu cầu PR test gate pass trước khi merge — xem `12-CONTRIBUTING.md`).
7. **Push code lần đầu lên `main`** → pipeline tự động chạy: test → build Docker → push GHCR → trigger Render deploy. Vercel deploy song song độc lập qua integration riêng.
8. **Xác nhận `/actuator/health`** trả `200 OK` trên domain Render thật → xác nhận Flyway đã chạy migration + seed data thành công (kiểm tra qua Neon Dashboard hoặc gọi thử `POST /api/v1/auth/login` với tài khoản Admin mặc định `admin@itam.local`, xem `05-DATABASE.md` mục 10).
9. **Xác nhận CORS** — `CORS_ALLOWED_ORIGINS` trên Render đã bao gồm đúng domain Vercel vừa deploy (kiểm tra bằng cách gọi API thật từ Frontend đã deploy, không chỉ test local).

## 11. TODO / Open Questions

> TODO: Need confirmation — **Giới hạn cụ thể của Neon Free Tier** (số ngày point-in-time recovery history, dung lượng storage tối đa) cần được xác nhận tại thời điểm triển khai thật, vì chính sách free tier của nhà cung cấp cloud (Neon) có thể thay đổi theo thời gian — không nên hardcode con số cụ thể trong tài liệu kỹ thuật lâu dài.

> TODO: Need confirmation — **Thời điểm và tiêu chí nâng cấp backup strategy** (Neon Paid tier hoặc thêm backup thủ công/tự động riêng) — đã ghi nhận từ `00-OVERVIEW.md` mục 12 và `05-DATABASE.md`. Cần Tech Lead xác nhận ngưỡng cụ thể (VD: "khi có > X người dùng thật" hoặc "khi chuyển từ demo sang production chính thức") để biết khi nào cần hành động.

> TODO: Need confirmation — **Rollback Strategy** (mục 9) là suy luận hợp lý bổ sung dựa trên cơ chế sẵn có của Render/Vercel/Flyway, **chưa được đặc tả tường minh** trong nghiên cứu gốc (Chủ đề 13 không đề cập tới kịch bản rollback). Cần Tech Lead xác nhận quy trình này có phù hợp thực tế hay cần điều chỉnh.

> TODO: Need confirmation — **Bổ sung công cụ Monitoring/Error Tracking** (Sentry, UptimeRobot...) ngoài Actuator + Render Dashboard mặc định — hiện đánh giá là chưa cần thiết ở MVP, nhưng cần xác nhận lại nếu dự án chuyển sang giai đoạn vận hành thật với người dùng thật (không còn thuần demo).

---

*Xem tiếp: `12-CONTRIBUTING.md` để biết quy ước Git Flow, commit convention, Pull Request và versioning của team.*