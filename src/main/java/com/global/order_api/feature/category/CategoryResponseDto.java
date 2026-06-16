package com.global.order_api.feature.category;

import com.global.order_api.core.base.BaseResponseDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDto extends BaseResponseDto<Long> {
    private Long id; // important for front-end
    private String name;
    private String description;
}