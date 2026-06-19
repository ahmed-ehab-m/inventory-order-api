package com.global.order_api.feature.payment;

import com.global.order_api.core.security.JwtFilter;
import com.global.order_api.core.utils.AppTranslator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = PaymentController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppTranslator appTranslator;

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

    @Test
    @DisplayName("Webhook with VALID HMAC should return 200 OK")
    void handlePaymobWebHook_WithValidHmac_ShouldReturnOk() throws Exception {
        // 1. Arrange
        String validHmac = "secure_hmac_123";
        Map<String, Object> fakePayload = new HashMap<>();
        fakePayload.put("id", 123456);
        fakePayload.put("success", true);

        /// 2=> mock service
        when(paymentService.verifyPaymobHmac(eq(validHmac), any(Map.class))).thenReturn(true);
        doNothing().when(paymentService).processWebHook(any(Map.class));

        // 2. Act & Assert
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .param("hmac", validHmac)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fakePayload)))
                .andDo(print())
                .andExpect(status().isOk()); /// right hmac => success

        verify(paymentService, times(1)).processWebHook(any(Map.class));
    }

    @Test
    @DisplayName("Webhook with INVALID HMAC (Hacker) should return 401 Unauthorized")
    void handlePaymobWebHook_WithInvalidHmac_ShouldReturnUnauthorized() throws Exception {
        // 1. Arrange
        String invalidHmac = "hacker_hmac_999";
        Map<String, Object> fakePayload = new HashMap<>();
        fakePayload.put("success", true);

        /// wrong hmac
        when(paymentService.verifyPaymobHmac(eq(invalidHmac), any(Map.class))).thenReturn(false);

        // 2. Act & Assert
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .param("hmac", invalidHmac)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fakePayload)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(paymentService, never()).processWebHook(any());
    }

    @Test
    @DisplayName("Webhook when Service throws Exception should return 200 OK (to stop Paymob retries)")
    void handlePaymobWebHook_WhenServiceThrowsException_ShouldReturnOk() throws Exception {
        // 1. Arrange
        String hmac = "valid_hmac";
        Map<String, Object> fakePayload = new HashMap<>();

        when(paymentService.verifyPaymobHmac(anyString(), any(Map.class))).thenReturn(true);
        doThrow(new RuntimeException("Database timeout")).when(paymentService).processWebHook(any(Map.class));

        // 2. Act & Assert
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .param("hmac", hmac)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fakePayload)))
                .andDo(print())
                // بنتوقع 200 OK عشان منعلقش سيرفرات Paymob
                .andExpect(status().isOk());
    }
}