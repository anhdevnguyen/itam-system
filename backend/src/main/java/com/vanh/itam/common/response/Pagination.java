package com.vanh.itam.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Thông tin phân trang, nhúng trong Meta của các endpoint danh sách.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Pagination {

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    /** Factory từ Spring Page object */
    public static Pagination of(org.springframework.data.domain.Page<?> page) {
        return new Pagination(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    /** Factory từ 4 tham số rời (dùng trong Controller khi gọi page.getContent() trước) */
    public static Pagination of(int page, int size, long totalElements, int totalPages) {
        return new Pagination(page, size, totalElements, totalPages);
    }
}
