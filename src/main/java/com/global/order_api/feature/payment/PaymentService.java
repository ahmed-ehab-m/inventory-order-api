package com.global.order_api.feature.payment;

import com.global.order_api.core.base.BaseService;
import com.global.order_api.feature.user.UserEntity;
import com.global.order_api.feature.user.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Log4j2
public class PaymentService extends BaseService<PaymentEntity,Long> {

    private final UserService userService;
    private final RestTemplate restTemplate;
    public PaymentService(PaymentRepo paymentRepo, UserService userService, RestTemplate restTemplate) {
        super(paymentRepo);
        this.userService = userService;
        this.restTemplate = restTemplate;
    }

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

    private final String BASE_URL = "https://accept.paymob.com/api";

    /// rest template => like postman
    /// class for making my server to send request (server be client)


    //// GENERATING PAYMENT LINK FOR FRONT-END ////////
    public String generatePaymentLink(String paymentMethod, Long userId, BigDecimal amount, Long orderId)
    {
        /// 1=> convert amount to cents because paymob only understand cents
        /// we in java operate with money using big decimal
        /// so if we send bigDecimal to json = jackson may write this like 1.5E4
        /// then paymob refuses it so we use string
        String amountInCents= amount.multiply(new BigDecimal("100"))
                /// strip => remove unneeded 0 from price
                /// plainString => to force result be same not like 1.E
                .stripTrailingZeros().toPlainString();
        UserEntity user = userService.findById(userId);
        try {
            /// 2=> Authentication
            String authToken= authenticate();
            log.info("Step 1: Auth Token generated successfully");

            /// 3=> order registration
            String paymobOrderId= registerOrder(authToken,amountInCents);
            log.info("Step 2: Paymob Order registered with ID: {}", paymobOrderId);
            String activeIntegrationId = getIntegrationId(paymentMethod);
            /// 4=> payment key generation
            String paymentKeyToken=generatePaymentKey(user,authToken,paymobOrderId,amountInCents,activeIntegrationId);
            log.info("Step 3: Payment Key Token generated successfully");
            /// routing dependon payment type
            if("WALLET".equalsIgnoreCase((paymentMethod)))
            {
                /// link to user wallet
                return payWithWallet(paymentKeyToken, user.getPhone());
            }
            else if ("KIOSK".equalsIgnoreCase(paymentMethod)) {
                return payWithKiosk(paymentKeyToken); /// RETURN REFERENCE KEY OF FAWRY
            } else {
                // CARDS
                return BASE_URL + "/acceptance/iframes/" + iframeId + "?payment_token=" + paymentKeyToken;
            }
        }
        catch (Exception e) {
            log.error("Error during Paymob Integration: ", e);
            throw new RuntimeException("فشل في إنشاء رابط الدفع: " + e.getMessage());
        }
    }

    /// 1 => return temp token
    /// Authentication
    private String authenticate()
    {
        /// send api key from paymob to paymob for authentication
        String url =BASE_URL+ "/auth/tokens";
        Map<String,String> request=new HashMap<>();
        /// 1=> prepare our request to send api token
        request.put("api_key",apiKey);

        /// postForEntity => open connection and make POST request and return full response
        /// pass url , request , Map.class => convert json response into Map
        /// Response Entity => status code + headers + body
        ResponseEntity<Map> response=restTemplate.postForEntity(url,request,Map.class);
        /// 2=> return temp token
        return (String) response.getBody().get("token");
    }

    /// 2=> return paymob-order-id
    /// Order registration
    private String registerOrder(String authToken, String amountInCents)
    {
        String url= BASE_URL+ "/ecommerce/orders";
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
    private String generatePaymentKey(UserEntity user, String authToken,String paymobOrderId,String amountInCents,String integrationId)
    {
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
        /// fake data for testing now
        Map<String, String> billingData = new HashMap<>();
        billingData.put("email", user.getEmail());
        billingData.put("first_name", firstName);
        billingData.put("last_name", lastName);
        billingData.put("phone_number",user.getPhone() != null ? user.getPhone() : "+201000000000");
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

    //// for WALLETS
    private String payWithWallet(String paymentToken, String phone)
    {
        String url = BASE_URL + "/acceptance/payments/pay";
        Map<String, String> source = new HashMap<>();
        /// add user phone
        //// identifier => wallet number
        source.put("identifier", phone != null ? phone : "010000000000");
        source.put("subtype", "WALLET");

        Map<String, Object> request = new HashMap<>();
        request.put("source", source);
        request.put("payment_token", paymentToken);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        /// redirect url for user to complete payment with his phone
        return (String) response.getBody().get("redirect_url");
    }

    ///// for FAWRY
    private String payWithKiosk(String paymentToken)
    {
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

}
