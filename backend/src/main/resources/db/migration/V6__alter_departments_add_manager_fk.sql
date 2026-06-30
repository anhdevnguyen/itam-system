-- V6__alter_departments_add_manager_fk.sql
-- Bổ sung FK manager_id → employees(id) cho bảng departments
-- Chạy SAU V5 vì lúc này bảng employees đã tồn tại

ALTER TABLE departments
    ADD CONSTRAINT fk_departments_manager
    FOREIGN KEY (manager_id) REFERENCES employees(id);
