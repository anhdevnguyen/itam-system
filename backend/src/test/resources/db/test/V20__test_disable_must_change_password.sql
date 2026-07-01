-- V20__test_disable_must_change_password.sql
-- CHỈ DÙNG CHO TEST PROFILE — đặt trong classpath:db/test
-- Tắt must_change_password cho tất cả tài khoản seed để integration test không bị chặn bởi MustChangePasswordFilter
UPDATE employees SET must_change_password = false;
