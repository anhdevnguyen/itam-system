-- V14__init_audit_scans.sql
-- Tạo bảng audit_scans (lượt quét QR code trong 1 phiên kiểm kê)
-- Mỗi lần IT Staff quét QR của 1 thiết bị → ghi nhận 1 dòng
-- scanned_location: vị trí thực tế quan sát được khi quét

CREATE TABLE audit_scans (
    id               BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    audit_session_id BIGINT       NOT NULL REFERENCES audit_sessions(id),
    asset_id         BIGINT       NOT NULL REFERENCES assets(id),
    scanned_by       BIGINT       NOT NULL REFERENCES employees(id),
    scanned_location VARCHAR(255),
    scanned_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ
);
