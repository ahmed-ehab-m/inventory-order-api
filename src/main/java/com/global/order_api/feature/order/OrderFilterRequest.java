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

    @Override
    public String generateCacheKey() {
        String baseKey = super.generateCacheKey();

        return baseKey +
                "-status:" + (status != null ? status.name() : "all") +
                "-minPrice:" + (minPrice != null ? minPrice : "0") +
                "-maxPrice:" + (maxPrice != null ? maxPrice : "any") +
                "-fromDate:" + (fromDate != null ? fromDate.toString() : "any") +
                "-toDate:" + (toDate != null ? toDate.toString() : "any") +
                "-deleted:" + (isDeleted != null ? isDeleted : "false");
    }
}
