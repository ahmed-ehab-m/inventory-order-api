package com.global.order_api.feature.order;

import com.global.order_api.core.annotation.TrackExecutionTime;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.security.SecurityUtils;
import com.global.order_api.core.utils.AppTranslator;
import com.global.order_api.feature.payment.PaymentResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("api/v1/orders") // Base route for user
@Tag(name = "Order Management", description = "APIs for managing e-commerce orders for Customers")
public class OrderController {

    private static final String ENTITY_KEY = "entity.order";
    private final OrderService orderService;
    private final AppTranslator appTranslator;

    // ==================================================================================
    //                                1. GET METHODS
    // ==================================================================================

    @TrackExecutionTime
    @GetMapping
    @Operation(summary = "Get My Orders (Customer)",
            description = "Retrieves a paginated list of orders for the currently authenticated user.")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponseDto>>> getUserOrder(
            @Valid @ModelAttribute OrderFilterRequest filter
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        PageResponse<OrderResponseDto> pageResponse = orderService.getUserOrders(userId, filter);
        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        ApiResponse<PageResponse<OrderResponseDto>> apiResponse = ApiResponse.success(pageResponse, message);
        return ResponseEntity.ok(apiResponse);
    }

    @TrackExecutionTime
    @GetMapping("/{id}") // Removed /me
    @Operation(summary = "Get My Order Details (Customer)",
            description = "Retrieves the details of a specific order for the currently authenticated user.")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getUserOrderById(
            @PathVariable Long id
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponseDto orderResponse = orderService.getUserOrderById(userId, id);

        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        ApiResponse<OrderResponseDto> apiResponse = ApiResponse.success(orderResponse, message);
        return ResponseEntity.ok(apiResponse);
    }


    // ==================================================================================
    //                                2. WRITING METHODS
    // ==================================================================================

    /// Create an order
    @PostMapping
    @Operation(summary = "Create an Order (Checkout)",
            description = "Creates a new order based on the user's cart items, clears the cart, and deducts from stock.")
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(
            @Valid @RequestBody OrderRequestDto orderRequestDto
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponseDto orderResponseDto = orderService.createOrder(userId, orderRequestDto);
        String message = appTranslator.getTranslatedAction("success.created", ENTITY_KEY);
        ApiResponse<OrderResponseDto> apiResponse = ApiResponse.success(orderResponseDto, message);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse); /// 201
    }

    /// cancel order
    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an Order",
            description = "Cancels a PENDING order and restores the product quantities to the stock.")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable(name = "id") Long orderId
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        orderService.cancelOrder(userId, orderId);
        String message = appTranslator.getTranslatedAction("success.updated", ENTITY_KEY);
        ApiResponse<Void> apiResponse = ApiResponse.success(null, message);
        return ResponseEntity.ok(apiResponse);
    }

    /// return order
    @PutMapping("/{id}/return") // Changed to {id} for consistency
    @Operation(summary = "Return Order Status",
            description = "Allows Users to return delivered orders")
    public ResponseEntity<ApiResponse<OrderResponseDto>> returnOrder(
            @PathVariable(name = "id") Long orderId // Changed to id
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        orderService.returnDeliveredOrder(userId, orderId);
        String message = appTranslator.getTranslatedAction("success.updated", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    /// retry payment
    @GetMapping("/{id}/retry-payment")
    @Operation(summary = "Retry Order Payment",
            description = "Generates a new Paymob payment link for a pending order that previously failed.")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> retryPayment(
            @PathVariable("id") Long orderId,
            @RequestParam(value = "walletNumber", required = false) String walletNumber) {

        Long userId = SecurityUtils.getCurrentUserId();
        PaymentResponseDto paymentUrl = orderService.retryPayment(userId, orderId, walletNumber);
        String message = appTranslator.getTranslatedAction("success.payment_link_generated", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(paymentUrl, message));
    }

    /// soft-delete order (Archive)
    @DeleteMapping("/{id}")
    @Operation(summary = "Soft Delete an Order",
            description = "Archives an order so it no longer appears in the user's history, but keeps it in the database for financial records.")
    public ResponseEntity<ApiResponse<Void>> softDeleteOrder(
            @PathVariable(name = "id") Long orderId
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        orderService.softDeleteOrder(userId, orderId);
        String message = appTranslator.getTranslatedAction("success.deleted", ENTITY_KEY);
        ApiResponse<Void> apiResponse = ApiResponse.success(null, message);
        return ResponseEntity.ok(apiResponse);
    }
}