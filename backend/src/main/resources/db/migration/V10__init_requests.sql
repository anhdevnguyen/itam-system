-- V10__init_requests.sql
-- Tạo bảng requests (yêu cầu cấp phát / thu hồi thiết bị)
-- Workflow state machine: PENDING → APPROVED / REJECTED → FULFILLED / CANCELLED
-- rejection_reason: bắt buộc điền tại tầng Service khi status = REJECTED (không enforce ở DB)
-- approved_by / fulfilled_by: nullable, được điền khi Manager duyệt / IT Staff thực hiện

CREATE TABLE requests (
    id               BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type             VARCHAR(10) NOT NULL
                         CHECK (type IN ('ASSIGN', 'RETURN')),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'FULFILLED', 'CANCELLED')),
    asset_id         BIGINT      NOT NULL REFERENCES assets(id),
    employee_id      BIGINT      NOT NULL REFERENCES employees(id),  -- người tạo yêu cầu
    approved_by      BIGINT      REFERENCES employees(id),            -- Manager duyệt (nullable)
    fulfilled_by     BIGINT      REFERENCES employees(id),            -- IT Staff thực hiện (nullable)
    note             TEXT,
    rejection_reason TEXT,                                             -- bắt buộc tại Service khi REJECTED
    approved_at      TIMESTAMPTZ,
    fulfilled_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ
);
