package com.vanh.itam.asset.dto.response;

import com.vanh.itam.asset.entity.AssetStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
public class AssetResponse {
    private Long id;
    private String code;
    private String name;
    private Long categoryId;
    private String categoryName;
    private Long branchId;
    private String branchName;
    private AssetStatus status;
    private Long assignedToId;
    private String assignedToName;
    private LocalDate purchaseDate;
    private BigDecimal value;
    private Instant createdAt;
    private Instant updatedAt;
}
