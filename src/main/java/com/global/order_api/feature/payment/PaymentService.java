package com.global.order_api.feature.payment;

import com.global.order_api.feature.order.OrderEntity;
import com.global.order_api.feature.order.OrderRepo;
import com.global.order_api.feature.order.OrderStatus;
import com.global.order_api.feature.user.UserEntity;
import com.global.order_api.feature.user.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
public class PaymentService {

    private final UserService userService;
    private final RestTemplate restTemplate;
    private final PaymentRepo paymentRepo;
    private final OrderRepo orderRepo;
    private final String BASE_URL = "https://accept.paymob.com/api";
    @Value("${paymob.api.key}")
    private String apiKey;

    @Value("${paymob.integration.id.card}")
    private String cardIntegrationId;

    @Value("${paymob.integration.id.wallet}")
    private String walletIntegrationId;

    @Value("${paymob.integration.id.fawry}")
    private String kioskIntegrationId;

    @Value("${paymob.iframe.id}")
    private String iframeId;

    @Value("${paymob.hmac.secret}")
    private String hmacSecret;

    public PaymentService(PaymentRepo paymentRepo, UserService userService, RestTemplate restTemplate, OrderRepo orderRepo) {
        this.userService = userService;
        this.restTemplate = restTemplate;
        this.paymentRepo = paymentRepo;
        this.orderRepo = orderRepo;
    }

    /// rest template => like postman
    /// class for making my server to send request (server be client)

