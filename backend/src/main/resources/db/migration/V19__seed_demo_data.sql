-- V19__seed_demo_data.sql
-- Seed data demo để chạy ngay sau deploy mà không cần nhập liệu thủ công
-- Bao gồm: 1 branch, 10 categories, 3 departments, 6 employees (đủ 4 role), 10 assets (đủ trạng thái)
--
-- Lưu ý mật khẩu:
--   Password gốc: "Admin@123456"  → BCrypt hash bên dưới
--   Password gốc: "Itstaff@123"   → BCrypt hash bên dưới
--   Password gốc: "Manager@123"   → BCrypt hash bên dưới
--   Password gốc: "Employee@123"  → BCrypt hash bên dưới
--   Tất cả must_change_password = true → bắt buộc đổi mật khẩu lần đầu đăng nhập
--
--   Hash được tạo bởi BCrypt strength=10 (tương thích Spring Security BCryptPasswordEncoder)
--   Thay bằng hash thật khi chạy production seed script

-- ============================================================
-- 1. BRANCH
-- ============================================================
INSERT INTO branches (code, name, address) VALUES
    ('HN', 'Chi nhánh Hà Nội', '123 Đường Láng, Đống Đa, Hà Nội');

-- ============================================================
-- 2. CATEGORIES (10 loại thiết bị theo docs/00-OVERVIEW.md mục 6)
-- ============================================================
INSERT INTO categories (code, name) VALUES
    ('LAP', 'Laptop'),
    ('MON', 'Màn hình'),
    ('PHN', 'Điện thoại'),
    ('SRV', 'Máy chủ'),
    ('KBM', 'Bàn phím & Chuột'),
    ('PRN', 'Máy in'),
    ('NET', 'Thiết bị mạng'),
    ('USB', 'USB'),
    ('HDD', 'Ổ cứng di động'),
    ('PRJ', 'Máy chiếu');

-- ============================================================
-- 3. DEPARTMENTS (3 phòng ban, manager_id cập nhật sau khi có employees)
-- ============================================================
INSERT INTO departments (name, branch_id) VALUES
    ('Phòng Kỹ thuật IT', (SELECT id FROM branches WHERE code = 'HN')),
    ('Phòng Kinh doanh',  (SELECT id FROM branches WHERE code = 'HN')),
    ('Phòng Hành chính',  (SELECT id FROM branches WHERE code = 'HN'));

-- ============================================================
-- 4. EMPLOYEES (6 người, đủ 4 role để demo toàn bộ workflow)
-- ============================================================

-- 4.1 Admin (không gắn department)
INSERT INTO employees (email, password_hash, full_name, role_id, branch_id, department_id, must_change_password)
VALUES (
    'admin@itam.local',
    '$2a$10$ZzAb8N.rfxruluWrcdKgvuPsjwEti7CyoKY2QNAKzopfSB9vGQaqm',  -- Admin@123456
    'Quản trị hệ thống',
    (SELECT id FROM roles WHERE code = 'ADMIN'),
    (SELECT id FROM branches WHERE code = 'HN'),
    NULL,
    true
);

-- 4.2 IT Staff (thuộc Phòng Kỹ thuật IT)
INSERT INTO employees (email, password_hash, full_name, role_id, branch_id, department_id, must_change_password)
VALUES (
    'it.staff@itam.local',
    '$2a$10$wbjLZSeeOKEyoBczdscixeDBYg64wYkCiqDQCWH1rZutUCZktLcMC',  -- Itstaff@123
    'Nguyễn Văn Hùng',
    (SELECT id FROM roles WHERE code = 'IT_STAFF'),
    (SELECT id FROM branches WHERE code = 'HN'),
    (SELECT id FROM departments WHERE name = 'Phòng Kỹ thuật IT'),
    true
);

-- 4.3 Manager Phòng Kinh doanh
INSERT INTO employees (email, password_hash, full_name, role_id, branch_id, department_id, must_change_password)
VALUES (
    'manager.kd@itam.local',
    '$2a$10$9XcMLuREaiITDOOPEi3UTuxWt0MC2gCh0rfcaHGVLwyWlV5UmiF9q',  -- Manager@123
    'Trần Thị Mai',
    (SELECT id FROM roles WHERE code = 'MANAGER'),
    (SELECT id FROM branches WHERE code = 'HN'),
    (SELECT id FROM departments WHERE name = 'Phòng Kinh doanh'),
    true
);

