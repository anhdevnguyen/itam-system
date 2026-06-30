package com.vanh.itam.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

/**
 * Metadata kèm theo mọi response: timestamp + pagination (chỉ có ở endpoint danh sách).
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Meta {

    private final Instant timestamp;
    private final Pagination pagination;

    private Meta(Pagination pagination) {
        this.timestamp = Instant.now();
        this.pagination = pagination;
    }

    public static Meta now() {
        return new Meta(null);
    }

    public static Meta withPagination(Pagination pagination) {
        return new Meta(pagination);
    }
}
