-- V4__init_departments.sql
-- Tạo bảng departments (CHƯA có FK manager_id → employees để tránh circular dependency)
-- Circular dependency: departments.manager_id → employees, employees.department_id → departments
-- Giải pháp: tạo departments trước không có manager_id FK,
--            tạo employees (V5), rồi ALTER TABLE thêm FK ở V6

CREATE TABLE departments (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    branch_id  BIGINT       NOT NULL REFERENCES branches(id),
    manager_id BIGINT,           -- FK sẽ được bổ sung ở V6 sau khi có bảng employees
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
