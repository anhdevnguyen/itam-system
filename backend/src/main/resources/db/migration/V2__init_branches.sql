-- V2__init_branches.sql
-- Tạo bảng branches (chi nhánh công ty)

CREATE TABLE branches (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code       VARCHAR(10)  NOT NULL UNIQUE,  -- VD: HN, HCM, DN
    name       VARCHAR(100) NOT NULL,
    address    VARCHAR(255),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
