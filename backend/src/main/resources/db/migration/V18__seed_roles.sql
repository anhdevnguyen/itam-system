-- V18__seed_roles.sql
-- Seed 4 role cố định (không thay đổi trong suốt vòng đời ứng dụng)
-- name: tên hiển thị tiếng Việt

INSERT INTO roles (code, name) VALUES
    ('ADMIN',    'Quản trị viên'),
    ('IT_STAFF', 'Nhân viên IT chi nhánh'),
    ('MANAGER',  'Trưởng phòng'),
    ('EMPLOYEE', 'Nhân viên');
