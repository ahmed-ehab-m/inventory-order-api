package com.global.order_api.feature.order;

import com.global.order_api.core.base.PageResponse;
import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.core.security.JwtFilter;
import com.global.order_api.core.utils.AppTranslator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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

@WebMvcTest(value = AdminOrderController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class),
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
class AdminOrderControllerTest {

    private static final String ENTITY_KEY = "entity.order";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminOrderService adminOrderService;

    @MockitoBean
    private AppTranslator appTranslator;

//    @Autowired
//    private ObjectMapper objectMapper;

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
        @DisplayName("Get All Orders (Admin) - Should return paginated orders")
        void getAllOrders_ShouldReturnPagedOrders() throws Exception {
            OrderResponseDto responseDto = new OrderResponseDto();
            responseDto.setId(100L);

            List<OrderResponseDto> dtoList = List.of(responseDto);
            PageResponse<OrderResponseDto> fakePageResponse = PageResponse.from(
                    new PageImpl<>(dtoList), dtoList);

            String fakeMessage = "Orders retrieved successfully";

            when(adminOrderService.getAllOrders(ArgumentMatchers.any(OrderFilterRequest.class)))
                    .thenReturn(fakePageResponse);
            when(appTranslator.getTranslatedAction(eq("success.retrieved"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(get("/api/v1/admin/orders")
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.data[0].id").value(100L));
        }

        @Test
        @DisplayName("Get All Orders (Admin) - Invalid filter should return 400")
        void getAllOrders_WithInvalidFilter_ShouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/admin/orders")
                            .param("page", "-1")
                            .param("size", "-5")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors").exists());
        }

        @Test
        @DisplayName("Get Order By ID (Admin) - Should return order")
        void getOrderById_ShouldReturnOrder() throws Exception {
            Long orderId = 50L;
            OrderResponseDto responseDto = new OrderResponseDto();
            responseDto.setId(orderId);

            String fakeMessage = "Order retrieved successfully";

            when(adminOrderService.getOrderById(orderId))
                    .thenReturn(responseDto);
            when(appTranslator.getTranslatedAction(eq("success.retrieved"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(get("/api/v1/admin/orders/{id}", orderId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.id").value(orderId));
        }

        @Test
        @DisplayName("Get Order By ID (Admin) - Not found should return 404")
        void getOrderById_WhenNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidOrderId = 999L;
            String errorKey = "error.resource.not.found";
            String fakeErrorMessage = "Order not found with id: " + invalidOrderId;

            doThrow(new ResourceNotFoundException("Order", "id", invalidOrderId))
                    .when(adminOrderService).getOrderById(invalidOrderId);

            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeErrorMessage);

            mockMvc.perform(get("/api/v1/admin/orders/{id}", invalidOrderId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeErrorMessage));
        }
    }

    // ==================================================================================
    //                                2. UPDATE METHODS
    // ==================================================================================

    @Nested
    @DisplayName("2. Update Order Tests (PUT)")
    class UpdateOrderTests {

        @Test
        @DisplayName("Update Order Status - Should return updated order")
        void updateOrderStatus_ShouldReturnUpdatedOrder() throws Exception {
            Long orderId = 75L;
            OrderStatus newStatus = OrderStatus.SHIPPED;

            OrderResponseDto responseDto = new OrderResponseDto();
            responseDto.setId(orderId);

            String fakeMessage = "Order status updated successfully";

            when(adminOrderService.updateOrderStatusByAdmin(orderId, newStatus))
                    .thenReturn(responseDto);
            when(appTranslator.getTranslatedAction(eq("success.updated"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(put("/api/v1/admin/orders/{id}/status", orderId)
                            .param("status", newStatus.name())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage))
                    .andExpect(jsonPath("$.data.id").value(orderId));
        }

        @Test
        @DisplayName("Update Order Status - Not found should return 404")
        void updateOrderStatus_WhenNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidOrderId = 999L;
            OrderStatus newStatus = OrderStatus.DELIVERED;
            String errorKey = "error.resource.not.found";
            String fakeErrorMessage = "Order not found with id: " + invalidOrderId;

            doThrow(new ResourceNotFoundException("Order", "id", invalidOrderId))
                    .when(adminOrderService).updateOrderStatusByAdmin(invalidOrderId, newStatus);

            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeErrorMessage);

            mockMvc.perform(put("/api/v1/admin/orders/{id}/status", invalidOrderId)
                            .param("status", newStatus.name())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeErrorMessage));
        }
    }

    // ==================================================================================
    //                                3. DELETE METHODS
    // ==================================================================================

    @Nested
    @DisplayName("3. Delete Order Tests (DELETE)")
    class DeleteOrderTests {

        @Test
        @DisplayName("Hard Delete Order (Admin) - Should return 200")
        void hardDeleteOrder_ShouldReturnOk() throws Exception {
            Long orderId = 99L;
            String fakeMessage = "Order deleted successfully";

            doNothing().when(adminOrderService).hardDeleteOrder(orderId);
            when(appTranslator.getTranslatedAction(eq("success.deleted"), eq(ENTITY_KEY)))
                    .thenReturn(fakeMessage);

            mockMvc.perform(delete("/api/v1/admin/orders/{id}/force", orderId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(fakeMessage));
        }

        @Test
        @DisplayName("Hard Delete Order (Admin) - Not found should return 404")
        void hardDeleteOrder_WhenNotFound_ShouldReturnNotFound() throws Exception {
            Long invalidOrderId = 999L;
            String errorKey = "error.resource.not.found";
            String fakeErrorMessage = "Order not found with id: " + invalidOrderId;

            doThrow(new ResourceNotFoundException("Order", "id", invalidOrderId))
                    .when(adminOrderService).hardDeleteOrder(invalidOrderId);

            when(appTranslator.translateMessage(eq(errorKey), ArgumentMatchers.any(Object[].class)))
                    .thenReturn(fakeErrorMessage);

            mockMvc.perform(delete("/api/v1/admin/orders/{id}/force", invalidOrderId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(fakeErrorMessage));
        }
    }
}