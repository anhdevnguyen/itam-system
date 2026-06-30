-- V7__init_refresh_tokens.sql
-- Tạo bảng refresh_tokens
-- Lưu SHA-256 hash của refresh token (KHÔNG lưu plaintext) để bảo mật khi DB bị lộ
-- Logout thực hiện HARD DELETE bản ghi (không soft delete)
-- Hỗ trợ 1-n theo employee_id cho multi-device login

CREATE TABLE refresh_tokens (
    id           BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id  BIGINT       NOT NULL REFERENCES employees(id),
    token_hash   VARCHAR(255) NOT NULL UNIQUE,  -- SHA-256 hash, KHÔNG lưu plaintext
    device_label VARCHAR(255),                   -- User-Agent rút gọn, hỗ trợ nhận diện multi-device
    expires_at   TIMESTAMPTZ  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
    -- Không có deleted_at: logout = hard delete bản ghi này
);