    /// / PRIMARY FUNCTION ///////////////
    /// / GENERATING PAYMENT LINK FOR FRONT-END ////////
    public PaymentResponseDto generatePaymentLink(
            String paymentMethod,
            Long userId,
            BigDecimal amount,
            Long orderId,
            String walletNumber) {
        /// 1=> convert amount to cents because paymob only understand cents
        /// we in java operate with money using big decimal
        /// so if we send bigDecimal to json = jackson may write this like 1.5E4
        /// then paymob refuses it so we use string
        String amountInCents = amount.multiply(new BigDecimal("100"))
                /// strip => remove unneeded 0 from price
                /// plainString => to force result be same not like 1.E
                .stripTrailingZeros().toPlainString();
        UserEntity user = userService.findById(userId);
        try {
            /// 2=> Authentication
            String authToken = authenticate();
            log.info("Step 1: Auth Token generated successfully");

            /// 3=> order registration
            String paymobOrderId = registerOrder(authToken, amountInCents);
            log.info("Step 2: Paymob Order registered with ID: {}", paymobOrderId);

            /////////////////////Create Payment Table /////////////////
            OrderEntity order = orderRepo.findByIdOrThrow(orderId);

            PaymentEntity payment = new PaymentEntity();
            payment.setOrder(order);
            payment.setAmount(amount);
            payment.setCurrency("EGP");
            payment.setPaymentStatus(PaymentStatus.PENDING);
            payment.setPaymobOrderId(paymobOrderId);
            /// initial value , webhook will update it
            payment.setPaymentMethod(paymentMethod);

            paymentRepo.save(payment);
            log.info("Payment record created in DB with Pending status");


            String activeIntegrationId = getIntegrationId(paymentMethod);
            /// 4=> payment key generation
            String paymentKeyToken = generatePaymentKey(user, authToken, paymobOrderId, amountInCents, activeIntegrationId);
            log.info("Step 3: Payment Key Token generated successfully");
            /// routing dependon payment type
            if ("WALLET".equalsIgnoreCase((paymentMethod))) {
                /// link to user wallet
                String redirectUrl = payWithWallet(paymentKeyToken, walletNumber, user.getPhone());
                return PaymentResponseDto.builder()
                        .actionType(PaymentActionType.REDIRECT)
                        .targetUrl(redirectUrl)
                        .build();
            } else if ("KIOSK".equalsIgnoreCase(paymentMethod)) {
                String billReference = payWithKiosk(paymentKeyToken);
                return PaymentResponseDto.builder()
                        .actionType(PaymentActionType.REFERENCE)
                        .referenceNumber(billReference)
                        .build(); /// RETURN REFERENCE KEY OF FAWRY
            } else {
                // CARDS
                String iframeUrl = BASE_URL + "/acceptance/iframes/" + iframeId + "?payment_token=" + paymentKeyToken;
                return PaymentResponseDto.builder()
                        .actionType(PaymentActionType.IFRAME)
                        .targetUrl(iframeUrl)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error during Paymob Integration: ", e);
            throw new RuntimeException("فشل في إنشاء رابط الدفع: " + e.getMessage());
        }
    }

    /// 1 => return temp token
    /// Authentication
    private String authenticate() {
        /// send api key from paymob to paymob for authentication
        String url = BASE_URL + "/auth/tokens";
        Map<String, String> request = new HashMap<>();
        /// 1=> prepare our request to send api token
        request.put("api_key", apiKey);

        /// postForEntity => open connection and make POST request and return full response
        /// pass url , request , Map.class => convert json response into Map
        /// Response Entity => status code + headers + body
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        /// 2=> return temp token
        return (String) response.getBody().get("token");
    }

    /// 2=> return paymob-order-id
    /// Order registration
    private String registerOrder(String authToken, String amountInCents) {
        String url = BASE_URL + "/ecommerce/orders";
        Map<String, Object> request = new HashMap<>();
        request.put("auth_token", authToken);
        /// no need to shipping now
        request.put("delivery_needed", "false");
        request.put("amount_cents", amountInCents);
        request.put("currency", "EGP");
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        /// convert paymob-order-id into String
        /// because we need to send this id again to paymob and string format is easy in sending in json
        return String.valueOf(response.getBody().get("id"));
    }

    /// 3=> return paymob-key-generation
    private String generatePaymentKey(UserEntity user, String authToken, String paymobOrderId, String amountInCents, String integrationId) {
        String url = BASE_URL + "/acceptance/payment_keys";
        String firstName = "NA";
        String lastName = "NA";

        if (user.getName() != null && !user.getName().trim().isEmpty()) {
            String[] nameParts = user.getName().split(" ", 2);
            firstName = nameParts[0];
            if (nameParts.length > 1) {
                lastName = nameParts[1];
            }
        }
        Map<String, String> billingData = new HashMap<>();
        billingData.put("email", user.getEmail());
        billingData.put("first_name", firstName);
        billingData.put("last_name", lastName);
        billingData.put("phone_number", user.getPhone() != null ? user.getPhone() : "+201000000000");
        billingData.put("apartment", "NA");
        billingData.put("street", "NA");
        billingData.put("building", "NA");
        billingData.put("floor", "NA");
        billingData.put("shipping_method", "NA");
        billingData.put("postal_code", "NA");
        billingData.put("city", "Cairo");
        billingData.put("country", "EG");
        billingData.put("state", "NA");

        Map<String, Object> request = new HashMap<>();
        request.put("auth_token", authToken);
        request.put("amount_cents", amountInCents);
        request.put("expiration", 3600); /// 1 hour
        request.put("order_id", paymobOrderId);
        request.put("billing_data", billingData);
        request.put("currency", "EGP");
        request.put("integration_id", Integer.parseInt(integrationId));

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        return (String) response.getBody().get("token");
    }

    /// / for WALLETS
    private String payWithWallet(String paymentToken, String walletNumber, String userPhone) {
        String url = BASE_URL + "/acceptance/payments/pay";
        Map<String, String> source = new HashMap<>();
        /// add user wallet number
        String finalPhone = (walletNumber != null && !walletNumber.isEmpty()) ?
                walletNumber : (userPhone != null ? userPhone : "010000000000");
        //// identifier => wallet number
        source.put("identifier", finalPhone);
        source.put("subtype", "WALLET");

        Map<String, Object> request = new HashMap<>();
        request.put("source", source);
        request.put("payment_token", paymentToken);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        /// redirect url for user to complete payment with his phone
        return (String) response.getBody().get("redirect_url");
    }

    /// // for FAWRY
    private String payWithKiosk(String paymentToken) {
        String url = BASE_URL + "/acceptance/payments/pay";
        Map<String, String> source = new HashMap<>();
        /// fawry send static word => AGGREGATOR
        source.put("identifier", "AGGREGATOR");
        source.put("subtype", "AGGREGATOR");

        Map<String, Object> request = new HashMap<>();
        request.put("source", source);
        request.put("payment_token", paymentToken);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);


        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return String.valueOf(data.get("bill_reference"));
    }

    private String getIntegrationId(String paymentMethod) {
        if ("WALLET".equalsIgnoreCase(paymentMethod)) return walletIntegrationId;
        if ("KIOSK".equalsIgnoreCase(paymentMethod)) return kioskIntegrationId;
        return cardIntegrationId;
    }

