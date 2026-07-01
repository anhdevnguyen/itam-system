package com.vanh.itam.employee.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class BranchResponse {
    private Long id;
    private String code;
    private String name;
    private String address;
    private Instant createdAt;
}
