package com.global.order_api.feature.payment;

import com.global.order_api.core.exception.BusinessLogicException;
import com.global.order_api.feature.order.entity.OrderEntity;
import com.global.order_api.feature.order.repo.OrderRepo;
import com.global.order_api.feature.order.enums.OrderStatus;
import com.global.order_api.feature.user.entity.UserEntity;
import com.global.order_api.feature.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private PaymentRepo paymentRepo;
    @Mock
    private OrderRepo orderRepo;

    @InjectMocks
    private PaymentService paymentService;

    private final String DUMMY_HMAC_SECRET = "TEST_SECRET_123";

    @BeforeEach
    void setUp() {
        /// mock values in app.prop
        /// because we only run test environment so all vars in paymentservice have @Value
        /// will be Null
        /// ReflectionTestUtils => Class using Reflection technique
        /// means that => can access private fields in PaymentService
        ReflectionTestUtils.setField(paymentService, "apiKey", "dummy_api_key");
        ReflectionTestUtils.setField(paymentService, "cardIntegrationId", "1111");
        ReflectionTestUtils.setField(paymentService, "walletIntegrationId", "2222");
        ReflectionTestUtils.setField(paymentService, "kioskIntegrationId", "3333");
        ReflectionTestUtils.setField(paymentService, "iframeId", "4444");
        ReflectionTestUtils.setField(paymentService, "hmacSecret", DUMMY_HMAC_SECRET);
    }

    // ==================================================================================
    //                     1. GENERATE PAYMENT LINK TESTS
    // ==================================================================================
    @Nested
    @DisplayName("1. Generate Payment Link")
    class GeneratePaymentLinkTests {

        @Test
        void generatePaymentLink_CardPayment_ShouldReturnIframeUrl() {
            /// 1. Arrange fake Data of order request dto
            Long userId = 1L;
            Long orderId = 10L;
            BigDecimal amount = new BigDecimal("150.00"); // 150 EGP -> 15000 Cents
            String paymentMethod = "CARD";

            UserEntity fakeUser = new UserEntity();
            fakeUser.setId(userId);
            fakeUser.setName("ahmed ehab");
            fakeUser.setEmail("ahmed@test.com");
            fakeUser.setPhone("01012345678");

            OrderEntity fakeOrder = new OrderEntity();
            fakeOrder.setId(orderId);

            /// 2. Mocks for Repositories
            when(userService.findById(userId)).thenReturn(fakeUser);
            when(orderRepo.findByIdOrThrow(orderId)).thenReturn(fakeOrder);
            when(paymentRepo.save(any(PaymentEntity.class))).thenReturn(new PaymentEntity());

            /// 3. Mocks for Paymob API Calls (3 Steps)
            // Step 1: Auth
            Map<String, String> authResponseMap = new HashMap<>();
            authResponseMap.put("token", "dummy_auth_token");
            ResponseEntity<Map> authResponseEntity = new ResponseEntity<>(authResponseMap, HttpStatus.OK);
            when(restTemplate.postForEntity(contains("/auth/tokens"), any(), eq(Map.class)))
                    .thenReturn(authResponseEntity);

            // Step 2: Order Registration
            Map<String, Object> orderResponseMap = new HashMap<>();
            orderResponseMap.put("id", 999888); // Paymob order ID
            ResponseEntity<Map> orderResponseEntity = new ResponseEntity<>(orderResponseMap, HttpStatus.OK);
            when(restTemplate.postForEntity(contains("/ecommerce/orders"), any(), eq(Map.class)))
                    .thenReturn(orderResponseEntity);

            // Step 3: Payment Key Generation
            Map<String, String> keyResponseMap = new HashMap<>();
            keyResponseMap.put("token", "dummy_payment_key");
            ResponseEntity<Map> keyResponseEntity = new ResponseEntity<>(keyResponseMap, HttpStatus.OK);
            when(restTemplate.postForEntity(contains("/acceptance/payment_keys"), any(), eq(Map.class)))
                    .thenReturn(keyResponseEntity);

            // 4. Act
            PaymentResponseDto result = paymentService.generatePaymentLink(paymentMethod, userId, amount, orderId, null);

            // 5. Assert
            assertNotNull(result);
            assertEquals(PaymentActionType.IFRAME, result.getActionType());
            assertTrue(result.getTargetUrl().contains("dummy_payment_key"));
            assertTrue(result.getTargetUrl().contains("4444")); // iframeId

            // Verify Save was called
            verify(paymentRepo, times(1)).save(any(PaymentEntity.class));
        }

        @Test
        void generatePaymentLink_WhenPaymobFails_ShouldThrowBusinessLogicException() {
            Long userId = 1L;
            Long orderId = 10L;

            when(userService.findById(userId)).thenReturn(new UserEntity());

            /// check of paymob servers has problem
            when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                    .thenThrow(new RuntimeException("Connection Refused"));

            assertThrows(BusinessLogicException.class, () ->
                    paymentService.generatePaymentLink("CARD", userId, new BigDecimal("100"), orderId, null)
            );
        }
    }

    // ==================================================================================
    //                     2. HMAC SECURE WEBHOOK TESTS
    // ==================================================================================
    @Nested
    @DisplayName("2. HMAC & Webhook Processing")
    class WebhookTests {

        @Test
        void verifyPaymobHmac_WithValidHmac_ShouldReturnTrue() throws Exception {
            // 1. Arrange payload exactly as Paymob sends it
            Map<String, Object> obj = new HashMap<>();
            obj.put("amount_cents", "15000");
            obj.put("created_at", "2026-06-19T10:00:00");
            obj.put("currency", "EGP");
            obj.put("error_occured", "false");
            obj.put("has_parent_transaction", "false");
            obj.put("id", "123456");
            obj.put("integration_id", "1111");
            obj.put("is_3d_secure", "true");
            obj.put("is_auth", "false");
            obj.put("is_capture", "false");
            obj.put("is_refunded", "false");
            obj.put("is_standalone_payment", "true");
            obj.put("is_voided", "false");
            obj.put("success", "true");
            obj.put("owner", "999");
            obj.put("pending", "false");

            Map<String, Object> order = new HashMap<>();
            order.put("id", "999888");
            obj.put("order", order);

            Map<String, Object> sourceData = new HashMap<>();
            sourceData.put("pan", "2346");
            sourceData.put("sub_type", "MasterCard");
            sourceData.put("type", "card");
            obj.put("source_data", sourceData);

            Map<String, Object> paymobResponse = new HashMap<>();
            paymobResponse.put("obj", obj);

            // 2. Calculate Expected HMAC manually based on exactly what the service concatenates
            String concatenatedString = "150002026-06-19T10:00:00EGPfalsefalse1234561111truefalsefalsefalsetruefalse999888999false2346MasterCardcardtrue";

            Mac sha512Hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(DUMMY_HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512Hmac.init(keySpec);
            byte[] macData = sha512Hmac.doFinal(concatenatedString.getBytes(StandardCharsets.UTF_8));

            StringBuilder expectedHmac = new StringBuilder();
            for (byte b : macData) {
                expectedHmac.append(String.format("%02x", b));
            }

            // 3. Act
            boolean isValid = paymentService.verifyPaymobHmac(expectedHmac.toString(), paymobResponse);

            // 4. Assert
            assertTrue(isValid, "HMAC should be valid for correct payload and secret");
        }

        @Test
        void processWebHook_SuccessPayment_ShouldUpdateStatus() {
            // 1. Mock Webhook Payload
            Map<String, Object> obj = new HashMap<>();
            obj.put("success", true);
            obj.put("id", 123456); // transaction id
            obj.put("amount_cents", "15000"); // 150 EGP

            Map<String, Object> orderData = new HashMap<>();
            orderData.put("id", 999888); // Paymob Order ID
            obj.put("order", orderData);

            Map<String, Object> sourceData = new HashMap<>();
            sourceData.put("sub_type", "MasterCard");
            obj.put("source_data", sourceData);

            Map<String, Object> paymobResponse = new HashMap<>();
            paymobResponse.put("obj", obj);

            // 2. Mock DB Entity
            OrderEntity orderEntity = new OrderEntity();
            orderEntity.setStatus(OrderStatus.PENDING);

            PaymentEntity paymentEntity = new PaymentEntity();
            paymentEntity.setPaymobOrderId("999888");
            paymentEntity.setAmount(new BigDecimal("150.00"));
            paymentEntity.setPaymentStatus(PaymentStatus.PENDING);
            paymentEntity.setOrder(orderEntity);

            when(paymentRepo.findByPaymobOrderId("999888")).thenReturn(Optional.of(paymentEntity));

            // 3. Act
            paymentService.processWebHook(paymobResponse);

            // 4. Assert
            assertEquals(PaymentStatus.SUCCESS, paymentEntity.getPaymentStatus());
            assertEquals("123456", paymentEntity.getTransactionId());
            assertEquals(OrderStatus.PROCESSING, orderEntity.getStatus());
        }

        @Test
        void processWebHook_AmountMismatch_ShouldMarkAsFailed() {
            /// check if hacker change amount of order during front-end request
            Map<String, Object> obj = new HashMap<>();
            obj.put("success", true);
            obj.put("id", 123456);
            obj.put("amount_cents", "5000");

            Map<String, Object> orderData = new HashMap<>();
            orderData.put("id", 999888);
            obj.put("order", orderData);
            obj.put("source_data", new HashMap<>());

            Map<String, Object> paymobResponse = new HashMap<>();
            paymobResponse.put("obj", obj);

            PaymentEntity paymentEntity = new PaymentEntity();
            paymentEntity.setPaymobOrderId("999888");
            paymentEntity.setAmount(new BigDecimal("150.00"));
            paymentEntity.setPaymentStatus(PaymentStatus.PENDING);

            when(paymentRepo.findByPaymobOrderId("999888")).thenReturn(Optional.of(paymentEntity));

            // Act
            paymentService.processWebHook(paymobResponse);

            // Assert
            assertEquals(PaymentStatus.FAILED, paymentEntity.getPaymentStatus());
            assertTrue(paymentEntity.getProviderMessage().contains("Fraud"));
        }
    }
}