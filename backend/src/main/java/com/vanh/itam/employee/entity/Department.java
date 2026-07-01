package com.vanh.itam.employee.entity;

import com.vanh.itam.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity bảng departments — phòng ban.
 * Chú ý: manager_id có circular dependency với employees — được giải quyết qua ALTER TABLE migration V6.
 */
@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
public class Department extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    /**
     * Manager phụ trách phòng ban — nullable.
     * FK bổ sung qua ALTER TABLE (migration V6) để giải circular dependency.
     * FetchType.LAZY để tránh N+1 query.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;
}
