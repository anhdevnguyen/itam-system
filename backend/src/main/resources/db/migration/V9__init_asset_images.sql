-- V9__init_asset_images.sql
-- Tạo bảng asset_images (ảnh thiết bị, lưu trên Cloudinary)
-- Giới hạn nghiệp vụ (KHÔNG enforce ở DB, kiểm tra tại Service):
--   - Tối đa 5 ảnh/thiết bị
--   - Mỗi ảnh <= 5MB

CREATE TABLE asset_images (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    asset_id   BIGINT       NOT NULL REFERENCES assets(id),
    url        VARCHAR(500) NOT NULL,  -- Cloudinary URL
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
