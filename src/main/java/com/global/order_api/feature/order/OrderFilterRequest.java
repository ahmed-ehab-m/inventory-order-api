package com.global.order_api.feature.order;

import com.global.order_api.core.base.BaseFilterRequestDto;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class OrderFilterRequest extends BaseFilterRequestDto {
    private OrderStatus status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Boolean isDeleted = false;
}