-- 4.4 Manager Phòng Hành chính
INSERT INTO employees (email, password_hash, full_name, role_id, branch_id, department_id, must_change_password)
VALUES (
    'manager.hc@itam.local',
    '$2a$10$9t0N7WmWoljmnM4IkF1MdOYuHPLU4Io.sYfWG0LCpjH6jGnnEB/Fa',  -- Manager@123
    'Lê Quang Nam',
    (SELECT id FROM roles WHERE code = 'MANAGER'),
    (SELECT id FROM branches WHERE code = 'HN'),
    (SELECT id FROM departments WHERE name = 'Phòng Hành chính'),
    true
);

-- 4.5 Employee 1 (Phòng Kinh doanh)
INSERT INTO employees (email, password_hash, full_name, role_id, branch_id, department_id, must_change_password)
VALUES (
    'employee1@itam.local',
    '$2a$10$vg4Ii0qZqffwIEVSR4DWt.3kcIhnH.DB.LF3abv51Pc4v8FXgiBlC',  -- Employee@123
    'Phạm Thị Lan',
    (SELECT id FROM roles WHERE code = 'EMPLOYEE'),
    (SELECT id FROM branches WHERE code = 'HN'),
    (SELECT id FROM departments WHERE name = 'Phòng Kinh doanh'),
    true
);

-- 4.6 Employee 2 (Phòng Hành chính)
INSERT INTO employees (email, password_hash, full_name, role_id, branch_id, department_id, must_change_password)
VALUES (
    'employee2@itam.local',
    '$2a$10$fRRIM48OxeB.E8Nr/3vQ6uUOXR8pP3M15cdF2jYun11HYTp2e2eom',  -- Employee@123
    'Đỗ Minh Tuấn',
    (SELECT id FROM roles WHERE code = 'EMPLOYEE'),
    (SELECT id FROM branches WHERE code = 'HN'),
    (SELECT id FROM departments WHERE name = 'Phòng Hành chính'),
    true
);

-- ============================================================
-- 5. CẬP NHẬT MANAGER_ID cho các phòng ban
-- ============================================================
UPDATE departments
SET manager_id = (SELECT id FROM employees WHERE email = 'manager.kd@itam.local')
WHERE name = 'Phòng Kinh doanh';

UPDATE departments
SET manager_id = (SELECT id FROM employees WHERE email = 'manager.hc@itam.local')
WHERE name = 'Phòng Hành chính';

-- ============================================================
-- 6. ASSETS (10 thiết bị, đủ các trạng thái để demo)
-- ============================================================

-- 6.1 AVAILABLE — chưa cấp phát
INSERT INTO assets (code, name, category_id, branch_id, assigned_to, status, purchase_date, value)
VALUES (
    'HN-LAP-0001',
    'Dell Latitude 5440',
    (SELECT id FROM categories WHERE code = 'LAP'),
    (SELECT id FROM branches WHERE code = 'HN'),
    NULL,
    'AVAILABLE',
    '2024-01-15',
    22000000
);

INSERT INTO assets (code, name, category_id, branch_id, assigned_to, status, purchase_date, value)
VALUES (
    'HN-MON-0001',
    'Dell P2422H 24-inch',
    (SELECT id FROM categories WHERE code = 'MON'),
    (SELECT id FROM branches WHERE code = 'HN'),
    NULL,
    'AVAILABLE',
    '2024-01-15',
    5500000
);

INSERT INTO assets (code, name, category_id, branch_id, assigned_to, status, purchase_date, value)
VALUES (
    'HN-KBM-0001',
    'Logitech MK295 Combo',
    (SELECT id FROM categories WHERE code = 'KBM'),
    (SELECT id FROM branches WHERE code = 'HN'),
    NULL,
    'AVAILABLE',
    '2024-03-10',
    850000
);

-- 6.2 ASSIGNED — đang được cấp phát cho employee1
INSERT INTO assets (code, name, category_id, branch_id, assigned_to, status, purchase_date, value)
VALUES (
    'HN-LAP-0002',
    'HP EliteBook 840 G9',
    (SELECT id FROM categories WHERE code = 'LAP'),
    (SELECT id FROM branches WHERE code = 'HN'),
    (SELECT id FROM employees WHERE email = 'employee1@itam.local'),
    'ASSIGNED',
    '2023-06-20',
    25000000
);

