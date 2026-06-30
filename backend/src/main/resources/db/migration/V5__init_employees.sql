-- V5__init_employees.sql
-- Tạo bảng employees
-- email dùng làm username đăng nhập (unique)
-- password_hash: BCrypt
-- department_id nullable: Admin/IT Staff trung tâm có thể không gắn phòng ban
-- must_change_password: true khi tạo mới hoặc reset password → bắt buộc đổi mật khẩu lần đầu

CREATE TABLE employees (
    id                   BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email                VARCHAR(255) NOT NULL UNIQUE,
    password_hash        VARCHAR(255) NOT NULL,
    full_name            VARCHAR(150) NOT NULL,
    role_id              BIGINT       NOT NULL REFERENCES roles(id),
    branch_id            BIGINT       NOT NULL REFERENCES branches(id),
    department_id        BIGINT       REFERENCES departments(id),  -- nullable
    must_change_password BOOLEAN      NOT NULL DEFAULT true,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ
);
