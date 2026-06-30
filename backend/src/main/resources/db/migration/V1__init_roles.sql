-- V1__init_roles.sql
-- Tạo bảng roles (seed data cố định: ADMIN, IT_STAFF, MANAGER, EMPLOYEE)
-- Không có deleted_at vì seed data cố định, không expose API delete

CREATE TABLE roles (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code       VARCHAR(20)  NOT NULL UNIQUE,  -- ADMIN | IT_STAFF | MANAGER | EMPLOYEE
    name       VARCHAR(100) NOT NULL,          -- Tên hiển thị tiếng Việt
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
