-- V16__init_notifications.sql
-- Tạo bảng notifications (thông báo in-app, cơ chế polling 30–60s, không dùng WebSocket)
-- type values: REQUEST_CREATED | REQUEST_APPROVED | REQUEST_REJECTED | REQUEST_FULFILLED
--              | DISCREPANCY_FOUND | AUDIT_REMINDER
-- related_entity_id: ID của entity liên quan (VD: request_id, discrepancy_id) — nullable, không enforce FK
--   (polymorphic ref → không thể dùng FK ở DB level, kiểm tra ở tầng Service khi cần)

CREATE TABLE notifications (
    id                BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id       BIGINT       NOT NULL REFERENCES employees(id),
    type              VARCHAR(30)  NOT NULL
                          CHECK (type IN (
                              'REQUEST_CREATED',
                              'REQUEST_APPROVED',
                              'REQUEST_REJECTED',
                              'REQUEST_FULFILLED',
                              'DISCREPANCY_FOUND',
                              'AUDIT_REMINDER'
                          )),
    message           VARCHAR(500) NOT NULL,
    is_read           BOOLEAN      NOT NULL DEFAULT false,
    related_entity_id BIGINT,      -- polymorphic: request_id hoặc discrepancy_id, không có FK
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ
);
