package com.global.order_api.feature.order;

import com.global.order_api.core.annotation.TrackExecutionTime;
import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.utils.AppTranslator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("api/v1/admin/orders") // Separate Admin Route
@PreAuthorize("hasRole('ADMIN')") // Protect the entire controller
@Tag(name = "Admin Order Management", description = "APIs for managing e-commerce orders for Admins")
public class AdminOrderController {

    private static final String ENTITY_KEY = "entity.order";
    private final AdminOrderService adminOrderService;
    private final AppTranslator appTranslator;

    // ==================================================================================
    //                                1. GET METHODS
    // ==================================================================================

    @TrackExecutionTime
    @GetMapping
    @Operation(summary = "Get All Orders (Admin)",
            description = "Retrieves a paginated list of all orders in the system with filtering. Accessible only by Admins.")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponseDto>>> getAllOrders(
            @Valid @ModelAttribute OrderFilterRequest filter
    ) {
        PageResponse<OrderResponseDto> pageResponse = adminOrderService.getAllOrders(filter);
        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        ApiResponse<PageResponse<OrderResponseDto>> apiResponse = ApiResponse.success(pageResponse, message);
        return ResponseEntity.ok(apiResponse);
    }

    @TrackExecutionTime
    @GetMapping("/{id}")
    @Operation(summary = "Get Order By ID (Admin)",
            description = "Retrieves the details of a specific order by its ID. Accessible only by Admins.")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrderById(
            @PathVariable Long id
    ) {
        OrderResponseDto orderResponse = adminOrderService.getOrderById(id);
        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        ApiResponse<OrderResponseDto> apiResponse = ApiResponse.success(orderResponse, message);
        return ResponseEntity.ok(apiResponse);
    }


    // ==================================================================================
    //                                2. WRITING METHODS
    // ==================================================================================

    @PutMapping("/{id}/status")
    @Operation(summary = "Update Order Status (Admin Only)",
            description = "Allows admins to move order to SHIPPED, DELIVERED, etc.")
    public ResponseEntity<ApiResponse<OrderResponseDto>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status
    ) {
        OrderResponseDto orderResponse = adminOrderService.updateOrderStatusByAdmin(id, status);
        String message = appTranslator.getTranslatedAction("success.updated", ENTITY_KEY);
        return ResponseEntity.ok(ApiResponse.success(orderResponse, message));
    }

    /// hard-delete order
    @Operation(summary = "Hard Delete an Order (Admin)",
            description = "Permanently deletes a CANCELLED order from the database. Cannot be undone.")
    @DeleteMapping("/{id}/force")
    public ResponseEntity<ApiResponse<Void>> hardDeleteOrder(
            @PathVariable(name = "id") Long orderId
    ) {
        adminOrderService.hardDeleteOrder(orderId);
        String message = appTranslator.getTranslatedAction("success.deleted", ENTITY_KEY);
        ApiResponse<Void> apiResponse = ApiResponse.success(null, message);
        return ResponseEntity.ok(apiResponse);
    }
}