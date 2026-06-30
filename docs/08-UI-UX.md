# 08 — UI/UX

> Design system đầy đủ của ITAM: phong cách thiết kế, bảng màu, typography, component library, layout, responsive strategy, loading/empty state và accessibility. AI Coding Agent tuân thủ tài liệu này khi sinh code Frontend (React + Tailwind CSS + shadcn/ui).

## Mục lục

1. [Design Style tổng quan](#1-design-style-tổng-quan)
2. [Color Palette](#2-color-palette)
3. [Typography](#3-typography)
4. [Component Library](#4-component-library)
5. [Layout tổng thể](#5-layout-tổng-thể)
6. [Responsive Strategy](#6-responsive-strategy)
7. [Loading & Empty States](#7-loading--empty-states)
8. [Status Badge — Mapping Enum ↔ Màu sắc](#8-status-badge--mapping-enum--màu-sắc)
9. [Dark Mode](#9-dark-mode)
10. [Accessibility](#10-accessibility)
11. [Animation](#11-animation)
12. [Form & Validation UX](#12-form--validation-ux)
13. [TODO / Open Questions](#13-todo--open-questions)

---

## 1. Design Style tổng quan

| Hạng mục | Lựa chọn |
|---|---|
| Phong cách | **Minimal / Clean** — dashboard hiện đại, gọn gàng |
| Triết lý | Ưu tiên rõ ràng, dễ quét thông tin (scannable), hạn chế chi tiết trang trí không cần thiết |
| Đối tượng dùng | Nhân viên nội bộ doanh nghiệp — ưu tiên **hiệu quả thao tác** hơn là gây ấn tượng thị giác |

**Nguyên tắc xuyên suốt:** Mọi quyết định thiết kế (màu sắc, spacing, animation) đều phục vụ mục tiêu giúp người dùng (đặc biệt là IT Staff thao tác lặp đi lặp lại hàng ngày) hoàn thành công việc nhanh, ít nhầm lẫn — không phải showcase thẩm mỹ.

## 2. Color Palette

| Vai trò | Tên màu | Mã Tailwind | Mã Hex | Dùng cho |
|---|---|---|---|---|
| **Primary** | Indigo | `indigo-600` | `#4F46E5` | Nút hành động chính, link, trạng thái active của menu |
| **Background** | Trắng / Xám rất nhạt | `white` / `gray-50` | `#FFFFFF` / `#F9FAFB` | Nền trang, nền card |
| **Text chính** | Xám đậm | `gray-900` | `#111827` | Tiêu đề, nội dung quan trọng |
| **Text phụ** | Xám trung tính | `gray-500` | `#6B7280` | Mô tả, label phụ, placeholder |
| **Border/Divider** | Xám nhạt | `gray-200` | `#E5E7EB` | Viền input, viền table, divider |
| **Success** | Xanh lá | `green-600` | `#16A34A` | `AVAILABLE`, `APPROVED`, `COMPLETED`, `RESOLVED` |
| **Warning** | Vàng/Cam | `amber-500` | `#F59E0B` | `PENDING`, `IN_MAINTENANCE`, `SCHEDULED` |
| **Error/Danger** | Đỏ | `red-600` | `#DC2626` | `BROKEN`, `LOST`, `REJECTED`, `DISPOSED` |
| **Info** | Xanh dương nhạt | `blue-500` | `#3B82F6` | `ASSIGNED`, `IN_PROGRESS`, `FULFILLED` |

**Cấu hình `tailwind.config.js` (gợi ý):**

```js
// tailwind.config.js
module.exports = {
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#4F46E5', // indigo-600
          hover: '#4338CA',    // indigo-700
        },
      },
    },
  },
};
```

> Toàn bộ màu sắc nên dùng **trực tiếp class Tailwind chuẩn** (`bg-indigo-600`, `text-gray-900`...) thay vì định nghĩa lại biến CSS tuỳ chỉnh không cần thiết — giữ codebase đơn giản, dễ đọc, đúng tinh thần "Minimal".

## 3. Typography

| Hạng mục | Giá trị |
|---|---|
| Font chữ | **Roboto** (qua Google Fonts) |
| Lý do chọn | Sans-serif, hỗ trợ tốt dấu Tiếng Việt, độ phủ rộng, miễn phí |
| Thang cỡ chữ | Theo chuẩn Tailwind: `text-sm`, `text-base`, `text-lg`, `text-xl`, `text-2xl`... |

**Import font (gợi ý — `index.html`):**

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;500;600;700&display=swap" rel="stylesheet">
```

```js
// tailwind.config.js
module.exports = {
  theme: {
    extend: {
      fontFamily: {
        sans: ['Roboto', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
    },
  },
};
```

**Quy ước cỡ chữ theo ngữ cảnh:**

| Ngữ cảnh | Class Tailwind | Font weight |
|---|---|---|
| Page title | `text-2xl` | `font-semibold` |
| Section heading | `text-lg` | `font-semibold` |
| Body text | `text-base` | `font-normal` |
| Label / phụ chú | `text-sm` | `font-normal`, màu `text-gray-500` |
| Table header | `text-sm` | `font-medium`, màu `text-gray-500`, `uppercase` |

## 4. Component Library

| Hạng mục | Lựa chọn |
|---|---|
| Base | **shadcn/ui** (xây trên Tailwind CSS) |
| Icon | **lucide-react** |

**Lý do chọn shadcn/ui:** Component **copy trực tiếp vào source code** (không phải npm dependency đóng gói sẵn) — phù hợp triết lý "dễ tuỳ biến, không phụ thuộc black-box library", đồng thời output đã tuân thủ chuẩn accessibility cơ bản (ARIA roles, keyboard nav) sẵn có từ Radix UI (nền tảng của shadcn/ui).

**Danh sách component dùng chung phổ biến (đặt tại `frontend/src/shared/components/` — xem `02-FOLDER-STRUCTURE.md`):**

| Component | Dùng cho |
|---|---|
| `Button.tsx` | Nút hành động (primary/secondary/destructive variant) |
| `Modal.tsx` | Dialog xác nhận, form tạo/sửa nhanh |
| `Table.tsx` | Danh sách asset/employee/request... |
| `Pagination.tsx` | Điều hướng trang cho danh sách |
| `SkeletonLoader.tsx` | Trạng thái đang tải (mục 7) |
| `EmptyState.tsx` | Trạng thái không có dữ liệu (mục 7) |
| `StatusBadge.tsx` *(gợi ý bổ sung)* | Hiển thị màu trạng thái theo enum (mục 8) |

**Ví dụ Button component (gợi ý, tuân theo `03-CODING-STANDARDS.md`):**

```tsx
// shared/components/Button.tsx
import { type ButtonHTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'destructive';
}

export function Button({ variant = 'primary', className, ...props }: ButtonProps) {
  const variantClass = {
    primary: 'bg-indigo-600 hover:bg-indigo-700 text-white',
    secondary: 'bg-white border border-gray-200 hover:bg-gray-50 text-gray-900',
    destructive: 'bg-red-600 hover:bg-red-700 text-white',
  }[variant];

  return (
    <button
      className={cn('px-4 py-2 rounded-md text-sm font-medium transition-colors', variantClass, className)}
      {...props}
    />
  );
}
```

## 5. Layout tổng thể

```
┌──────────────────────────────────────────────────────────┐
│  Top Navbar                                                  │
│  [Logo ITAM]              [🔔 3]  [Nguyễn Văn A ▾]              │
├───────────┬──────────────────────────────────────────────┤
│            │                                                  │
│  Sidebar    │              Main Content Area                    │
│            │                                                  │
│  📊 Dashboard│   ┌────────────────────────────────────────┐  │
│  💻 Thiết bị  │   │  Page Title                                 │  │
│  📋 Yêu cầu   │   │  ──────────────────────────────────────  │  │
│  🔧 Bảo trì   │   │  [Filter/Search bar]      [+ Tạo mới]       │  │
│  📷 Kiểm kê   │   │  ┌──────────────────────────────────┐  │  │
│  👥 Nhân viên  │   │  │  Table / Card list                    │  │  │
│  ⚙️ Cài đặt    │   │  └──────────────────────────────────┘  │  │
│            │   │  [Pagination]                                │  │
│ (collapse  │   └────────────────────────────────────────┘  │
│  trên màn   │                                                  │
│  hình nhỏ)  │                                                  │
└───────────┴──────────────────────────────────────────────┘
```

| Vùng | Mô tả |
|---|---|
| **Top Navbar** | Logo, user menu (dropdown: hồ sơ cá nhân, đổi mật khẩu, đăng xuất), chuông notification (badge số chưa đọc, polling — xem `07-BUSINESS-RULES.md` mục 8.3) |
| **Sidebar** | Menu điều hướng chính theo từng module (Dashboard, Assets, Requests, Maintenance, Audit, Employees...) — menu hiển thị **theo role đăng nhập** (VD: Employee không thấy menu "Nhân viên") — **collapse được** trên màn hình nhỏ |
| **Main Content Area** | Page title + breadcrumb (nếu cần) + filter/search bar + bảng dữ liệu/form + pagination |

## 6. Responsive Strategy

**Chiến lược hỗn hợp (theo từng nhóm tính năng):**

| Nhóm tính năng | Chiến lược | Lý do |
|---|---|---|
| **Audit / QR Scan** | **Mobile-first** | IT Staff dùng **điện thoại** quét QR ngoài hiện trường (kho, văn phòng các tầng) — giao diện phải tối ưu cho màn hình nhỏ trước tiên |
| **Tất cả phần còn lại** (Assets, Requests, Employees, Dashboard...) | **Desktop-first** | Đây là tác vụ quản lý dữ liệu (xem bảng, lọc, nhập liệu) — IT Staff/Manager/Admin chủ yếu thao tác trên máy tính tại bàn làm việc |

**Breakpoints chuẩn Tailwind:**

| Breakpoint | Giá trị | Dùng cho |
|---|---|---|
| `sm` | `640px` | Điện thoại lớn / màn hình rất nhỏ |
| `md` | `768px` | Tablet |
| `lg` | `1024px` | Laptop nhỏ — **ngưỡng Sidebar tự collapse** |
| `xl` | `1280px` | Desktop chuẩn |

**Ví dụ áp dụng — Sidebar tự collapse dưới `lg`:**

```tsx
// shared/components/Sidebar.tsx (trích đoạn minh hoạ)
<aside className={cn(
  'fixed inset-y-0 left-0 z-40 w-64 bg-white border-r border-gray-200 transition-transform',
  'lg:translate-x-0 lg:static',
  isOpen ? 'translate-x-0' : '-translate-x-full'
)}>
  {/* menu items */}
</aside>
```

**Ví dụ áp dụng — trang Audit/QR Scan ưu tiên Mobile-first:**

```tsx
// audit/pages/AuditScanPage.tsx (trích đoạn minh hoạ)
<div className="flex flex-col p-4 gap-4 max-w-md mx-auto md:max-w-2xl">
  {/* Camera viewport full-width trên mobile trước, giới hạn max-width khi màn hình lớn hơn */}
  <QrScannerView />
  <ScanResultList />
</div>
```

## 7. Loading & Empty States

### 7.1 Skeleton Loading

**Quy tắc:** Dùng **Skeleton loading** (placeholder mô phỏng hình dạng nội dung thật) thay vì spinner đơn giản — giúp người dùng cảm nhận tốc độ tải nhanh hơn và giảm "layout shift" khi dữ liệu thật xuất hiện.

```tsx
// shared/components/SkeletonLoader.tsx
interface SkeletonLoaderProps {
  rows?: number;
}

export function SkeletonLoader({ rows = 5 }: SkeletonLoaderProps) {
  return (
    <div className="space-y-3">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="h-12 bg-gray-100 rounded-md animate-pulse" />
      ))}
    </div>
  );
}
```

### 7.2 Empty State

**Quy tắc:** Icon + text ngắn gọn, **không cần illustration phức tạp** (đúng tinh thần Minimal/Clean — tránh tải thêm asset đồ hoạ nặng không cần thiết).

```tsx
// shared/components/EmptyState.tsx
import { Inbox } from 'lucide-react';

interface EmptyStateProps {
  message?: string;
}

export function EmptyState({ message = 'Không có dữ liệu' }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-12 text-gray-500">
      <Inbox className="w-10 h-10 mb-2 text-gray-300" />
      <p className="text-sm">{message}</p>
    </div>
  );
}
```

**Ví dụ sử dụng thực tế (kết hợp với hook `useAssets`, xem `03-CODING-STANDARDS.md` mục 3):**

```tsx
if (isLoading) return <SkeletonLoader rows={5} />;
if (assets.length === 0) return <EmptyState message="Chưa có thiết bị nào trong chi nhánh" />;
```

## 8. Status Badge — Mapping Enum ↔ Màu sắc

> Bảng tổng hợp dùng để build component `StatusBadge.tsx` dùng chung — đảm bảo **nhất quán màu sắc** giữa các module khác nhau theo đúng Color Palette (mục 2).

| Enum | Giá trị | Màu Badge |
|---|---|---|
| `AssetStatus` | `AVAILABLE` | 🟢 Success (`green-600`) |
| `AssetStatus` | `ASSIGNED` | 🔵 Info (`blue-500`) |
| `AssetStatus` | `IN_MAINTENANCE` | 🟡 Warning (`amber-500`) |
| `AssetStatus` | `BROKEN` | 🔴 Error (`red-600`) |
| `AssetStatus` | `DISPOSED` | 🔴 Error (`red-600`), hiển thị nhạt hơn (`text-red-400` hoặc kèm icon riêng để phân biệt với `BROKEN`) |
| `AssetStatus` | `LOST` | 🔴 Error (`red-600`) |
| `RequestStatus` | `PENDING` | 🟡 Warning (`amber-500`) |
| `RequestStatus` | `APPROVED` | 🟢 Success (`green-600`) |
| `RequestStatus` | `REJECTED` | 🔴 Error (`red-600`) |
| `RequestStatus` | `FULFILLED` | 🔵 Info (`blue-500`) |
| `RequestStatus` | `CANCELLED` | ⚪ Neutral (`gray-500`) *(suy luận hợp lý — không thuộc 4 nhóm màu chính)* |
| `MaintenanceStatus` | `SCHEDULED` | 🟡 Warning (`amber-500`) |
| `MaintenanceStatus` | `IN_PROGRESS` | 🔵 Info (`blue-500`) |
| `MaintenanceStatus` | `COMPLETED` | 🟢 Success (`green-600`) |
| `MaintenanceStatus` | `CANCELLED` | ⚪ Neutral (`gray-500`) |
| `DiscrepancyStatus` | `OPEN` | 🟡 Warning (`amber-500`) |
| `DiscrepancyStatus` | `RESOLVED` | 🟢 Success (`green-600`) |
| `AuditSessionStatus` | `IN_PROGRESS` | 🔵 Info (`blue-500`) |
| `AuditSessionStatus` | `COMPLETED` | 🟢 Success (`green-600`) |

> ⚠️ Màu cho `CANCELLED` (Request/Maintenance) dùng **Neutral xám** thay vì 1 trong 4 màu chính (Success/Warning/Error/Info) vì về bản chất đây là trạng thái "đã dừng lại" trung tính, không phải kết quả tích cực hay tiêu cực — đây là **suy luận hợp lý bổ sung**, không nằm trong bảng màu gốc 4 nhóm đã liệt kê ở Chủ đề 10 (`nghien_cuu.md`).

```tsx
// shared/components/StatusBadge.tsx
const STATUS_COLOR_MAP: Record<string, string> = {
  AVAILABLE: 'bg-green-100 text-green-700',
  ASSIGNED: 'bg-blue-100 text-blue-700',
  IN_MAINTENANCE: 'bg-amber-100 text-amber-700',
  BROKEN: 'bg-red-100 text-red-700',
  DISPOSED: 'bg-red-50 text-red-400',
  LOST: 'bg-red-100 text-red-700',
  PENDING: 'bg-amber-100 text-amber-700',
  APPROVED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-700',
  FULFILLED: 'bg-blue-100 text-blue-700',
  CANCELLED: 'bg-gray-100 text-gray-600',
  // ... các giá trị còn lại tương tự
};

interface StatusBadgeProps {
  status: string;
  label: string; // text hiển thị Tiếng Việt
}

export function StatusBadge({ status, label }: StatusBadgeProps) {
  const colorClass = STATUS_COLOR_MAP[status] ?? 'bg-gray-100 text-gray-600';
  return (
    <span className={cn('px-2 py-1 rounded-full text-xs font-medium', colorClass)}>
      {label}
    </span>
  );
}
```

## 9. Dark Mode

❌ **Không hỗ trợ ở MVP** — chỉ Light Mode.

→ Không cần cấu hình `darkMode` trong `tailwind.config.js`, không cần class `dark:` ở bất kỳ component nào — giữ codebase đơn giản đúng phạm vi MVP. Định hướng bổ sung sau MVP nếu cần (xem `00-OVERVIEW.md` mục 9 — Roadmap).

## 10. Accessibility

**Mức độ áp dụng ở MVP:** Thực hành cơ bản, **không cần tuân thủ WCAG 2.1 AA đầy đủ**.

| Hạng mục | Yêu cầu tối thiểu |
|---|---|
| Contrast màu | Đủ rõ giữa text và background (Color Palette ở mục 2 đã được chọn với độ tương phản hợp lý — VD: `gray-900` trên `white`/`gray-50`) |
| Label form input | Mọi `<input>`/`<select>` phải có `<label>` rõ ràng liên kết qua `htmlFor`/`id`, hoặc `aria-label` nếu label ẩn |
| Keyboard navigation | Cơ bản — các action quan trọng (submit form, đóng modal, điều hướng menu) phải thao tác được bằng bàn phím (Tab, Enter, Esc); shadcn/ui (nền Radix UI) đã hỗ trợ sẵn phần lớn yêu cầu này cho component dialog/dropdown |
| Alt text hình ảnh | Ảnh thiết bị (asset images) cần `alt` mô tả ngắn gọn (VD: `alt="Ảnh thiết bị {asset.name}"`) |

**Ví dụ form input đúng chuẩn:**

```tsx
<div className="space-y-1">
  <label htmlFor="asset-name" className="text-sm font-medium text-gray-900">
    Tên thiết bị
  </label>
  <input
    id="asset-name"
    type="text"
    className="w-full px-3 py-2 border border-gray-200 rounded-md text-sm"
    aria-required="true"
  />
</div>
```

## 11. Animation

> ⚠️ Animation **chưa được đặc tả cụ thể** trong nghiên cứu gốc (Chủ đề 10) — nguyên tắc dưới đây là **suy luận hợp lý**, áp dụng nhất quán với phong cách Minimal/Clean đã chọn.

**Nguyên tắc:** **Animation tối giản, có mục đích** — chỉ dùng transition để hỗ trợ nhận thức người dùng (feedback rõ ràng khi có thay đổi trạng thái UI), **không** thêm animation trang trí thừa.

| Tình huống | Animation đề xuất |
|---|---|
| Chuyển trang (route change) | Không cần transition phức tạp — load tức thì hoặc fade nhẹ (`transition-opacity duration-150`) |
| Mở/đóng Modal | Fade + scale nhẹ (`transition-all duration-200`) |
| Hover Button/Link | `transition-colors duration-150` (đổi màu nền mượt khi hover) |
| Sidebar collapse/expand | `transition-transform duration-200` |
| Skeleton loading | `animate-pulse` (class Tailwind có sẵn) |
| Toast/Notification xuất hiện | Slide-in nhẹ từ góc màn hình, tự ẩn sau vài giây |

```tsx
// Ví dụ Modal với transition tối giản (gợi ý, dùng cùng shadcn/ui Dialog primitive)
<DialogContent className="transition-all duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0">
  {/* nội dung modal */}
</DialogContent>
```

## 12. Form & Validation UX

> Bổ sung nhất quán với `01-ARCHITECTURE.md` mục 7 (Error Response trả **toàn bộ** lỗi validation cùng lúc trong mảng `errors`).

**Quy tắc hiển thị lỗi validation:**

- Khi submit form thất bại do validation (`400 Bad Request`, `errors` là mảng nhiều lỗi), Frontend **hiển thị lỗi ngay tại từng field tương ứng** (dựa vào `field` trong mỗi phần tử `errors`), không chỉ hiển thị 1 toast chung chung.
- Field bị lỗi: viền đổi sang `border-red-500`, kèm message lỗi nhỏ màu `text-red-600` ngay bên dưới input.
- Nếu lỗi không gắn được với field cụ thể (lỗi nghiệp vụ tổng quát, VD: `ASSET_NOT_AVAILABLE`), hiển thị qua **toast/banner lỗi** ở đầu form.

```tsx
// Ví dụ field-level error display (trích đoạn minh hoạ)
<div className="space-y-1">
  <label htmlFor="email" className="text-sm font-medium text-gray-900">Email</label>
  <input
    id="email"
    className={cn(
      'w-full px-3 py-2 border rounded-md text-sm',
      errors.email ? 'border-red-500' : 'border-gray-200'
    )}
  />
  {errors.email && <p className="text-xs text-red-600">{errors.email}</p>}
</div>
```

## 13. TODO / Open Questions

> TODO: Need confirmation — **Chi tiết animation cụ thể hơn**: mục 11 hiện áp dụng nguyên tắc tối giản suy luận hợp lý (transition cơ bản, không trang trí thừa) theo đúng tinh thần Minimal/Clean, nhưng **chưa được Product Owner/Tech Lead xác nhận tường minh** — nếu có yêu cầu animation đặc thù hơn (VD: micro-interaction khi quét QR thành công), cần bổ sung riêng.

> TODO: Need confirmation — **Logo và branding cụ thể** (favicon, tên hiển thị chính xác trên Navbar) chưa được đề cập trong nghiên cứu gốc — hiện dùng placeholder `[Logo ITAM]` mang tính minh hoạ.

---

*Xem tiếp: `09-ERROR-CODES.md` để biết danh sách đầy đủ mã lỗi theo từng module và HTTP status mapping.*