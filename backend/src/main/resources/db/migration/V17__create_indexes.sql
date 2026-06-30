-- V17__create_indexes.sql
-- Tạo toàn bộ index theo chiến lược tại docs/05-DATABASE.md mục 8
-- Các cột UNIQUE constraint đã có index ngầm (code, email, token_hash) — không tạo lại

-- assets
CREATE INDEX idx_assets_status      ON assets(status);
CREATE INDEX idx_assets_branch_id   ON assets(branch_id);
CREATE INDEX idx_assets_assigned_to ON assets(assigned_to);
CREATE INDEX idx_assets_deleted_at  ON assets(deleted_at);

-- employees
CREATE INDEX idx_employees_branch_id     ON employees(branch_id);
CREATE INDEX idx_employees_department_id ON employees(department_id);
CREATE INDEX idx_employees_deleted_at    ON employees(deleted_at);

-- requests
CREATE INDEX idx_requests_employee_id ON requests(employee_id);
CREATE INDEX idx_requests_status      ON requests(status);
CREATE INDEX idx_requests_asset_id    ON requests(asset_id);

-- audit_scans
CREATE INDEX idx_audit_scans_session_id ON audit_scans(audit_session_id);
CREATE INDEX idx_audit_scans_asset_id   ON audit_scans(asset_id);

-- refresh_tokens
CREATE INDEX idx_refresh_tokens_employee_id ON refresh_tokens(employee_id);
