-- V3__init_categories.sql
-- Tạo bảng categories (danh mục loại thiết bị — tách bảng riêng, KHÔNG hardcode enum)
-- categories KHÔNG thuộc branch, dùng chung toàn hệ thống
-- code dùng làm <CATEGORY_CODE> khi tự sinh asset.code (VD: HN-LAP-0001)

CREATE TABLE categories (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code       VARCHAR(10)  NOT NULL UNIQUE,  -- VD: LAP, MON, PHN, SRV, KBM, PRN, NET, USB, HDD, PRJ
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
