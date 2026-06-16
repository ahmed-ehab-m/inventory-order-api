package com.global.order_api.feature.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponseDto {

    /// TO TELL FRONT-END WHAT HE SHOULD DO WHEN GET TARGET
    /// IFRAME || REDIRECT || REFERENCE
    private PaymentActionType actionType;

    /// IFRAME || REDIRECT
    private String targetUrl;

    /// FOR KIOSK (FAWRY)
    private String referenceNumber;
}
