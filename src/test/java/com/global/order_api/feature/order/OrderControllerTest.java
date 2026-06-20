package com.global.order_api.feature.order;

import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.core.security.JwtFilter;
import com.global.order_api.core.security.SecurityUtils;
import com.global.order_api.core.utils.AppTranslator;
import com.global.order_api.feature.order.controller.OrderController;
import com.global.order_api.feature.order.specification.OrderFilterRequest;
import com.global.order_api.feature.order.dto.OrderRequestDto;
import com.global.order_api.feature.order.dto.OrderResponseDto;
import com.global.order_api.feature.order.enums.PaymentType;
import com.global.order_api.feature.order.service.OrderService;
import com.global.order_api.feature.payment.PaymentResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = OrderController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class),
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
class OrderControllerTest {

    private static final String ENTITY_KEY = "entity.order";
    private static final Long CURRENT_USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private AppTranslator appTranslator;

    private final ObjectMapper objectMapper = new ObjectMapper();


    /// Mock redis
    /// because our test request will enter rate limiter interceptor
    /// then interceptor call redis
    /// and we in test environment so result is error
    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        /// mock this first request
        when(valueOperations.increment(anyString())).thenReturn(1L);
    }

    // ==================================================================================
    //                                1. GET METHODS
    // ==================================================================================

    @Nested
    @DisplayName("1. Get Orders Tests (GET)")
    class GetOrdersTests {

        @Test
        @DisplayName("Get User Orders - Should return paginated orders")
        void getUserOrder_ShouldReturnUserOrders() throws Exception {
            OrderResponseDto responseDto = new OrderResponseDto();
            responseDto.setId(200L);

            List<OrderResponseDto> dtoList = List.of(responseDto);
            PageResponse<OrderResponseDto> fakePageResponse = PageResponse.from(
                    new PageImpl<>(dtoList), dtoList);

            String fakeMessage = "Orders retrieved successfully";

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                when(orderService.getUserOrders(eq(CURRENT_USER_ID), ArgumentMatchers.any(OrderFilterRequest.class)))
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
                        .andExpect(jsonPath("$.data.data[0].id").value(200L));
            }
        }

        @Test
        @DisplayName("Get User Orders - Invalid filter should return 400")
        void getUserOrder_WithInvalidFilter_ShouldReturnBadRequest() throws Exception {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                mockMvc.perform(get("/api/v1/orders")
                                .param("page", "-1")
                                .param("size", "-5")
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.errors").exists());
            }
        }

        @Test
        @DisplayName("Get User Order By ID - Should return order")
        void getUserOrderById_ShouldReturnOrder() throws Exception {
            Long orderId = 10L;
            OrderResponseDto responseDto = new OrderResponseDto();
            responseDto.setId(orderId);

            String fakeMessage = "Order retrieved successfully";

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                when(orderService.getUserOrderById(CURRENT_USER_ID, orderId))
                        .thenReturn(responseDto);
                when(appTranslator.getTranslatedAction(eq("success.retrieved"), eq(ENTITY_KEY)))
                        .thenReturn(fakeMessage);

                mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage))
                        .andExpect(jsonPath("$.data.id").value(orderId));
            }
        }

        @Test
        @DisplayName("Get User Order By ID - Not found should return 404")
        void getUserOrderById_WhenNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidOrderId = 999L;
            String errorKey = "error.resource.not.found";
            String fakeErrorMessage = "Order not found with id: " + invalidOrderId;

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                doThrow(new ResourceNotFoundException("Order", "id", invalidOrderId))
                        .when(orderService).getUserOrderById(CURRENT_USER_ID, invalidOrderId);

                when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                        .thenReturn(fakeErrorMessage);

                mockMvc.perform(get("/api/v1/orders/{id}", invalidOrderId)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value(fakeErrorMessage));
            }
        }
    }

    // ==================================================================================
    //                                2. CREATE METHODS
    // ==================================================================================

    @Nested
    @DisplayName("2. Create Order Tests (POST)")
    class CreateOrderTests {

        @Test
        @DisplayName("Create Order - Valid data should return 201")
        void createOrder_WithValidData_ShouldReturnCreatedOrder() throws Exception {
            OrderRequestDto requestDto = new OrderRequestDto();
            requestDto.setShippingAddress("shubra");
            requestDto.setPaymentType(PaymentType.CASH);
            OrderResponseDto responseDto = new OrderResponseDto();
            responseDto.setId(500L);

            String fakeMessage = "Order created successfully";

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                when(orderService.createOrder(eq(CURRENT_USER_ID), ArgumentMatchers.any(OrderRequestDto.class)))
                        .thenReturn(responseDto);
                when(appTranslator.getTranslatedAction(eq("success.created"), eq(ENTITY_KEY)))
                        .thenReturn(fakeMessage);

                mockMvc.perform(post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto)))
                        .andDo(print())
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.message").value(fakeMessage))
                        .andExpect(jsonPath("$.data.id").value(500L));
            }
        }

        @Test
        @DisplayName("Create Order - Invalid data should return 400")
        void createOrder_WithInvalidData_ShouldReturnBadRequest() throws Exception {
            OrderRequestDto invalidRequest = new OrderRequestDto();

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                mockMvc.perform(post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                        .andDo(print())
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.errors").exists());
            }
        }
    }

    // ==================================================================================
    //                                3. UPDATE METHODS
    // ==================================================================================

    @Nested
    @DisplayName("3. Update Order Tests (PUT)")
    class UpdateOrderTests {

        @Test
        @DisplayName("Cancel Order - Should return 200")
        void cancelOrder_ShouldReturnOk() throws Exception {
            Long orderId = 15L;
            String fakeMessage = "Order cancelled successfully";

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                doNothing().when(orderService).cancelOrder(CURRENT_USER_ID, orderId);
                when(appTranslator.getTranslatedAction(eq("success.updated"), eq(ENTITY_KEY)))
                        .thenReturn(fakeMessage);

                mockMvc.perform(put("/api/v1/orders/{id}/cancel", orderId)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage));
            }
        }

        @Test
        @DisplayName("Cancel Order - Not found should return 404")
        void cancelOrder_WhenNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidOrderId = 999L;
            String errorKey = "error.resource.not.found";
            String fakeErrorMessage = "Order not found with id: " + invalidOrderId;

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                doThrow(new ResourceNotFoundException("Order", "id", invalidOrderId))
                        .when(orderService).cancelOrder(CURRENT_USER_ID, invalidOrderId);

                when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                        .thenReturn(fakeErrorMessage);

                mockMvc.perform(put("/api/v1/orders/{id}/cancel", invalidOrderId)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value(fakeErrorMessage));
            }
        }

        @Test
        @DisplayName("Return Order - Should return 200")
        void returnOrder_ShouldReturnOk() throws Exception {
            Long orderId = 25L;
            String fakeMessage = "Order returned successfully";

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                doNothing().when(orderService).returnDeliveredOrder(CURRENT_USER_ID, orderId);
                when(appTranslator.getTranslatedAction(eq("success.updated"), eq(ENTITY_KEY)))
                        .thenReturn(fakeMessage);

                mockMvc.perform(put("/api/v1/orders/{id}/return", orderId)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage));
            }
        }

        @Test
        @DisplayName("Return Order - Not found should return 404")
        void returnOrder_WhenNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidOrderId = 999L;
            String errorKey = "error.resource.not.found";
            String fakeErrorMessage = "Order not found with id: " + invalidOrderId;

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                doThrow(new ResourceNotFoundException("Order", "id", invalidOrderId))
                        .when(orderService).returnDeliveredOrder(CURRENT_USER_ID, invalidOrderId);

                when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                        .thenReturn(fakeErrorMessage);

                mockMvc.perform(put("/api/v1/orders/{id}/return", invalidOrderId)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value(fakeErrorMessage));
            }
        }

        @Test
        @DisplayName("Soft Delete (Archive) Order - Should return 200")
        void softDeleteOrder_ShouldReturnOk() throws Exception {
            Long orderId = 20L;
            String fakeMessage = "Order archived successfully";

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                doNothing().when(orderService).softDeleteOrder(CURRENT_USER_ID, orderId);
                when(appTranslator.getTranslatedAction(eq("success.deleted"), eq(ENTITY_KEY)))
                        .thenReturn(fakeMessage);

                mockMvc.perform(delete("/api/v1/orders/{id}", orderId)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage));
            }
        }

        @Test
        @DisplayName("Soft Delete (Archive) Order - Not found should return 404")
        void softDeleteOrder_WhenNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidOrderId = 999L;
            String errorKey = "error.resource.not.found";
            String fakeErrorMessage = "Order not found with id: " + invalidOrderId;

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                doThrow(new ResourceNotFoundException("Order", "id", invalidOrderId))
                        .when(orderService).softDeleteOrder(CURRENT_USER_ID, invalidOrderId);

                when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                        .thenReturn(fakeErrorMessage);

                mockMvc.perform(delete("/api/v1/orders/{id}", invalidOrderId)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value(fakeErrorMessage));
            }
        }
    }

    // ==================================================================================
    //                                4. PAYMENT METHODS
    // ==================================================================================

    @Nested
    @DisplayName("4. Payment Tests (GET)")
    class PaymentTests {

        @Test
        @DisplayName("Retry Payment - Should return payment URL")
        void retryPayment_ShouldReturnPaymentUrl() throws Exception {
            Long orderId = 30L;
            String walletNumber = "01012345678";
            PaymentResponseDto paymentResponse = new PaymentResponseDto();
            paymentResponse.setTargetUrl("https://paymob.com/pay/abc123");

            String fakeMessage = "Payment link generated";

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                when(orderService.retryPayment(CURRENT_USER_ID, orderId, walletNumber))
                        .thenReturn(paymentResponse);
                when(appTranslator.getTranslatedAction(eq("success.payment_link_generated"), eq(ENTITY_KEY)))
                        .thenReturn(fakeMessage);

                mockMvc.perform(get("/api/v1/orders/{id}/retry-payment", orderId)
                                .param("walletNumber", walletNumber)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage))
                        .andExpect(jsonPath("$.data.targetUrl").value("https://paymob.com/pay/abc123"));
            }
        }

        @Test
        @DisplayName("Retry Payment - Without wallet number should work")
        void retryPayment_WithoutWalletNumber_ShouldReturnPaymentUrl() throws Exception {
            Long orderId = 30L;
            PaymentResponseDto paymentResponse = new PaymentResponseDto();
            paymentResponse.setTargetUrl("https://paymob.com/pay/xyz789");

            String fakeMessage = "Payment link generated";

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getCurrentUserId).thenReturn(CURRENT_USER_ID);

                when(orderService.retryPayment(CURRENT_USER_ID, orderId, null))
                        .thenReturn(paymentResponse);
                when(appTranslator.getTranslatedAction(eq("success.payment_link_generated"), eq(ENTITY_KEY)))
                        .thenReturn(fakeMessage);

                mockMvc.perform(get("/api/v1/orders/{id}/retry-payment", orderId)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value(fakeMessage))
                        .andExpect(jsonPath("$.data.targetUrl").value("https://paymob.com/pay/xyz789"));
            }
        }
    }
}