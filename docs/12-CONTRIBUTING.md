# 12 — CONTRIBUTING

> Quy ước làm việc của team dành cho dự án ITAM: Git Flow, branch naming, commit convention, quy trình Pull Request, review rules, semantic versioning và release notes. Đây là tài liệu cuối cùng trong bộ 13 file — AI Coding Agent tuân thủ tài liệu này khi tạo branch, viết commit message, hoặc mở Pull Request thay mặt người dùng.

## Mục lục

1. [Git Flow (Đơn giản hoá)](#1-git-flow-đơn-giản-hoá)
2. [Branch Naming](#2-branch-naming)
3. [Commit Convention](#3-commit-convention)
4. [Pull Request](#4-pull-request)
5. [Review Rules](#5-review-rules)
6. [Versioning (Semantic Versioning)](#6-versioning-semantic-versioning)
7. [Release Notes (CHANGELOG)](#7-release-notes-changelog)
8. [Setup môi trường Local cho Contributor mới](#8-setup-môi-trường-local-cho-contributor-mới)
9. [Checklist trước khi mở PR](#9-checklist-trước-khi-mở-pr)
10. [TODO / Open Questions](#10-todo--open-questions)

---

## 1. Git Flow (Đơn giản hoá)

```
main          - luôn deploy-able, nguồn Production duy nhất
  │
  ├── feature/asset-qr-generation     ─┐
  ├── fix/request-approval-bug         ├─► PR → review → merge → main → auto-deploy
  ├── refactor/asset-service           │
  └── docs/api-documentation          ─┘
```

| Branch | Vai trò |
|---|---|
| `main` | Luôn ở trạng thái **deploy-able** — mọi commit trên `main` tương ứng 1:1 với 1 lần deploy Production thật (xem `11-DEPLOYMENT.md` mục 5) |
| `feature/*`, `fix/*`, `refactor/*`, `docs/*` | Nhánh phát triển ngắn hạn, tạo từ `main`, merge lại vào `main` qua Pull Request |

**Quyết định:** **Không** dùng `develop`/`release`/`hotfix` branch theo mô hình Git Flow đầy đủ (Vincent Driessen).

**Lý do:** Dự án phù hợp với mô hình **Trunk-based development đơn giản hoá** — chỉ 1 môi trường Production duy nhất (xem `11-DEPLOYMENT.md` mục 1), quy mô team nhỏ (solo dev hoặc nhóm rất nhỏ). Thêm `develop`/`release` branch sẽ tạo ra độ trễ không cần thiết (code phải "chờ" qua nhiều tầng branch mới tới Production) mà không mang lại lợi ích tương xứng — nhất quán với nguyên tắc "tránh phức tạp hoá sớm" xuyên suốt bộ tài liệu (`01-ARCHITECTURE.md` mục 11).

> **Hotfix khẩn cấp xử lý thế nào nếu không có nhánh `hotfix/*` riêng?** Dùng chung quy ước `fix/*` như mọi bug fix khác — không có sự phân biệt đặc biệt về quy trình giữa "fix thường" và "fix khẩn cấp" ở mô hình đơn giản hoá này. Nếu một lỗi cần fix gấp ngoài giờ, vẫn đi qua đúng luồng: tạo nhánh `fix/...` → PR → test gate pass → merge → auto-deploy (toàn bộ pipeline đã đủ nhanh để xử lý hotfix, xem `11-DEPLOYMENT.md` mục 3).

## 2. Branch Naming

**Quy ước:** `kebab-case`, prefix theo loại thay đổi, mô tả ngắn gọn bằng **Tiếng Anh**.

```
feature/<mô-tả-ngắn>     VD: feature/asset-qr-generation
fix/<mô-tả-ngắn>          VD: fix/request-approval-bug
refactor/<mô-tả-ngắn>     VD: refactor/asset-service
docs/<mô-tả-ngắn>         VD: docs/api-documentation
```

| Prefix | Khi nào dùng |
|---|---|
| `feature/` | Thêm tính năng mới (VD: `feature/audit-qr-scan`, `feature/notification-polling`) |
| `fix/` | Sửa lỗi (VD: `fix/asset-code-race-condition`, `fix/cors-preflight-error`) |
| `refactor/` | Tái cấu trúc code không đổi hành vi (VD: `refactor/request-service-extract-validation`) |
| `docs/` | Chỉ thay đổi tài liệu, không đụng code logic (VD: `docs/update-deployment-guide`) |

**Quy tắc bổ sung:**

- Mô tả ngắn gọn, đủ để hiểu mục đích nhánh mà **không cần** mở PR để đọc thêm — tránh tên quá chung chung như `fix/bug` hay `feature/update`.
- Toàn bộ thường viết Tiếng Anh, nhất quán với quy ước "tên class/method/biến/commit message luôn Tiếng Anh" đã chốt tại `03-CODING-STANDARDS.md` mục 6.
- Nếu 1 nhánh liên quan tới nhiều module cùng lúc (VD: vừa sửa Backend vừa sửa Frontend cho cùng 1 feature), **vẫn dùng chung 1 nhánh** (nhờ mono-repo — xem `02-FOLDER-STRUCTURE.md` mục 1), không tách nhánh riêng theo `backend/feature/...` hay `frontend/feature/...`.

## 3. Commit Convention

**Chuẩn:** [Conventional Commits](https://www.conventionalcommits.org/), viết bằng **Tiếng Anh**.

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### 3.1 Bảng `type` chuẩn

| Type | Khi dùng |
|---|---|
| `feat` | Thêm tính năng mới |
| `fix` | Sửa lỗi |
| `docs` | Chỉ thay đổi tài liệu (`docs/`, README, comment) |
| `refactor` | Tái cấu trúc code, không thêm tính năng/sửa lỗi, không đổi hành vi quan sát được |
| `test` | Thêm/sửa test (Unit/Integration/E2E) |
| `chore` | Thay đổi không ảnh hưởng code nghiệp vụ (cập nhật dependency, cấu hình CI/CD, `.gitignore`...) |
| `style` | Thay đổi format code (indentation, dấu chấm phẩy...), không đổi logic |

### 3.2 Quy tắc `scope`

`scope` là tên feature package liên quan, viết thường, khớp với tên package Backend (`auth`, `employee`, `asset`, `request`, `maintenance`, `audit`) hoặc tên feature Frontend (`assets`, `requests`...) — xem `01-ARCHITECTURE.md` mục 2 và `02-FOLDER-STRUCTURE.md` mục 4.

Nếu thay đổi liên quan tới `common/`/`shared/` (cross-cutting, không thuộc riêng 1 feature), dùng `scope` là `common`, `config`, hoặc bỏ trống `scope` nếu thay đổi quá rộng để gán cho 1 module cụ thể.

### 3.3 Ví dụ commit message thực tế

```
feat(asset): add QR code generation on asset creation

fix(request): fix approval status not updating correctly

docs(readme): update deployment instructions

refactor(auth): simplify JWT validation logic

test(audit): add integration test for discrepancy creation

chore(deps): upgrade spring-boot to 3.5.16

style(frontend): apply prettier formatting to requests module

feat(audit): implement auto-expire scheduler for audit sessions

The scheduler runs hourly and auto-completes audit sessions where
expires_at < now(), reusing the same logic as manual complete().

Closes #42
```

> **Footer `Closes #<issue-number>`:** Tuỳ chọn, dùng khi commit/PR giải quyết trực tiếp 1 GitHub Issue cụ thể — GitHub tự động đóng issue tương ứng khi PR được merge.

### 3.4 Commit message KHÔNG đạt chuẩn (tránh)

```
❌ fix bug
❌ update
❌ asset stuff
❌ Sửa lỗi duyệt request    (không phải Tiếng Anh)
❌ Feat: Add Asset (Asset viết hoa sai PascalCase cho description, type "Feat" sai case)
```

## 4. Pull Request

### 4.1 PR Template chuẩn

**`.github/pull_request_template.md`:**

```markdown
## Mô tả thay đổi

<!-- Mô tả ngắn gọn PR này làm gì và tại sao -->

## Loại thay đổi

- [ ] feat — Tính năng mới
- [ ] fix — Sửa lỗi
- [ ] refactor — Tái cấu trúc
- [ ] docs — Tài liệu
- [ ] test — Test
- [ ] chore — Khác (cấu hình, dependency...)

## Checklist trước khi merge

- [ ] Code tuân thủ `03-CODING-STANDARDS.md` (naming convention, logging, comment)
- [ ] Đã thêm/cập nhật test tương ứng (nếu có business logic mới — xem `10-TESTING.md` mục 4)
- [ ] Unit Test + Integration Test pass local (`mvn clean verify`)
- [ ] Không có secret/credential hardcode trong code (xem `11-DEPLOYMENT.md` mục 2)
- [ ] Đã cập nhật tài liệu liên quan trong `docs/` nếu thay đổi ảnh hưởng API/DB schema/business rule
- [ ] (Frontend) Đã kiểm tra responsive trên màn hình nhỏ nếu liên quan tới UI

## Liên quan tới Issue

Closes #<số-issue>  <!-- nếu có -->
```

### 4.2 Quy trình mở PR

```
1. Tạo nhánh từ main (đúng quy ước mục 2)
2. Code + commit theo Conventional Commits (mục 3)
3. Push nhánh lên GitHub
4. Mở PR → main, điền đầy đủ PR Template
5. CI tự động chạy "PR Test Gate" (xem 11-DEPLOYMENT.md mục 3.2)
   → FAIL: sửa code, push lại, CI chạy lại tự động
   → PASS: chuyển sang bước review (mục 5)
6. Review pass → Merge (squash hoặc merge commit — xem mục 4.3)
7. main tự động build & deploy (xem 11-DEPLOYMENT.md mục 3.3)
```

### 4.3 Chiến lược Merge

> ⚠️ **Không được đặc tả tường minh** trong nghiên cứu gốc — khuyến nghị **Squash and Merge** làm mặc định, vì:
> - Lịch sử `main` gọn gàng, mỗi PR tương ứng đúng 1 commit duy nhất trên `main` (dễ đọc lịch sử, dễ revert nguyên 1 PR nếu cần — xem `11-DEPLOYMENT.md` mục 9).
> - Commit message của squash commit nên được sửa lại cho khớp Conventional Commits ngay cả khi các commit lẻ trong nhánh không hoàn toàn chuẩn (GitHub cho phép sửa message khi squash).
>
> Đánh dấu **TODO: Need confirmation** nếu team muốn dùng chiến lược khác (Merge commit giữ nguyên toàn bộ lịch sử nhánh, hoặc Rebase and Merge) — xem mục 10.

## 5. Review Rules

> ⚠️ Mục này **không được đặc tả chi tiết** trong nghiên cứu gốc (Chủ đề 14 chỉ xác nhận "PR luôn chạy CI test trước khi merge") — nội dung dưới đây là **best practice bổ sung hợp lý**, áp dụng nhất quán với quy mô dự án (solo dev/nhóm nhỏ) đã xác định xuyên suốt bộ tài liệu. Đánh dấu **TODO: Need confirmation** ở mục 10.

| Quy tắc | Mô tả |
|---|---|
| Số lượng reviewer tối thiểu | **TODO: Need confirmation** — nếu solo dev, có thể tự review qua checklist PR Template thay vì yêu cầu reviewer thứ 2; nếu có ≥2 thành viên, khuyến nghị tối thiểu 1 approval trước khi merge |
| CI bắt buộc pass trước khi review | ✅ — không review code khi "PR Test Gate" (`11-DEPLOYMENT.md` mục 3.2) còn đang fail, tránh tốn thời gian review code chưa qua được test cơ bản |
| Phạm vi review ưu tiên | Business logic phức tạp (workflow, RBAC, soft delete — xem `10-TESTING.md` mục 4) cần review kỹ hơn so với CRUD đơn giản |
| Branch Protection Rule (GitHub) | Khuyến nghị bật **"Require status checks to pass before merging"** (chọn check "PR Test Gate") trên `main` — đảm bảo không ai (kể cả admin) merge được code chưa pass CI, kể cả khi thao tác trực tiếp qua GitHub UI bỏ qua local check |

## 6. Versioning (Semantic Versioning)

**Chuẩn:** [Semantic Versioning (SemVer)](https://semver.org/) chính thức — `MAJOR.MINOR.PATCH`.

```
v1.0.0   → Bản phát hành đầu tiên (MVP hoàn chỉnh, đủ 4 module chính)
v1.1.0   → Thêm tính năng mới, tương thích ngược (VD: thêm logout-all-devices)
v1.0.1   → Sửa lỗi (patch), không thêm tính năng mới
v2.0.0   → Breaking change (VD: đổi cấu trúc ApiResponse, đổi API versioning path)
```

| Thành phần | Tăng khi nào |
|---|---|
| `MAJOR` | Có **breaking change** — thay đổi không tương thích ngược (VD: đổi format response API, xoá/đổi tên field DTO đang được Frontend dùng, đổi cấu trúc bảng DB cần migrate dữ liệu thủ công) |
| `MINOR` | Thêm **tính năng mới**, vẫn tương thích ngược hoàn toàn (VD: thêm endpoint mới, thêm field optional vào response) |
| `PATCH` | **Sửa lỗi**, không thay đổi hành vi API/tính năng đã có (VD: fix bug logic, fix lỗi hiển thị UI) |

**Quy tắc gắn tag:** Mỗi lần release, gắn **Git tag** tương ứng trên commit `main` vừa deploy thành công:

```bash
git tag -a v1.0.0 -m "Release v1.0.0: MVP hoàn chỉnh"
git push origin v1.0.0
```

> **Lưu ý:** Vì pipeline hiện tại **tự động deploy mọi merge vào `main`** (không có khái niệm "release riêng" tách biệt khỏi deploy — xem `11-DEPLOYMENT.md` mục 5), việc gắn tag SemVer là hành động **thủ công bổ sung** đánh dấu mốc quan trọng (VD: "đây là bản hoàn chỉnh module Audit"), không phải bước bắt buộc trong pipeline CI/CD. Tag không kích hoạt thêm hành động CI/CD nào (không có workflow `on: push: tags:` riêng ở MVP).

## 7. Release Notes (CHANGELOG)

**File:** `CHANGELOG.md` ở thư mục gốc repo (`itam-system/CHANGELOG.md`).

**Định dạng:** Khuyến nghị theo chuẩn [Keep a Changelog](https://keepachangelog.com/) — format phổ biến, thường đi kèm SemVer.

```markdown
# Changelog

Mọi thay đổi đáng chú ý của dự án ITAM sẽ được ghi lại trong file này.

Định dạng dựa trên [Keep a Changelog](https://keepachangelog.com/),
và dự án tuân theo [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- (các thay đổi đã merge vào main nhưng chưa gắn tag release chính thức)

## [1.0.0] - 2026-06-30

### Added
- Module Quản lý danh mục thiết bị (Asset CRUD, QR code generation)
- Module Cấp phát/Thu hồi (Request workflow 3 bước: Employee → Manager → IT Staff)
- Module Bảo hành/Bảo trì (Maintenance tracking)
- Module Báo cáo kiểm kê (Audit session, QR scan, Discrepancy report)
- JWT Authentication (Access Token + Refresh Token httpOnly cookie)
- RBAC 4 role: Admin, IT Staff, Manager, Employee
- Notification system (Email qua SendGrid + In-app polling)

### Known Issues
- Chưa hỗ trợ logout toàn bộ thiết bị (xem 06-AUTHENTICATION.md TODO)
- Chưa có auto-approve theo ngưỡng giá trị (xem 07-BUSINESS-RULES.md TODO)
```

**Nhóm thay đổi chuẩn (theo Keep a Changelog):** `Added` (tính năng mới), `Changed` (thay đổi hành vi đã có), `Deprecated` (sắp loại bỏ), `Removed` (đã loại bỏ), `Fixed` (sửa lỗi), `Security` (vá lỗ hổng bảo mật).

**Quy tắc cập nhật:** Mỗi PR mang tính năng/sửa lỗi đáng chú ý (không bắt buộc cho mọi `chore`/`style`/`docs` nhỏ) nên kèm theo 1 dòng cập nhật vào mục `[Unreleased]` của `CHANGELOG.md` trong cùng PR đó — khi gắn tag release chính thức (mục 6), đổi `[Unreleased]` thành `[<version>] - <ngày>` và mở mục `[Unreleased]` mới trống cho chu kỳ tiếp theo.

## 8. Setup môi trường Local cho Contributor mới

> Bổ sung thực tế giúp AI Coding Agent hoặc dev mới có thể bắt đầu làm việc ngay — tổng hợp lại các bước đã đề cập rải rác ở `02-FOLDER-STRUCTURE.md` và `11-DEPLOYMENT.md` thành 1 trình tự setup local rõ ràng.

```bash
# 1. Clone repo
git clone https://github.com/<owner>/itam-system.git
cd itam-system

# 2. Tạo file .env từ mẫu (KHÔNG commit file .env thật — xem .gitignore)
cp .env.example .env
# → điền giá trị thật cho JWT_SECRET, SENDGRID_API_KEY (có thể dùng giá trị giả cho local
#   nếu không cần test gửi email thật), CLOUDINARY_* (tương tự)

# 3. Khởi chạy Backend + PostgreSQL local qua Docker Compose
docker compose up -d
# → Backend chạy tại http://localhost:8080
# → PostgreSQL local chạy tại localhost:5432 (xem 02-FOLDER-STRUCTURE.md mục 6)
# → Flyway tự động chạy migration + seed data khi Backend khởi động lần đầu

# 4. Cài đặt và chạy Frontend
cd frontend
npm install
npm run dev
# → Frontend chạy tại http://localhost:5173 (mặc định Vite)

# 5. Đăng nhập thử với tài khoản Admin mặc định
# email: admin@itam.local (xem 05-DATABASE.md mục 10 — mật khẩu tạm sinh khi seed,
# kiểm tra log Backend lúc khởi động lần đầu hoặc query trực tiếp DB local để lấy)
```

**File `.env.example` gợi ý (commit lên Git, KHÔNG chứa giá trị thật):**

```bash
# .env.example
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/itam_dev
SPRING_DATASOURCE_USERNAME=itam
SPRING_DATASOURCE_PASSWORD=itam_local_dev
JWT_SECRET=
SENDGRID_API_KEY=
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
CORS_ALLOWED_ORIGINS=http://localhost:5173
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

## 9. Checklist trước khi mở PR

Tổng hợp nhanh (đã có đầy đủ trong PR Template mục 4.1, liệt kê lại riêng để AI Coding Agent dễ tự kiểm tra trước khi đề xuất mở PR thay mặt người dùng):

1. Branch đặt tên đúng quy ước (mục 2).
2. Commit message đúng Conventional Commits, Tiếng Anh (mục 3).
3. `mvn clean verify` (Backend) pass local — không đẩy code biết trước sẽ fail CI.
4. Không có secret hardcode (đối chiếu `11-DEPLOYMENT.md` mục 2).
5. Naming convention đúng `03-CODING-STANDARDS.md`.
6. Nếu có business logic mới thuộc nhóm ưu tiên cao (workflow, RBAC, soft delete, audit — xem `10-TESTING.md` mục 4), đã có test tương ứng.
7. Nếu thay đổi ảnh hưởng tới API/DB schema/business rule đã chốt trong `docs/`, cập nhật file tương ứng trong cùng PR (giữ tài liệu luôn đồng bộ với code).
8. Cập nhật `CHANGELOG.md` mục `[Unreleased]` nếu là thay đổi đáng chú ý (mục 7).

## 10. TODO / Open Questions

> TODO: Need confirmation — **Chiến lược Merge** (Squash and Merge vs Merge commit vs Rebase and Merge, mục 4.3) — khuyến nghị Squash and Merge làm mặc định nhưng chưa được xác nhận tường minh từ nghiên cứu gốc.

> TODO: Need confirmation — **Review Rules chi tiết** (mục 5): số lượng reviewer tối thiểu bắt buộc, có cần CODEOWNERS file để tự động assign reviewer theo từng module hay không — nghiên cứu gốc chỉ xác nhận "PR luôn chạy CI test trước khi merge", chưa đặc tả quy trình review người-với-người.

> TODO: Need confirmation — **Quy trình hotfix khẩn cấp** ngoài giờ (mục 1) hiện dùng chung quy trình `fix/*` thông thường — cần xác nhận có cần 1 luồng "expedited" riêng (VD: cho phép skip review trong tình huống cực kỳ khẩn cấp, miễn vẫn phải pass CI) hay giữ nguyên 1 luồng duy nhất cho mọi loại fix.

> TODO: Need confirmation — **Workflow gắn Git tag SemVer** hiện là thao tác thủ công độc lập, không kích hoạt thêm hành động CI/CD nào (mục 6). Cần xác nhận có muốn bổ sung GitHub Actions Release workflow (`on: push: tags:`) để tự động tạo GitHub Release kèm release notes từ `CHANGELOG.md` hay giữ đơn giản như hiện tại.

---

*Đây là tài liệu cuối cùng trong bộ 13 file. Quay lại `00-OVERVIEW.md` để xem bản đồ tổng thể toàn bộ tài liệu dự án ITAM.*