    /// /////////////////////////////////////////////////////////
    /// CHECK WEBHOOK
    public boolean verifyPaymobHmac(String receiveHmac, Map<String, Object> paymobResponse) {
        try {
            /// get specific fields from obj Object from response
            /// and sort them + concatenation into one string +
            /// encryption (HMAC SHA512) USing my secret key
            Map<String, Object> obj = (Map<String, Object>) paymobResponse.get("obj");
            /// 1=> concatenation
            String concatenatedString =
                    getValue(obj, "amount_cents") +
                            getValue(obj, "created_at") +
                            getValue(obj, "currency") +
                            getValue(obj, "error_occured") +
                            getValue(obj, "has_parent_transaction") +
                            getValue(obj, "id") +
                            getValue(obj, "integration_id") +
                            getValue(obj, "is_3d_secure") +
                            getValue(obj, "is_auth") +
                            getValue(obj, "is_capture") +
                            getValue(obj, "is_refunded") +
                            getValue(obj, "is_standalone_payment") +
                            getValue(obj, "is_voided") +
                            getOrderValue(obj, "id") +
                            getValue(obj, "owner") +
                            getValue(obj, "pending") +
                            getSourceDataValue(obj, "pan") +
                            getSourceDataValue(obj, "sub_type") +
                            getSourceDataValue(obj, "type") +
                            getValue(obj, "success");

            /// 2=> encryption using HMAC SHA 512
            /// Mac => Message Authentication Code
            /// create class for encryption using HmacSHA512 Algorithm
            Mac sha512Hmac = Mac.getInstance("HmacSHA512");
            /// to run our mac must pass key (Hmac Secret from paymob Dashboard)
            /// and convert it to object to make our mac operate with it
            SecretKeySpec keySpec = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512");
            /// here initialize our mac with key
            sha512Hmac.init(keySpec);

            /// dofinal => method in mac  take our concatenation string to hash it
            /// return is array of binary data
            byte[] macData = sha512Hmac.doFinal(concatenatedString.getBytes(StandardCharsets.UTF_8));

            /// 3=> convert result to Hexadecimal String
            /// paymob return hash result in string format in url
            /// any my is bytes
            /// so here convert this binary[] into string to compare it
            StringBuilder calculatedHmac = new StringBuilder();
            for (byte b : macData) {
                /// convert bin to hexadecimal
                calculatedHmac.append(String.format("%02x", b));
            }
            /// if hmac is real => hmac calculated equal hmac from pay mob
            return calculatedHmac.toString().equals(receiveHmac);
        } catch (Exception e) {
            log.error("Error calculating HMAC: ", e);
            return false;
        }

    }

    /// update our order data (Order Status , Payment Status) in DB
    @Transactional
    public void processWebHook(Map<String, Object> paymobResponse) {
        Map<String, Object> obj = (Map<String, Object>) paymobResponse.get("obj");

        /// 1=> extract basic data from obj
        boolean isSuccess = (boolean) obj.get("success");
        String transactionId = String.valueOf(obj.get("id"));

        /// order data
        Map<String, Object> orderData = (Map<String, Object>) obj.get("order");
        String paymobOrderId = String.valueOf(orderData.get("id"));

        /// payment type
        Map<String, Object> sourceData = (Map<String, Object>) obj.get("source_data");
        String actualPaymentMethod = String.valueOf(sourceData.get("sub_type"));

        /// Provider Details (Message & Code)
        Map<String, Object> dataObj = (Map<String, Object>) obj.get("data");
        String providerMessage = dataObj != null ? String.valueOf(dataObj.get("message")) : null;
        String providerResponseCode = dataObj != null ? String.valueOf(dataObj.get("txn_response_code")) : null;

        //// UPDATE OUR DATA IN DB using payment order id
        PaymentEntity paymentEntity = paymentRepo.findByPaymobOrderId(paymobOrderId)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymobOrderId));

        /// ITOMPOTENCY
        if (paymentEntity.getPaymentStatus() == PaymentStatus.SUCCESS) {
            log.info("Webhook already processed for this order before. Skipping.");
            return;
        }

        /// Update common fields
        paymentEntity.setTransactionId(transactionId);
        paymentEntity.setPaymentMethod(actualPaymentMethod);
        paymentEntity.setProviderMessage(providerMessage);
        paymentEntity.setProviderResponseCode(providerResponseCode);

