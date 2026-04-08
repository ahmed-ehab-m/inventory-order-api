package com.global.order_api.feature.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class ProductRequestDto {
	
    @NotBlank(message = "{validation.field.required}")
    @Size(max = 255)
	private String name;
    
	private String description;
	private String image;
	
	@NotNull(message = "{validation.field.required}")
	@Positive(message = "{validation.field.positive}")
	private BigDecimal price;
	
	@Min(value = 0,message = "{validation.field.equalsOrGreaterThanZero}")
	private int stockCount;
	
	@NotNull(message = "{validation.field.required}")
	private Long categoryId;
}
