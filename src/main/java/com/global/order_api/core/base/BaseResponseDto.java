package com.global.order_api.core.base;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseResponseDto<ID> {
	private ID id;
	private LocalDateTime createdAt;
}
