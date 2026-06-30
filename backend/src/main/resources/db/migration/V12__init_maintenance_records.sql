-- V12__init_maintenance_records.sql
-- Tạo bảng maintenance_records (lịch sử bảo hành / bảo trì)
-- Khi status → IN_PROGRESS: Service tự động set asset.status = IN_MAINTENANCE (logic ở tầng Service)
-- Enum lưu VARCHAR + CHECK constraint (không dùng PostgreSQL native ENUM type để dễ mở rộng)

CREATE TABLE maintenance_records (
    id             BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    asset_id       BIGINT      NOT NULL REFERENCES assets(id),
    type           VARCHAR(20) NOT NULL
                       CHECK (type IN ('WARRANTY', 'REPAIR', 'PERIODIC')),
    status         VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
                       CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    description    TEXT,
    scheduled_date DATE,
    completed_date DATE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ
);
