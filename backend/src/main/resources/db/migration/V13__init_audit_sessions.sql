-- V13__init_audit_sessions.sql
-- Tạo bảng audit_sessions (phiên kiểm kê định kỳ, phạm vi 1 chi nhánh)
-- expires_at = started_at + 3 ngày (tính tại Service khi tạo session)
-- @Scheduled job tự động complete các session quá hạn (expires_at < now() AND status = 'IN_PROGRESS')

CREATE TABLE audit_sessions (
    id           BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id    BIGINT      NOT NULL REFERENCES branches(id),  -- 1 session = phạm vi 1 chi nhánh
    status       VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
                     CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    created_by   BIGINT      NOT NULL REFERENCES employees(id),
    note         TEXT,
    started_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ NOT NULL,                           -- started_at + 3 ngày
    completed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ
);
