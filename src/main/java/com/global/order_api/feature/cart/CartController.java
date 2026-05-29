package com.global.order_api.feature.cart;

import com.global.order_api.core.annotation.TrackExecutionTime;
import com.global.order_api.core.response.ApiResponse;
import com.global.order_api.core.security.SecurityUtils;
import com.global.order_api.core.utils.AppTranslator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Cart Management",
        description = "Endpoints for managing user carts, items, and quantities")
@RestController
@AllArgsConstructor
@RequestMapping("api/v1/cart")
public class CartController {

    private final CartService cartService;
    private final AppTranslator appTranslator;
    private static final String ENTITY_KEY = "entity.cart";
    private static final String CART_ITEM_ENTITY_KEY = "entity.cart_item"; // ده للعناصر

    ///////////////////////////////////////
    ///GET METHODS
    @Operation(summary = "Get User Cart",
            description = "Retrieves the cart andupdateQuantity all its items for a specific user")
    @TrackExecutionTime
    @GetMapping("")
    public ResponseEntity<ApiResponse<CartResponseDto>> getUserCart()
    {
        Long userId = SecurityUtils.getCurrentUserId();
        CartResponseDto cartResponseDto=cartService.getUserCart(userId);
        String message = appTranslator.getTranslatedAction("success.retrieved", ENTITY_KEY);
        ApiResponse<CartResponseDto> apiResponse=ApiResponse.success(cartResponseDto,message);
        return ResponseEntity.ok(apiResponse);
    }

    ////////////////////////////////////////////
    /// WRITING METHODS
    /// Add Cart Item
    @Operation(summary = "Add Item to Cart",
            description = "Adds a new product to the user's cart or increases its quantity if it already exists")
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponseDto>> addCartItem(

            @Valid @RequestBody CartItemRequestDto cartItemRequestDto
    )
    {
        Long userId = SecurityUtils.getCurrentUserId();
        CartResponseDto cartResponseDto=cartService.addCartItem(userId,cartItemRequestDto);
        String message = appTranslator.getTranslatedAction("success.added", CART_ITEM_ENTITY_KEY);
        ApiResponse<CartResponseDto> apiResponse=ApiResponse.success(cartResponseDto,message);
        return ResponseEntity.ok(apiResponse);
    }

    /// Update cart item
    @Operation(summary = "Update Item Quantity",
            description = "Directly updates the quantity of a specific item in the cart")
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponseDto>> updateQuantity(
            @RequestParam Long cartItemId,
            @RequestParam Integer quantity
    )
    {
        Long userId = SecurityUtils.getCurrentUserId();
        CartResponseDto cartResponseDto= cartService.updateItemQuantity(userId,cartItemId,quantity);
        String message = appTranslator.getTranslatedAction("success.updated", CART_ITEM_ENTITY_KEY);
        ApiResponse<CartResponseDto>apiResponse=ApiResponse.success(cartResponseDto,message);
        return ResponseEntity.ok(apiResponse);
    }


    /// Remove Cart Item
    @Operation(summary = "Remove Item from Cart",
            description = "Removes a specific item completely from the user's cart")
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            @RequestParam Long cartItemId
    )
    {
        Long userId = SecurityUtils.getCurrentUserId();
        cartService.removeCartItem(userId,cartItemId);
        String message = appTranslator.getTranslatedAction("success.deleted", CART_ITEM_ENTITY_KEY);
        ApiResponse<Void>apiResponse=ApiResponse.success(null,message);
        return ResponseEntity.ok(apiResponse);
    }

    /// Remove Cart
    @Operation(summary = "Clear Cart",
            description = "Removes all items from the user's cart")
    @DeleteMapping("")
    public ResponseEntity<ApiResponse<Void>> removeCart(
    )
    {
        Long userId = SecurityUtils.getCurrentUserId();
        cartService.clearCart(userId);
        String message = appTranslator.getTranslatedAction("success.deleted", ENTITY_KEY);
        ApiResponse<Void>apiResponse=ApiResponse.success(null,message);
        return ResponseEntity.ok(apiResponse);
    }


}
