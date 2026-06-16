package com.global.order_api.feature.order;

import com.global.order_api.core.annotation.TrackExecutionTime;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.security.SecurityUtils;
import com.global.order_api.core.utils.AppTranslator;
import com.global.order_api.feature.cart.CartItemRequestDto;
import com.global.order_api.feature.cart.CartResponseDto;
import com.global.order_api.feature.cart.CartService;
import com.global.order_api.feature.payment.PaymentResponseDto;
import com.global.order_api.feature.user.UserFilterRequest;
import com.global.order_api.feature.user.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("api/v1/orders")
@Tag(name = "Order Management", description = "APIs for managing e-commerce orders for both Admins and Customers")
public class OrderController {

    private final OrderService orderService;
    private final AppTranslator appTranslator;
    private static final String ENTITY_KEY = "entity.order";

    ///GET METHODS
    /// get all orders for ADMIN
    @TrackExecutionTime
    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get All Orders (Admin)",
            description = "Retrieves a paginated list of all orders in the system with filtering. Accessible only by Admins.")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponseDto>>> getAllOrders(
            @Valid @ModelAttribute OrderFilterRequest filter
    )
    {
        PageResponse<OrderResponseDto> pageResponse=orderService.getAllOrders(filter);
        String message=appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        ApiResponse<PageResponse<OrderResponseDto>> apiResponse=ApiResponse.success(pageResponse,message);
        return  ResponseEntity.ok(apiResponse);
    }
    /// get order by id for Admin
    @TrackExecutionTime
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Order By ID (Admin)",
            description = "Retrieves the details of a specific order by its ID. Accessible only by Admins.")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrderById(
            @PathVariable Long id
    ) {
        OrderResponseDto orderResponse = orderService.getOrderById(id);
        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        ApiResponse<OrderResponseDto> apiResponse = ApiResponse.success(orderResponse, message);
        return ResponseEntity.ok(apiResponse);
    }
    /// get order by id for User
    @TrackExecutionTime
    @GetMapping("/me/{id}")
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

    /// get user orders
    @TrackExecutionTime
    @GetMapping("/me")
    @Operation(summary = "Get My Orders (Customer)",
            description = "Retrieves a paginated list of orders for the currently authenticated user.")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponseDto>>> getUserOrder(
            @Valid @ModelAttribute OrderFilterRequest filter
    )
    {
        Long userId = SecurityUtils.getCurrentUserId();
        PageResponse<OrderResponseDto> pageResponse=orderService.getUserOrders(userId,filter);
        String message=appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        ApiResponse<PageResponse<OrderResponseDto>> apiResponse=ApiResponse.success(pageResponse,message);
        return  ResponseEntity.ok(apiResponse);
    }

    ////////////////////////////////////////////
    /// WRITING METHODS
    /// Create an order
    @PostMapping("")
    @Operation(summary = "Create an Order (Checkout)",
            description = "Creates a new order based on the user's cart items, clears the cart, and deducts from stock.")
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(
            @Valid @RequestBody OrderRequestDto orderRequestDto
    )
    {
        Long userId = SecurityUtils.getCurrentUserId();
        OrderResponseDto orderResponseDto=orderService.createOrder(userId,orderRequestDto);
        String message = appTranslator.getTranslatedAction("success.created", ENTITY_KEY);
        ApiResponse<OrderResponseDto> apiResponse=ApiResponse.success(orderResponseDto,message);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse); /// 201
    }

    /// cancel order
    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an Order",
            description = "Cancels a PENDING order and restores the product quantities to the stock.")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable(name = "id") Long orderId
    )
    {
        Long userId = SecurityUtils.getCurrentUserId();
        orderService.cancelOrder(userId,orderId);
        String message = appTranslator.getTranslatedAction("success.updated", ENTITY_KEY);
        ApiResponse<Void> apiResponse=ApiResponse.success(null,message);
        return ResponseEntity.ok(apiResponse);
    }
    //////////////////////////
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Order Status (Admin Only)",
            description = "Allows admins to move order to SHIPPED, DELIVERED, etc.")
    public ResponseEntity<ApiResponse<OrderResponseDto>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status
    ) {
        OrderResponseDto orderResponse = orderService.updateOrderStatusByAdmin(id, status);
        String message = appTranslator.getTranslatedAction("success.updated", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(orderResponse, message));
    }

    ///////////////
    @PutMapping("/{orderId}/return")
    @Operation(summary = "Return Order Status",
            description = "Allows Users to return orders")
    public ResponseEntity<ApiResponse<OrderResponseDto>> returnOrder(
            @PathVariable Long orderId
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
         orderService.returnDeliveredOrder(userId, orderId);
        String message = appTranslator.getTranslatedAction("success.updated", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    //////////////
    @GetMapping("/{id}/retry-payment")
    @Operation(summary = "Retry Order Payment",
            description = "Generates a new Paymob payment link for a pending order that previously failed.")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> retryPayment(
            @PathVariable("id") Long orderId,
            @RequestParam(value = "walletNumber", required = false) String walletNumber) {
        {

            Long userId = SecurityUtils.getCurrentUserId();

            PaymentResponseDto paymentUrl = orderService.retryPayment(userId, orderId, walletNumber);

            String message = appTranslator.getTranslatedAction("success.payment_link_generated", ENTITY_KEY);

            return ResponseEntity.ok(ApiResponse.success(paymentUrl, message));
        }
    }

    /// soft-delete order
    @PutMapping("/{id}/archive")
    @Operation(summary = "Soft Delete an Order",
            description = "Archives an order so it no longer appears in the user's history, but keeps it in the database for financial records.")
    public ResponseEntity<ApiResponse<Void>> softDeleteOrder(
            @PathVariable(name = "id") Long orderId
    )
    {
        Long userId = SecurityUtils.getCurrentUserId();
        orderService.softDeleteOrder(userId,orderId);
        String message = appTranslator.getTranslatedAction("success.deleted", ENTITY_KEY);
        ApiResponse<Void> apiResponse=ApiResponse.success(null,message);
        return ResponseEntity.ok(apiResponse);
    }

    /// hard-delete order
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Hard Delete an Order (Admin)",
            description = "Permanently deletes a CANCELLED order from the database. Cannot be undone.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> hardDeleteOrder(
            @PathVariable(name = "id") Long orderId
    )
    {
        orderService.hardDeleteOrder(orderId);
        String message = appTranslator.getTranslatedAction("success.deleted", ENTITY_KEY);
        ApiResponse<Void> apiResponse=ApiResponse.success(null,message);
        return ResponseEntity.ok(apiResponse);
    }

}
