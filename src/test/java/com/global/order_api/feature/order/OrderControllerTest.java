package com.global.order_api.feature.order;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.global.order_api.core.base.PageResponse;
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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@WebMvcTest(value = OrderController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class),
        excludeAutoConfiguration = {SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private AppTranslator appTranslator;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ENTITY_KEY = "entity.order";
    private static final Long CURRENT_USER_ID = 1L;

    ////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// READING METHODS ///////////////////////////////////

    @Nested
    @DisplayName("1. Get Orders Tests (GET)")
    class GetOrdersTests {

        ///// Get All Orders (Admin)
        @Test
        void getAllOrders_ShouldReturnPagedOrders() throws Exception {
            OrderResponseDto responseDto = new OrderResponseDto();
            responseDto.setId(100L);

            // 1. fake list to pass it intp fake page
            List<OrderResponseDto> dtoList = List.of(responseDto);

            // 2. fake page
            org.springframework.data.domain.Page<OrderResponseDto> dummyPage = new PageImpl<>(dtoList);

            // 3. build our page response to be returned from service
            PageResponse<OrderResponseDto> fakePageResponse = PageResponse.from(dummyPage, dtoList);

            String fakeMessage = "Orders retrieved successfully";

            when(orderService.getAllOrders(ArgumentMatchers.any(OrderFilterRequest.class)))
                    .thenReturn(fakePageResponse);
            when(appTranslator.getTranslatedAction(eq("success.retrieved"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(get("/api/v1/orders")
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.data[0].id").value(100L))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        ///// Get User Orders (Customer)
        @Test
        void getUserOrder_ShouldReturnUserOrders() throws Exception {
            OrderResponseDto responseDto = new OrderResponseDto();
            responseDto.setId(200L);

            List<OrderResponseDto> dtoList = List.of(responseDto);
            Page<OrderResponseDto> dummyPage = new PageImpl<>(dtoList);

            PageResponse<OrderResponseDto> fakePageResponse = PageResponse.from(dummyPage, dtoList);

            String fakeMessage = "Orders retrieved successfully";

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                when(orderService.getUserOrders(eq(CURRENT_USER_ID), ArgumentMatchers.any(OrderFilterRequest.class)))
                        .thenReturn(fakePageResponse);
                when(appTranslator.getTranslatedAction(eq("success.retrieved"), eq(ENTITY_KEY)))
                        .thenReturn(fakeMessage);

                mockMvc.perform(get("/api/v1/orders/my-orders")
                                .param("page", "0")
                                .param("size", "10")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage))
                        .andExpect(jsonPath("$.data.data[0].id").value(200L));
            }
        }

        ///// Get User Orders - Invalid Filter (Validation Failed)
        @Test
        void getUserOrder_WithInvalidFilter_ShouldReturnBadRequest() throws Exception {
            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                mockMvc.perform(get("/api/v1/orders/my-orders")
                                .param("page", "-1")
                                .param("size", "-5")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isBadRequest()) // 400
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.errors").exists());
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// WRITING METHODS ///////////////////////////////////

    @Nested
    @DisplayName("2. Create Order Tests (POST)")
    class CreateOrderTests {

        ///// Create Order - Success
        @Test
        void createOrder_WithValidData_ShouldReturnCreatedOrder() throws Exception {
            OrderRequestDto requestDto = new OrderRequestDto();

            OrderResponseDto responseDto = new OrderResponseDto();
            responseDto.setId(500L);

            String fakeMessage = "Order created successfully";

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                when(orderService.createOrder(eq(CURRENT_USER_ID), ArgumentMatchers.any(OrderRequestDto.class)))
                        .thenReturn(responseDto);
                when(appTranslator.getTranslatedAction(eq("success.created"), eq(ENTITY_KEY)))
                        .thenReturn(fakeMessage);

                mockMvc.perform(post("/api/v1/orders/create-order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto)))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage))
                        .andExpect(jsonPath("$.data.id").value(500L));
            }
        }

        ///// Create Order - Validation Failed (Empty/Invalid DTO)
        @Test
        void createOrder_WithInvalidData_ShouldReturnBadRequest() throws Exception {
            OrderRequestDto invalidRequest = new OrderRequestDto();

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                mockMvc.perform(post("/api/v1/orders/create-order")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                        .andDo(print())
                        .andExpect(status().isBadRequest()) // يتوقع 400
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.errors").exists());
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// UPDATE METHODS ///////////////////////////////////

    @Nested
    @DisplayName("3. Update Order Tests (PUT)")
    class UpdateOrderTests {

        ///// Cancel Order - Success
        @Test
        void cancelOrder_ShouldReturnOk() throws Exception {
            Long orderId = 15L;
            String fakeMessage = "Order cancelled successfully";

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                doNothing().when(orderService).cancelOrder(CURRENT_USER_ID, orderId);
                when(appTranslator.getTranslatedAction(eq("success.updated"), eq(ENTITY_KEY)))
                        .thenReturn(fakeMessage);

                mockMvc.perform(put("/api/v1/orders/cancel-order/{id}", orderId)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage));
            }


        }

        ///// Cancel Order - Order Not Found
        @Test
        void cancelOrder_WhenOrderNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidOrderId = 999L;
            String errorKey = "error.resource.not.found";
            String fakeErrorMessage = "Order not found with id: " + invalidOrderId;

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                doThrow(new ResourceNotFoundException("Order", "id", invalidOrderId))
                        .when(orderService).cancelOrder(CURRENT_USER_ID, invalidOrderId);

                when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                        .thenReturn(fakeErrorMessage);

                mockMvc.perform(put("/api/v1/orders/cancel-order/{id}", invalidOrderId)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isNotFound()) // يتوقع 404
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value(fakeErrorMessage));
            }
        }

        ///// Soft Delete Order - Success
        @Test
        void softDeleteOrder_ShouldReturnOk() throws Exception {
            Long orderId = 20L;
            String fakeMessage = "Order deleted successfully";

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                doNothing().when(orderService).softDeleteOrder(CURRENT_USER_ID, orderId);
                when(appTranslator.getTranslatedAction(eq("success.deleted"), eq(ENTITY_KEY)))
                        .thenReturn(fakeMessage);

                mockMvc.perform(put("/api/v1/orders/soft-delete-order/{id}", orderId)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage));
            }
        }

        ///// Soft Delete Order - Order Not Found
        @Test
        void softDeleteOrder_WhenOrderNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidOrderId = 999L;
            String errorKey = "error.resource.not.found";
            String fakeErrorMessage = "Order not found with id: " + invalidOrderId;

            try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
                mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                doThrow(new ResourceNotFoundException("Order", "id", invalidOrderId))
                        .when(orderService).softDeleteOrder(CURRENT_USER_ID, invalidOrderId);

                when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                        .thenReturn(fakeErrorMessage);

                mockMvc.perform(put("/api/v1/orders/soft-delete-order/{id}", invalidOrderId)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isNotFound()) // يتوقع 404
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value(fakeErrorMessage));
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// DELETE METHODS ////////////////////////////////////

    @Nested
    @DisplayName("4. Delete Order Tests (DELETE)")
    class DeleteOrderTests {

        ///// Hard Delete Order (Admin) - Success
        @Test
        void hardDeleteOrder_ShouldReturnOk() throws Exception {
            Long orderId = 99L;
            String fakeMessage = "Order deleted successfully";

            // No SecurityUtils needed as Admin endpoint relies on JWT filter which we bypass in WebMvcTest
            doNothing().when(orderService).hardDeleteOrder(orderId);
            when(appTranslator.getTranslatedAction(eq("success.deleted"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(delete("/api/v1/orders/hard-delete-order/{id}", orderId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }
    }
    ///// Hard Delete Order (Admin) - Order Not Found
    @Test
    void hardDeleteOrder_WhenOrderNotFound_ShouldReturnNotFound() throws Exception {
        Long invalidOrderId = 999L;
        String errorKey = "error.resource.not.found";
        String fakeErrorMessage = "Order not found with id: " + invalidOrderId;
        doThrow(new ResourceNotFoundException("Order", "id", invalidOrderId))
                .when(orderService).hardDeleteOrder(invalidOrderId);

        when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                .thenReturn(fakeErrorMessage);

        mockMvc.perform(delete("/api/v1/orders/hard-delete-order/{id}", invalidOrderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(fakeErrorMessage));
    }
}