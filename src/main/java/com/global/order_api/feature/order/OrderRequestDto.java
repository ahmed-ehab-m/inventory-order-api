package com.global.order_api.feature.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class OrderRequestDto {

    @NotBlank(message = "{validation.field.required}")
    private String shippingAddress;
    private String orderNotes;
    @NotNull(message = "{validation.field.required}")
    private PaymentType paymentType;
}
