package com.global.order_api.core.base;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter // for jackson to read values and convert it into json
@Builder // to build object using field name (self-documented)
// builder create private all args constructor
// no setter because this read only not editable
public class PageResponse<T> {

    private List<T> data;
    private int currentPage;
    private long totalElements;
    private int totalPages;
    private boolean isFirst;
    private boolean isLast;

    // Page => class return from db contains All these values
    // page => contains the data + meta data
    public static <T> PageResponse<T> from(Page<?> springPage, List<T> dtoList) {
        return PageResponse.<T>builder()
                .data(dtoList)
                .currentPage(springPage.getNumber())
                .totalPages(springPage.getTotalPages())
                .totalElements(springPage.getTotalElements())
                .isFirst(springPage.isFirst())
                .isLast(springPage.isLast())
                .build();
    }
}
