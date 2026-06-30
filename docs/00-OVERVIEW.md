# 00 — OVERVIEW

> Tài liệu tổng quan dự án **IT Asset Management System (ITAM)**. Đây là điểm khởi đầu cho mọi AI Coding Agent (ChatGPT, Claude Code, Codex, Gemini CLI, Cursor, Cline, Roo Code, Continue, Windsurf...) trước khi đọc các tài liệu chi tiết khác trong `docs/`.

## Mục lục

1. [Giới thiệu dự án](#1-giới-thiệu-dự-án)
2. [Bài toán & Mục tiêu](#2-bài-toán--mục-tiêu)
3. [Quy mô & Đối tượng sử dụng](#3-quy-mô--đối-tượng-sử-dụng)
4. [Vai trò người dùng (Roles)](#4-vai-trò-người-dùng-roles)
5. [Phạm vi chức năng MVP](#5-phạm-vi-chức-năng-mvp)
6. [Loại thiết bị quản lý](#6-loại-thiết-bị-quản-lý)
7. [Kiến trúc Multi-branch (tổng quan)](#7-kiến-trúc-multi-branch-tổng-quan)
8. [Tech Stack tổng quan](#8-tech-stack-tổng-quan)
9. [Lộ trình mở rộng (Roadmap)](#9-lộ-trình-mở-rộng-roadmap)
10. [Bản đồ tài liệu (Document Map)](#10-bản-đồ-tài-liệu-document-map)
11. [Thuật ngữ (Glossary)](#11-thuật-ngữ-glossary)
12. [TODO / Open Questions tổng hợp](#12-todo--open-questions-tổng-hợp)

---

## 1. Giới thiệu dự án

| Mục | Nội dung |
|---|---|
| **Tên dự án** | IT Asset Management System (ITAM) |
| **Tên repository** | `itam-system` (mono-repo) |
| **Loại dự án** | Web application nội bộ doanh nghiệp (internal tool) |
| **Package gốc Backend** | `com.vanh.itam` |

ITAM là hệ thống quản lý **toàn bộ vòng đời thiết bị IT** trong doanh nghiệp — từ lúc mua, cấp phát cho nhân viên, bảo trì/bảo hành, kiểm kê định kỳ, cho đến khi thanh lý. Hệ thống được thiết kế dạng **Monolith gọn nhẹ**, ưu tiên chi phí thấp, dễ triển khai, dễ bảo trì, phù hợp làm dự án demo năng lực cho nhà tuyển dụng nhưng vẫn tuân theo các best practice của hệ thống production thực thụ.

## 2. Bài toán & Mục tiêu

**Hiện trạng (Pain point):** Doanh nghiệp đang quản lý thiết bị IT bằng **Excel rời rạc**, dẫn đến:

- Không biết chính xác ai đang giữ thiết bị gì tại một thời điểm.
- Khó audit / đối chiếu thực tế với sổ sách.
- Dễ thất thoát thiết bị (mất, không rõ vị trí, không ai chịu trách nhiệm).

**Mục tiêu MVP:** Số hoá toàn bộ vòng đời thiết bị theo luồng:

```
Mua thiết bị → Cấp phát cho nhân viên → Bảo trì/Bảo hành → Thu hồi → Thanh lý
```

**Mục tiêu mở rộng (Phase 2 — ngoài phạm vi MVP):** Quản lý **license phần mềm** (software license management), dùng chung hạ tầng `categories` đã thiết kế mở sẵn từ MVP.

## 3. Quy mô & Đối tượng sử dụng

| Tiêu chí | Giá trị |
|---|---|
| Số lượng nhân viên | 50–100 |
| Số lượng thiết bị | 200–300 |
| Số chi nhánh ban đầu (MVP) | 1 chi nhánh |
| Khả năng mở rộng | Kiến trúc **phải sẵn sàng multi-branch** ngay từ MVP (xem mục 7) |
| Đối tượng sử dụng | Nhân viên nội bộ doanh nghiệp (không phải khách hàng/public) |

## 4. Vai trò người dùng (Roles)

Hệ thống có **4 vai trò cố định**, ánh xạ trực tiếp tới bảng `roles` trong database (xem `05-DATABASE.md`) và ma trận phân quyền chi tiết tại `06-AUTHENTICATION.md`:

| # | Role | Mô tả |
|---|---|---|
| 1 | **ADMIN** (IT Manager — Trung tâm) | Toàn quyền cấu hình hệ thống. **Read-only mặc định + có khả năng override** trên dữ liệu của **mọi chi nhánh**. |
| 2 | **IT_STAFF** (Chi nhánh) | Full CRUD trên thiết bị **của chi nhánh mình**. Thực hiện cấp phát / thu hồi / bảo trì / kiểm kê. |
| 3 | **MANAGER** (Trưởng phòng) | Duyệt (approve/reject) yêu cầu cấp phát của nhân viên **thuộc phòng/team mình**. |
| 4 | **EMPLOYEE** (Nhân viên) | Xem thiết bị mình đang giữ, gửi yêu cầu cấp phát mới, báo hỏng. |

> **TODO: Need confirmation** — Quy trình duyệt hiện tại **bắt buộc qua Manager cho mọi yêu cầu**, chưa có ngoại lệ "khẩn cấp" hoặc "ngưỡng giá trị thấp bỏ qua duyệt" (VD: auto-approve cho thiết bị < X VNĐ). Đây là điểm thiết kế mở rộng tiềm năng, cần xác nhận thêm nếu muốn triển khai trong tương lai.

## 5. Phạm vi chức năng MVP

ITAM MVP gồm **4 module chính**:

```
┌─────────────────────────┐     ┌──────────────────────────┐
│ 1. Quản lý danh mục      │     │ 2. Cấp phát / Thu hồi     │
│    thiết bị (Asset)      │     │    (Request Workflow)     │
└─────────────────────────┘     └──────────────────────────┘
┌─────────────────────────┐     ┌──────────────────────────┐
│ 3. Bảo hành / Bảo trì     │     │ 4. Báo cáo kiểm kê         │
│    (Maintenance)          │     │    (Audit / QR Scan)      │
└─────────────────────────┘     └──────────────────────────┘
```

1. **Quản lý danh mục thiết bị (Asset)** — Asset dùng chung **1 bộ thuộc tính cơ bản**: tên, mã, ngày mua, giá trị, trạng thái, người giữ. MVP **không** đặc tả schema riêng theo từng loại thiết bị (laptop, máy in... dùng chung 1 schema).
2. **Cấp phát / Thu hồi (Request Workflow)** — Luồng 3 bước: `Employee tạo yêu cầu → Manager duyệt → IT Staff thực hiện cấp phát`. Chi tiết đầy đủ tại `07-BUSINESS-RULES.md`.
3. **Bảo hành / Bảo trì (Maintenance)** — Theo dõi lịch sử bảo hành, sửa chữa, bảo trì định kỳ của từng thiết bị.
4. **Báo cáo kiểm kê (Audit)** — Định kỳ quý/năm, IT Staff quét **QR code tự sinh** bằng **camera điện thoại qua web app** (responsive, không cần app native) → đối chiếu hệ thống → **tự động tạo Discrepancy Report** khi phát hiện sai lệch (vị trí sai, thiết bị "mất tích" không quét được).

## 6. Loại thiết bị quản lý

MVP quản lý đầy đủ các loại thiết bị sau trong **cùng 1 schema** (không tách bảng riêng theo loại):

> Laptop, màn hình, điện thoại, máy chủ, bàn phím/chuột, máy in, thiết bị mạng (switch/router/access point), USB, ổ cứng di động, máy chiếu.

License phần mềm sẽ được bổ sung ở **Phase 2** (ngoài phạm vi tài liệu này), tái sử dụng cơ chế `categories` mở (xem `05-DATABASE.md` mục Validation/Category).

## 7. Kiến trúc Multi-branch (tổng quan)

Dù MVP chỉ vận hành **1 chi nhánh**, kiến trúc dữ liệu và phân quyền được thiết kế sẵn cho multi-branch ngay từ đầu — **data isolation 2 cấp**:

- Mỗi chi nhánh có dữ liệu **tách biệt** thông qua cột `branch_id` trên các bảng liên quan.
- **IT_STAFF**: CRUD đầy đủ trong phạm vi chi nhánh mình.
- **ADMIN** (Trung tâm): read-only toàn hệ thống + khả năng **override** can thiệp trực tiếp khi cần.

→ Chi tiết kỹ thuật đầy đủ: `01-ARCHITECTURE.md` (mục Data Isolation) và `06-AUTHENTICATION.md` (Authorization Matrix).

## 8. Tech Stack tổng quan

Bảng tóm tắt — chi tiết kỹ thuật & lý do lựa chọn xem tại `01-ARCHITECTURE.md` và `11-DEPLOYMENT.md`.

| Layer | Công nghệ |
|---|---|
| **Backend** | Java 21 LTS, Spring Boot 3.5.16, Maven, Spring Security, Spring Data JPA/Hibernate, Jakarta Bean Validation, Lombok, Springdoc OpenAPI (Swagger UI), MapStruct, ZXing (QR generation) |
| **Frontend** | React + Vite + TypeScript, Tailwind CSS, shadcn/ui, lucide-react, Axios, html5-qrcode |
| **Database** | PostgreSQL (Neon Cloud, hỗ trợ branching) |
| **Authentication** | JWT tự xây dựng (email/password độc lập, **không SSO**) |
| **File Storage** | Cloudinary (ảnh thiết bị, hoá đơn, ảnh QR) |
| **Email** | SendGrid (free tier) |
| **CI/CD** | GitHub Actions |
| **Containerization** | Docker, GitHub Container Registry (GHCR) |
| **Backend Hosting** | Render |
| **Frontend Hosting** | Vercel |
| **Database Hosting** | Neon |

> 🎯 Mục tiêu thiết kế xuyên suốt: **chi phí thấp, dễ bảo trì, phù hợp demo với nhà tuyển dụng** — toàn bộ stack ưu tiên free-tier/low-cost cloud-native.

## 9. Lộ trình mở rộng (Roadmap)

Các hướng mở rộng đã được **dự kiến trước trong thiết kế** (kiến trúc chừa sẵn chỗ) nhưng **chưa triển khai** ở MVP:

| Hạng mục | Trạng thái MVP | Ghi chú |
|---|---|---|
| Quản lý license phần mềm | Chưa có | Dùng cơ chế `categories` mở sẵn |
| Kích hoạt thực sự Multi-branch (>1 chi nhánh) | Kiến trúc sẵn sàng, chưa vận hành thật | Đã có `branch_id` trên các bảng liên quan |
| Tích hợp hệ thống HR | Chưa có (nhập tay nhân viên) | Thiết kế chừa interface/sync point — **TODO: Need confirmation** khi tích hợp thật được xác định |
| Mobile app riêng | Chưa có (dùng web responsive) | Định hướng sau MVP |
| i18n / đa ngôn ngữ | Chưa có (hardcode Tiếng Việt) | Xem `03-CODING-STANDARDS.md` |
| Dark Mode | Chưa có (chỉ Light Mode) | Xem `08-UI-UX.md` |
| Auto-approve theo ngưỡng giá trị | Chưa có | **TODO: Need confirmation** |
| Logout toàn bộ thiết bị (logout-all-devices) | Chưa có | **TODO: Need confirmation** — xem `06-AUTHENTICATION.md` |
| Backup riêng ngoài Neon Free Tier | Chưa có | **TODO: Need confirmation** — xem `11-DEPLOYMENT.md` |

## 10. Bản đồ tài liệu (Document Map)

| File | Nội dung |
|---|---|
| `00-OVERVIEW.md` | Tài liệu này — tổng quan dự án, roadmap, glossary |
| `01-ARCHITECTURE.md` | Kiến trúc tổng thể, module boundary, luồng dữ liệu, JWT flow, deployment diagram |
| `02-FOLDER-STRUCTURE.md` | Cấu trúc thư mục Backend/Frontend chi tiết |
| `03-CODING-STANDARDS.md` | Naming convention, logging, comment, ngôn ngữ hệ thống |
| `04-API.md` | Danh sách đầy đủ API endpoints, request/response, pagination, versioning |
| `05-DATABASE.md` | Schema 13 bảng, quan hệ, index, migration, seed data |
| `06-AUTHENTICATION.md` | JWT, RBAC, Authorization Matrix, password policy |
| `07-BUSINESS-RULES.md` | Toàn bộ quy tắc nghiệp vụ, workflow, edge case |
| `08-UI-UX.md` | Design system, màu sắc, typography, layout, responsive |
| `09-ERROR-CODES.md` | Danh sách mã lỗi đầy đủ theo module, HTTP status mapping |
| `10-TESTING.md` | Chiến lược test, công cụ, coverage, E2E |
| `11-DEPLOYMENT.md` | CI/CD pipeline, secrets, môi trường, monitoring |
| `12-CONTRIBUTING.md` | Git flow, commit convention, PR, versioning |

## 11. Thuật ngữ (Glossary)

| Thuật ngữ | Giải thích |
|---|---|
| **Asset** | Thiết bị IT (laptop, máy in, điện thoại...). Không nhầm với thư mục `frontend/src/assets/` (static FE assets). |
| **Branch** | Chi nhánh công ty. Đơn vị data isolation chính. |
| **Department** | Phòng ban, thuộc 1 Branch, có 1 Manager quản lý. |
| **Request** | Yêu cầu cấp phát (`ASSIGN`) hoặc thu hồi (`RETURN`) thiết bị, đi qua workflow duyệt. |
| **Fulfill** | Hành động IT Staff hoàn tất thực hiện một Request đã được duyệt. |
| **Force-return** | IT Staff chủ động thu hồi thiết bị, bỏ qua workflow Employee→Manager (dùng khi nghỉ việc/khẩn cấp). |
| **Audit Session** | Một phiên kiểm kê định kỳ, phạm vi 1 chi nhánh. |
| **Discrepancy** | Sai lệch phát hiện khi đối chiếu kết quả quét QR với hệ thống (mất, sai vị trí, phát sinh ngoài dự kiến). |
| **Override** (Admin) | Khả năng Admin can thiệp trực tiếp vào dữ liệu chi nhánh dù mặc định chỉ read-only. |
| **Soft delete** | Đánh dấu xoá qua cột `deleted_at` thay vì xoá vật lý khỏi DB. |

## 12. TODO / Open Questions tổng hợp

Danh sách tổng hợp **tất cả** các điểm cần xác nhận thêm xuất hiện xuyên suốt bộ tài liệu (chi tiết từng điểm nằm trong file tương ứng):

1. Auto-approve request theo ngưỡng giá trị thấp / trường hợp khẩn cấp (`00-OVERVIEW.md`, `07-BUSINESS-RULES.md`).
2. Thời điểm & phương thức tích hợp hệ thống HR thật (`00-OVERVIEW.md`).
3. Logout toàn bộ thiết bị (logout-all-devices) (`06-AUTHENTICATION.md`).
4. Chi tiết animation ngoài nguyên tắc tối giản đã áp dụng (`08-UI-UX.md`).
5. Nâng cấp chiến lược backup ngoài Neon Free Tier mặc định (`11-DEPLOYMENT.md`).
6. Lựa chọn kiểu Primary Key (BIGINT identity vs UUID) cho toàn bộ entity (`05-DATABASE.md`).
7. Chính sách rotate Refresh Token khi sử dụng (chưa được đặc tả rõ trong nghiên cứu gốc) (`06-AUTHENTICATION.md`).

---

*Tài liệu được tạo dựa trên kết quả phỏng vấn/nghiên cứu dự án (`nghien_cuu.md`). Mọi mục đánh dấu `> TODO: Need confirmation` cần được Product Owner / Tech Lead xác nhận trước khi AI Coding Agent triển khai phần liên quan.*