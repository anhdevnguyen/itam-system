# ITAM — IT Asset Management System

Hệ thống quản lý toàn bộ vòng đời thiết bị IT trong doanh nghiệp: mua → cấp phát → bảo trì → kiểm kê → thanh lý.

## Tech Stack

| Layer | Công nghệ |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Maven, Spring Security, Flyway, MapStruct, ZXing |
| Frontend | React 19, Vite, TypeScript, Tailwind CSS, shadcn/ui |
| Database | PostgreSQL (Neon Cloud) |
| Auth | JWT (Access Token in-memory + Refresh Token httpOnly cookie) |
| Storage | Cloudinary |
| Email | SendGrid |
| Hosting | Render (BE) + Vercel (FE) + Neon (DB) |

## Chạy Local (Docker Compose)

**Yêu cầu:** Docker Desktop đang chạy.

```bash
# 1. Clone repo
git clone https://github.com/<owner>/itam-system.git
cd itam-system

# 2. Tạo file .env từ template
cp .env.example .env
# Chỉnh sửa .env nếu cần (JWT_SECRET, cổng...)

# 3. Khởi động Backend + PostgreSQL
docker compose up -d

# 4. Kiểm tra health
curl http://localhost:8080/actuator/health

# 5. Chạy Frontend (dev server riêng)
cd frontend
cp .env.example .env          # VITE_API_BASE_URL=http://localhost:8080/api/v1
npm install
npm run dev                   # → http://localhost:5173
```

**Tài khoản demo mặc định** (sau khi Flyway seed chạy xong):

| Email | Password (tạm) | Role |
|---|---|---|
| `admin@itam.local` | `Admin@123456` | ADMIN |
| `it.staff@itam.local` | `Itstaff@123` | IT_STAFF |
| `manager.kd@itam.local` | `Manager@123` | MANAGER |
| `employee1@itam.local` | `Employee@123` | EMPLOYEE |

> ⚠️ Tất cả tài khoản có `mustChangePassword = true` — bắt buộc đổi mật khẩu lần đầu đăng nhập.

## API Documentation

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Tài liệu kỹ thuật

Xem thư mục [`docs/`](./docs/) — 13 file tài liệu đầy đủ từ kiến trúc, database, API đến deployment.

## CI/CD

- **Pull Request → main**: chạy Unit Test + Integration Test (Testcontainers)
- **Merge → main**: Test → Build Docker → Push GHCR → Auto-deploy Render
- **Frontend**: Vercel auto-deploy qua GitHub Integration

Xem [`docs/11-DEPLOYMENT.md`](./docs/11-DEPLOYMENT.md) để biết chi tiết và checklist deploy lần đầu.
