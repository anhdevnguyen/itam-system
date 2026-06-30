-- V8__init_assets.sql
-- Tạo bảng assets (thiết bị IT)
-- code: tự sinh theo format <BRANCH_CODE>-<CATEGORY_CODE>-<SEQUENCE> (VD: HN-LAP-0001), không do client gửi
-- status: lưu VARCHAR + CHECK constraint thay vì PostgreSQL ENUM type (dễ mở rộng thêm giá trị sau)
-- value: đơn vị VNĐ, NUMERIC(15,2)
-- assigned_to: nullable — null nghĩa là thiết bị chưa được cấp phát

CREATE TABLE assets (
    id            BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code          VARCHAR(50)    NOT NULL UNIQUE,
    name          VARCHAR(200)   NOT NULL,
    category_id   BIGINT         NOT NULL REFERENCES categories(id),
    branch_id     BIGINT         NOT NULL REFERENCES branches(id),
    assigned_to   BIGINT         REFERENCES employees(id),     -- nullable: ai đang giữ hiện tại
    status        VARCHAR(20)    NOT NULL DEFAULT 'AVAILABLE'
                      CHECK (status IN ('AVAILABLE', 'ASSIGNED', 'IN_MAINTENANCE', 'BROKEN', 'DISPOSED', 'LOST')),
    purchase_date DATE           NOT NULL CHECK (purchase_date <= CURRENT_DATE),
    value         NUMERIC(15, 2) NOT NULL CHECK (value > 0),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ
);
