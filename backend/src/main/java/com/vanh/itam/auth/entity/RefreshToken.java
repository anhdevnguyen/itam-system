package com.vanh.itam.auth.entity;

import com.vanh.itam.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Entity bảng refresh_tokens.
 * Lưu ý quan trọng:
 * - Không có deletedAt — logout thực hiện hard delete bản ghi.
 * - Lưu token_hash (SHA-256) thay vì plaintext — bảo mật khi DB bị lộ.
 * - 1 employee có nhiều record (multi-device login).
 * - device_label dùng cho tính năng "xem thiết bị đang đăng nhập" trong tương lai.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /**
     * SHA-256 hash của refresh token gốc (plaintext).
     * KHÔNG lưu giá trị gốc — phòng trường hợp DB bị lộ.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    /**
     * Ghi chú thiết bị — rút gọn từ User-Agent header.
     * VD: "Chrome on Windows", "Safari on iPhone".
     */
    @Column(name = "device_label", length = 255)
    private String deviceLabel;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Factory method tạo RefreshToken mới */
    public static RefreshToken create(Employee employee, String tokenHash,
                                      String deviceLabel, Instant expiresAt) {
        RefreshToken rt = new RefreshToken();
        rt.employee = employee;
        rt.tokenHash = tokenHash;
        rt.deviceLabel = deviceLabel;
        rt.expiresAt = expiresAt;
        return rt;
    }

    /** Kiểm tra token còn hiệu lực không */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
