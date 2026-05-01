package com.global.order_api.core.base;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor // for mapstruct and jackson library to create empty constructor then add values using 
//setters or all args constructor or reflection
@AllArgsConstructor // for unitTesting to create dto quickly and immutability if we use arg constructor
// and getter only to create dto for reading
@Getter
// no setters because this reading only dto
@Setter // for map struct
@SuperBuilder
public abstract class BaseResponseDto<ID> {
	private ID id;
	private LocalDateTime createdAt;	
	private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
