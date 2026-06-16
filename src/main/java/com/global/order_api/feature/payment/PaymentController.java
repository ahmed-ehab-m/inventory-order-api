package com.global.order_api.feature.payment;

import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.feature.order.OrderRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Log4j2
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderRepo orderRepo;
    private final PaymentRepo paymentRepo;

    /// paymob send POST request in background to my server
    /// to ensure our transaction process done successfully
    /// to update Order Status PENDING => SUCCESS || FAILED
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<?>> handlePaymobWebHook(
            @RequestParam(name = "hmac") String hmac,
            @RequestBody Map<String, Object> paymobResponse) {
        try {
            /// 1=> Using HMAC to ensure this request from Paymob
            boolean isAuthentic = paymentService.verifyPaymobHmac(hmac, paymobResponse);
            if (!isAuthentic) {
                log.warn("not allowed => different hamc");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            /// 2 & 3=> get order data & update payment status and order status in DB
            paymentService.processWebHook(paymobResponse);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("error occured during processing webhook ", e);
            return ResponseEntity.ok().build();

        }
    }
}
