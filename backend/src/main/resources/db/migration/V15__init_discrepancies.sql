-- V15__init_discrepancies.sql
-- Tạo bảng discrepancies (sai lệch phát hiện khi hoàn tất phiên kiểm kê)
-- Được tạo tự động tại tầng Service khi AuditService.complete():
--   - MISSING: asset thuộc chi nhánh nhưng không được quét trong session
--   - LOCATION_MISMATCH: asset bị quét ở vị trí khác với vị trí đã đăng ký
--   - UNEXPECTED_FOUND: asset bị quét nhưng không thuộc danh sách dự kiến
-- resolution_action: chỉ áp dụng khi type = MISSING
--   - CONFIRM_LOST → asset.status = LOST
--   - FOUND → giữ nguyên status cũ

CREATE TABLE discrepancies (
    id                BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    audit_session_id  BIGINT       NOT NULL REFERENCES audit_sessions(id),
    asset_id          BIGINT       NOT NULL REFERENCES assets(id),
    type              VARCHAR(20)  NOT NULL
                          CHECK (type IN ('LOCATION_MISMATCH', 'MISSING', 'UNEXPECTED_FOUND')),
    status            VARCHAR(20)  NOT NULL DEFAULT 'OPEN'
                          CHECK (status IN ('OPEN', 'RESOLVED')),
    expected_location VARCHAR(255),
    actual_location   VARCHAR(255),
    resolution_action VARCHAR(20)
                          CHECK (resolution_action IN ('CONFIRM_LOST', 'FOUND')),
    resolved_by       BIGINT       REFERENCES employees(id),
    resolved_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ
);