//         paymentEntity.setUpdatedAt(LocalDateTime.now());
        /// VALIDATE PRCIE FROM WEBHOOK
        String amountCentsStr = getValue(obj, "amount_cents");
        /// remove 00 from price to get original price to compare it with value in DB
        BigDecimal paymobAmount = new BigDecimal(amountCentsStr).divide(new BigDecimal("100"));
        if (paymobAmount.compareTo(paymentEntity.getAmount()) != 0) {
            log.error("Amount mismatch for Paymob Order ID: {}. Expected: {}, Received: {}",
                    paymobOrderId, paymentEntity.getAmount(), paymobAmount);
            paymentEntity.setPaymentStatus(PaymentStatus.FAILED);
            paymentEntity.setProviderMessage("Fraud Alert: Amount mismatch");
            return;
        }
        /// success status update order,payment tables
        if (isSuccess) {

            paymentEntity.setPaymentStatus(PaymentStatus.SUCCESS);
            OrderEntity order = paymentEntity.getOrder();
            order.setStatus(OrderStatus.PROCESSING);
        } else {
            paymentEntity.setPaymentStatus(PaymentStatus.FAILED);
        }

        log.info("Webhook processed successfully for Paymob Order ID: {} with status: {}", paymobOrderId, isSuccess ? "SUCCESS" : "FAILED");
    }

    /// Helper Methods to avoid NULLPOINTEREXCEPTION
    private String getValue(Map<String, Object> obj, String key) {
        return obj.get(key) != null ? String.valueOf(obj.get(key)) : "";
    }

    private String getOrderValue(Map<String, Object> obj, String key) {
        if (obj.get("order") != null) {
            Map<String, Object> order = (Map<String, Object>) obj.get("order");
            return order.get(key) != null ? String.valueOf(order.get(key)) : "";
        }
        return "";
    }

    private String getSourceDataValue(Map<String, Object> obj, String key) {
        if (obj.get("source_data") != null) {
            Map<String, Object> sourceData = (Map<String, Object>) obj.get("source_data");
            return sourceData.get(key) != null ? String.valueOf(sourceData.get(key)) : "";
        }
        return "";
    }


    /// //////////////////////////////
    /// / REFUND
    public void refundPaymentForOrder(OrderEntity orderEntity) {
        /// 1=> get payment record for this payment order api
        List<PaymentEntity> payments = paymentRepo.findByOrderId(orderEntity.getId());

        if (payments.isEmpty()) {
            return;
        }
        for (PaymentEntity payment : payments) {
            /// REFUND IF PAID ONLINE
            if (payment.getPaymentStatus() == PaymentStatus.SUCCESS &&
                    payment.getTransactionId() != null) {
                try {
                    String authToken = authenticate();
                    String url = BASE_URL + "/acceptance/void_refund/refund";
                    String amountInCents = payment.getAmount().multiply(new BigDecimal("100"))
                            .stripTrailingZeros().toPlainString();
                    Map<String, Object> request = new HashMap<>();
                    request.put("auth_token", authToken);
                    request.put("transaction_id", payment.getTransactionId());
                    request.put("amount_cents", amountInCents);
                    ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
                    Map<String, Object> responseBody = response.getBody();

                    if (responseBody != null && (boolean) responseBody.getOrDefault("success", false)) {
                        payment.setPaymentStatus(PaymentStatus.REFUNDED);
                        log.info("order returned successfully: {}", orderEntity.getId());
                    } else {
                        throw new RuntimeException("paymob refuse to return order" + payment.getTransactionId());
                    }
                } catch (Exception e) {
                    log.error("error ", e);
                    throw new RuntimeException("error" + e.getMessage());
                }
            }
            /// 2. REFUND IF PAID CASH (No Transaction ID)
            else if (payment.getPaymentStatus() == PaymentStatus.SUCCESS && payment.getTransactionId() == null) {
                payment.setPaymentStatus(PaymentStatus.REFUNDED);
                log.info("Cash payment marked as refunded for order: {}", orderEntity.getId());
            } else if (payment.getPaymentStatus() == PaymentStatus.PENDING) {
                payment.setPaymentStatus(PaymentStatus.CANCELLED);
            }
        }

        paymentRepo.saveAll(payments);
    }

    /// ///////
    public void createCashPaymentRecord(OrderEntity order, BigDecimal amount) {
        PaymentEntity payment = new PaymentEntity();
        payment.setOrder(order);
        payment.setAmount(amount);
        payment.setCurrency("EGP");
        payment.setPaymentMethod("CASH");
        payment.setPaymentStatus(PaymentStatus.PENDING);

        paymentRepo.save(payment);
        log.info("payment record created for this order id :  {}", order.getId());
    }
}

