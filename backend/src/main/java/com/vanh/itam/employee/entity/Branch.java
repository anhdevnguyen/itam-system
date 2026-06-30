package com.vanh.itam.employee.entity;

import com.vanh.itam.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity bảng branches — chi nhánh.
 */
@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
public class Branch extends BaseEntity {

    /** Mã chi nhánh — UNIQUE, VD: HN, HCM, DN */
    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String address;
}
