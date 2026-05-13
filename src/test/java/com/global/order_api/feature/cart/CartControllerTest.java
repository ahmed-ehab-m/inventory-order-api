package com.global.order_api.feature.cart;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.core.security.JwtFilter;
import com.global.order_api.core.security.SecurityUtils;
import com.global.order_api.core.utils.AppTranslator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;


import java.util.ArrayList;
import java.util.List;

@WebMvcTest(value = CartController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class),
        excludeAutoConfiguration = {SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppTranslator appTranslator;

    private static final String ENTITY_KEY = "entity.cart";
    private static final String CART_ITEM_ENTITY_KEY = "entity.cart_item"; // ده للعناصر
    /// to mock user id for security
    private static final Long CURRENT_USER_ID = 1L;

    ////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// READING METHODS ///////////////////////////////////

    @Nested
    @DisplayName("1. Get Cart Tests (GET)")
    class GetCartTests {

        ///// Get User Cart - Success
        @Test
        void getUserCart_ShouldReturnCart() throws Exception {
            /// fake dto returned from service
            CartResponseDto fakeDto = new CartResponseDto();
            fakeDto.setTotalCartPrice(500.0);
            fakeDto.setCartItems(new ArrayList<>());

            String fakeMessage = "Cart retrieved successfully";

            // Mock Static Method for SecurityUtils
            /// to avoid Unauthorized or NullPointerException
            /// MockedStatic => to mock static methods like in specified class will be tested
            /// try => because static methods on JVM Scope and affect other tests
            /// so using try with resource to remove this mock after this test
            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                when(cartService.getUserCart(CURRENT_USER_ID)).thenReturn(fakeDto);
                when(appTranslator.getTranslatedAction(eq("success.retrieved"), eq(ENTITY_KEY)))
                        .thenReturn(fakeMessage);

                mockMvc.perform(get("/api/v1/cart")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage))
                        .andExpect(jsonPath("$.data.totalCartPrice").value(500.0));
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// WRITING METHODS ///////////////////////////////////

    @Nested
    @DisplayName("2. Add Cart Item Tests (POST)")
    class AddCartItemTests {

        ///// Add Item - Success
        @Test
        void addCartItem_WithValidData_ShouldReturnUpdatedCart() throws Exception {
            CartItemRequestDto requestDto = new CartItemRequestDto();
            requestDto.setProductId(100L);
            requestDto.setQuantity(2);

            CartResponseDto responseDto = new CartResponseDto();
            responseDto.setTotalCartPrice(250.0);
            CartItemResponseDto fakeItem = new CartItemResponseDto();
            fakeItem.setProductId(100L);
            fakeItem.setQuantity(2);
            responseDto.setCartItems(List.of(fakeItem));

            String fakeMessage = "Cart Item added successfully";

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                when(cartService.addCartItem(eq(CURRENT_USER_ID), ArgumentMatchers.any(CartItemRequestDto.class)))
                        .thenReturn(responseDto);
                when(appTranslator.getTranslatedAction(eq("success.added"), eq(CART_ITEM_ENTITY_KEY)))
                        .thenReturn(fakeMessage);

                mockMvc.perform(post("/api/v1/cart/add-cart-item")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage))
                        .andExpect(jsonPath("$.data.cartItems").isNotEmpty())
                        .andExpect(jsonPath("$.data.totalCartPrice").value(250.0));
            }
        }

        ///// Add Item - Validation Failed (Null Product ID / Invalid Quantity)
        @Test
        void addCartItem_WithInvalidData_ShouldReturnBadRequest() throws Exception {
            CartItemRequestDto invalidRequest = new CartItemRequestDto();
            invalidRequest.setProductId(null); // Invalid
            invalidRequest.setQuantity(-5); // Invalid (Must be positive)

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                mockMvc.perform(post("/api/v1/cart/add-cart-item")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                        .andDo(print())
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.errors").exists());
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// UPDATE METHODS ///////////////////////////////////

    @Nested
    @DisplayName("3. Update Cart Item Tests (PUT)")
    class UpdateCartItemTests {

        ///// Update Quantity - Success
        @Test
        void updateQuantity_WithValidData_ShouldReturnOk() throws Exception {
            Long cartItemId = 50L;
            Integer newQuantity = 4;
            CartResponseDto responseDto = new CartResponseDto();
            responseDto.setTotalCartPrice(600.0);

            CartItemResponseDto fakeItem = new CartItemResponseDto();
            fakeItem.setProductId(100L);
            fakeItem.setQuantity(newQuantity);
            responseDto.setCartItems(List.of(fakeItem));

            String fakeMessage = "Cart item updated successfully";

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                // Service returns Dto, but controller currently maps to Void in ApiResponse
                when(cartService.updateItemQuantity(CURRENT_USER_ID, cartItemId, newQuantity))
                        .thenReturn(responseDto);
                when(appTranslator.getTranslatedAction(eq("success.updated"), eq(CART_ITEM_ENTITY_KEY)))
                        .thenReturn(fakeMessage);

                mockMvc.perform(put("/api/v1/cart/update-cart-item")
                                .param("cartItemId", String.valueOf(cartItemId))
                                .param("quantity", String.valueOf(newQuantity))
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage))
                        .andExpect(jsonPath("$.data.totalCartPrice").value(600.0))
                        .andExpect(jsonPath("$.data.cartItems").isNotEmpty());
            }
        }

        ///// Update Quantity - Cart Item Not Found
        @Test
        void updateQuantity_WhenItemNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidCartItemId = 99L;
            Integer newQuantity = 4;
            String errorKey = "error.resource.not.found";
            String fakeMessage = "Cart item not found";

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                when(cartService.updateItemQuantity(CURRENT_USER_ID, invalidCartItemId, newQuantity))
                        .thenThrow(new ResourceNotFoundException(errorKey, new Object[]{"id", invalidCartItemId}));
                when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                        .thenReturn(fakeMessage);

                mockMvc.perform(put("/api/v1/cart/update-cart-item")
                                .param("cartItemId", String.valueOf(invalidCartItemId))
                                .param("quantity", String.valueOf(newQuantity))
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value(fakeMessage));
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// DELETE METHODS ////////////////////////////////////

    @Nested
    @DisplayName("4. Delete Cart Tests (DELETE)")
    class DeleteCartTests {

        ///// Remove Single Cart Item
        @Test
        void removeCartItem_ShouldReturnOk() throws Exception {
            Long cartItemId = 50L;
            String fakeMessage = "Cart item removed successfully";

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                doNothing().when(cartService).removeCartItem(CURRENT_USER_ID, cartItemId);
                when(appTranslator.getTranslatedAction(eq("success.deleted"), eq(CART_ITEM_ENTITY_KEY)))
                        .thenReturn(fakeMessage);

                mockMvc.perform(delete("/api/v1/cart/remove-cart-item")
                                .param("cartItemId", String.valueOf(cartItemId))
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage));
            }
        }

        ///// Clear Entire Cart
        @Test
        void removeCart_ShouldReturnOk() throws Exception {
            String fakeMessage = "Cart cleared successfully";

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                doNothing().when(cartService).clearCart(CURRENT_USER_ID);
                when(appTranslator.getTranslatedAction(eq("success.deleted"), eq(ENTITY_KEY)))
                        .thenReturn(fakeMessage);

                mockMvc.perform(delete("/api/v1/cart/remove-cart")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage));
            }
        }
    }
}