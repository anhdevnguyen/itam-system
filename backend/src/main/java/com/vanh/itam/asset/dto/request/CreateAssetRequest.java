package com.vanh.itam.asset.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateAssetRequest {

    @NotBlank(message = "Tên thiết bị không được để trống")
    @Size(max = 200, message = "Tên thiết bị tối đa 200 ký tự")
    private String name;

    @NotNull(message = "Danh mục thiết bị là bắt buộc")
    private Long categoryId;

    @NotNull(message = "Chi nhánh là bắt buộc")
    private Long branchId;

    @NotNull(message = "Ngày mua là bắt buộc")
    @PastOrPresent(message = "Ngày mua không được là ngày trong tương lai")
    private LocalDate purchaseDate;

    @NotNull(message = "Giá trị thiết bị là bắt buộc")
    @DecimalMin(value = "0.01", message = "Giá trị thiết bị phải lớn hơn 0")
    private BigDecimal value;
}
