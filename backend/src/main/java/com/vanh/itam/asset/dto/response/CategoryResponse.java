package com.vanh.itam.asset.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class CategoryResponse {
    private Long id;
    private String code;
    private String name;
    private Instant createdAt;
}
