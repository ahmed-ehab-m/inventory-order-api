package com.global.order_api.core.base;

import java.time.LocalDateTime;

public class AdminBaseReponseDto<ID> extends BaseResponseDto<ID> {
	
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}