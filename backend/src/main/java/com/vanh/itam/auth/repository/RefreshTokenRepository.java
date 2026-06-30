package com.vanh.itam.auth.repository;

import com.vanh.itam.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Tìm RefreshToken theo hash — dùng khi validate /auth/refresh.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Xóa 1 token theo hash — dùng khi /auth/logout (xóa đúng thiết bị hiện tại).
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash")
    void deleteByTokenHash(String tokenHash);

    /**
     * Xóa toàn bộ token của 1 employee — dùng khi:
     * - Admin reset password
     * - Employee tự đổi mật khẩu (tất cả trừ phiên hiện tại — xử lý ở Service layer)
     * - Employee bị soft-delete (nghỉ việc)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.employee.id = :employeeId")
    void deleteAllByEmployeeId(Long employeeId);

    /**
     * Xóa token theo hash và employeeId — dùng khi đổi mật khẩu (giữ phiên hiện tại).
     * Xóa tất cả token của employee TRỪ token hiện tại.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.employee.id = :employeeId AND rt.tokenHash <> :currentTokenHash")
    void deleteAllByEmployeeIdExcept(Long employeeId, String currentTokenHash);
}
