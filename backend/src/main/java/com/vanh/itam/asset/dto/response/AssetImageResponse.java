package com.vanh.itam.asset.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AssetImageResponse {
    private Long id;
    private Long assetId;
    private String url;
    private Instant createdAt;
}
