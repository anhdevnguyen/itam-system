-- V11__init_asset_assignment_history.sql
-- Tạo bảng asset_assignment_history (lịch sử cấp phát thiết bị)
-- Logic tạo/cập nhật bản ghi này nằm ở tầng Service (RequestServiceImpl.fulfill()), KHÔNG dùng DB trigger
-- returned_at = NULL → thiết bị đang được giữ bởi employee_id
-- returned_at != NULL → đã trả lại

CREATE TABLE asset_assignment_history (
    id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    asset_id    BIGINT      NOT NULL REFERENCES assets(id),
    employee_id BIGINT      NOT NULL REFERENCES employees(id),  -- người giữ thiết bị trong giai đoạn này
    request_id  BIGINT      REFERENCES requests(id),             -- nullable: liên kết request gây ra assignment
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    returned_at TIMESTAMPTZ,                                      -- NULL = đang giữ
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);