INSERT INTO assets (code, name, category_id, branch_id, assigned_to, status, purchase_date, value)
VALUES (
    'HN-PHN-0001',
    'Samsung Galaxy A54',
    (SELECT id FROM categories WHERE code = 'PHN'),
    (SELECT id FROM branches WHERE code = 'HN'),
    (SELECT id FROM employees WHERE email = 'employee2@itam.local'),
    'ASSIGNED',
    '2023-09-05',
    9500000
);

INSERT INTO assets (code, name, category_id, branch_id, assigned_to, status, purchase_date, value)
VALUES (
    'HN-LAP-0003',
    'Lenovo ThinkPad T14',
    (SELECT id FROM categories WHERE code = 'LAP'),
    (SELECT id FROM branches WHERE code = 'HN'),
    (SELECT id FROM employees WHERE email = 'manager.kd@itam.local'),
    'ASSIGNED',
    '2023-03-12',
    28000000
);

-- 6.3 IN_MAINTENANCE — đang bảo trì
INSERT INTO assets (code, name, category_id, branch_id, assigned_to, status, purchase_date, value)
VALUES (
    'HN-PRN-0001',
    'HP LaserJet M404dn',
    (SELECT id FROM categories WHERE code = 'PRN'),
    (SELECT id FROM branches WHERE code = 'HN'),
    NULL,
    'IN_MAINTENANCE',
    '2022-11-01',
    7200000
);

-- 6.4 BROKEN — thiết bị hỏng
INSERT INTO assets (code, name, category_id, branch_id, assigned_to, status, purchase_date, value)
VALUES (
    'HN-MON-0002',
    'LG 24MK430H (màn hình nứt)',
    (SELECT id FROM categories WHERE code = 'MON'),
    (SELECT id FROM branches WHERE code = 'HN'),
    NULL,
    'BROKEN',
    '2021-07-15',
    3800000
);

-- 6.5 DISPOSED — đã thanh lý
INSERT INTO assets (code, name, category_id, branch_id, assigned_to, status, purchase_date, value)
VALUES (
    'HN-LAP-0000',
    'Dell Inspiron 3501 (thanh lý)',
    (SELECT id FROM categories WHERE code = 'LAP'),
    (SELECT id FROM branches WHERE code = 'HN'),
    NULL,
    'DISPOSED',
    '2019-05-20',
    15000000
);

-- 6.6 NET device — AVAILABLE
INSERT INTO assets (code, name, category_id, branch_id, assigned_to, status, purchase_date, value)
VALUES (
    'HN-NET-0001',
    'Cisco Switch SG110-16',
    (SELECT id FROM categories WHERE code = 'NET'),
    (SELECT id FROM branches WHERE code = 'HN'),
    NULL,
    'AVAILABLE',
    '2023-01-08',
    4200000
);

-- ============================================================
-- 7. ASSET ASSIGNMENT HISTORY (ghi lại lịch sử cho các ASSIGNED assets)
-- ============================================================
INSERT INTO asset_assignment_history (asset_id, employee_id, request_id, assigned_at, returned_at)
VALUES (
    (SELECT id FROM assets WHERE code = 'HN-LAP-0002'),
    (SELECT id FROM employees WHERE email = 'employee1@itam.local'),
    NULL,
    '2024-02-01 08:00:00+07',
    NULL  -- đang giữ
);

INSERT INTO asset_assignment_history (asset_id, employee_id, request_id, assigned_at, returned_at)
VALUES (
    (SELECT id FROM assets WHERE code = 'HN-PHN-0001'),
    (SELECT id FROM employees WHERE email = 'employee2@itam.local'),
    NULL,
    '2024-03-15 09:00:00+07',
    NULL  -- đang giữ
);

INSERT INTO asset_assignment_history (asset_id, employee_id, request_id, assigned_at, returned_at)
VALUES (
    (SELECT id FROM assets WHERE code = 'HN-LAP-0003'),
    (SELECT id FROM employees WHERE email = 'manager.kd@itam.local'),
    NULL,
    '2023-04-01 08:30:00+07',
    NULL  -- đang giữ
);

-- ============================================================
-- 8. MAINTENANCE RECORD (1 bản ghi cho thiết bị IN_MAINTENANCE)
-- ============================================================
INSERT INTO maintenance_records (asset_id, type, status, description, scheduled_date)
VALUES (
    (SELECT id FROM assets WHERE code = 'HN-PRN-0001'),
    'REPAIR',
    'IN_PROGRESS',
    'Máy in bị kẹt giấy, thay bộ phận nạp giấy',
    CURRENT_DATE
);
