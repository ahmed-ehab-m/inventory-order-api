package com.global.order_api.feature.order;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class OrderFilterRequest {
    private String keyWord;  // user name - email -phone - location - product name
    private OrderStatus status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Boolean isDeleted = false;
}
