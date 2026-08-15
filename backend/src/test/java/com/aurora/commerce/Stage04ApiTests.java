package com.aurora.commerce;

import com.aurora.commerce.inventory.InventoryFacade;
import com.aurora.commerce.order.OrderFacade;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Stage04ApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryFacade inventoryFacade;

    @Autowired
    private OrderFacade orderFacade;

    @Test
    void completesIdempotentOrderPaymentAndFulfillmentLifecycle() throws Exception {
        String customer = registerCustomer();
        String admin = login("admin@aurora.local", "Aurora@2026");
        int totalBefore = inventoryFacade.stock(1L).totalQuantity();
        addToCart(customer, 1, 2);

        String idempotencyKey = "stage04-full-" + UUID.randomUUID();
        String orderResponse = createOrder(customer, idempotencyKey)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.goodsAmount").value(4998.00))
                .andExpect(jsonPath("$.data.shippingAmount").value(0.00))
                .andReturn().getResponse().getContentAsString();
        String orderNo = data(orderResponse).path("orderNo").asText();
        assertThat(inventoryFacade.stock(1L).reservedQuantity()).isEqualTo(2);

        createOrder(customer, idempotencyKey)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").value(orderNo));
        mockMvc.perform(get("/api/v1/cart").header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuantity").value(0));

        String paymentResponse = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":"%s","channel":"MOCK"}
                                """.formatted(orderNo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        String paymentNo = data(paymentResponse).path("paymentNo").asText();

        completePayment(customer, paymentNo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":"%s","channel":"MOCK"}
                                """.formatted(orderNo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentNo").value(paymentNo))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
        completePayment(customer, paymentNo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
        assertThat(inventoryFacade.stock(1L).reservedQuantity()).isZero();
        assertThat(inventoryFacade.stock(1L).totalQuantity()).isEqualTo(totalBefore - 2);

        String trackingNo = "SF" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        mockMvc.perform(post("/api/v1/admin/orders/{orderNo}/ship", orderNo)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carrier":"顺丰速运","trackingNo":"%s"}
                                """.formatted(trackingNo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.data.tracks[0].description").value("商家已发货，包裹等待揽收"));
        mockMvc.perform(post("/api/v1/admin/orders/{orderNo}/ship", orderNo)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carrier":"顺丰速运","trackingNo":"%s"}
                                """.formatted(trackingNo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tracks.length()").value(1));

        mockMvc.perform(post("/api/v1/orders/{orderNo}/confirm-receipt", orderNo)
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        mockMvc.perform(post("/api/v1/orders/{orderNo}/confirm-receipt", orderNo)
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        mockMvc.perform(get("/api/v1/shipments/{orderNo}", orderNo)
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"))
                .andExpect(jsonPath("$.data.tracks.length()").value(2));
    }

    @Test
    void cancelsAndExpiresPendingOrdersWhileReleasingInventory() throws Exception {
        String customer = registerCustomer();
        addToCart(customer, 5, 1);
        String orderNo = data(createOrder(customer, "stage04-cancel-" + UUID.randomUUID())
                .andReturn().getResponse().getContentAsString()).path("orderNo").asText();
        int reservedAfterCreate = inventoryFacade.stock(5L).reservedQuantity();
        assertThat(reservedAfterCreate).isGreaterThanOrEqualTo(1);

        mockMvc.perform(post("/api/v1/orders/{orderNo}/cancel", orderNo)
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
        mockMvc.perform(post("/api/v1/orders/{orderNo}/cancel", orderNo)
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
        assertThat(inventoryFacade.stock(5L).reservedQuantity()).isEqualTo(reservedAfterCreate - 1);

        addToCart(customer, 2, 1);
        String expiringOrderNo = data(createOrder(customer, "stage04-expire-" + UUID.randomUUID())
                .andReturn().getResponse().getContentAsString()).path("orderNo").asText();
        assertThat(orderFacade.expirePendingOrders(Instant.now().plusSeconds(3600))).isGreaterThanOrEqualTo(1);
        mockMvc.perform(get("/api/v1/orders/{orderNo}", expiringOrderNo)
                        .header("Authorization", bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    void isolatesCustomerOrdersAndProtectsAdministrativeShipping() throws Exception {
        String owner = registerCustomer();
        String another = registerCustomer();
        addToCart(owner, 3, 1);
        String orderNo = data(createOrder(owner, "stage04-owner-" + UUID.randomUUID())
                .andReturn().getResponse().getContentAsString()).path("orderNo").asText();

        mockMvc.perform(get("/api/v1/orders/{orderNo}", orderNo)
                        .header("Authorization", bearer(another)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
        mockMvc.perform(post("/api/v1/admin/orders/{orderNo}/ship", orderNo)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carrier":"测试物流","trackingNo":"NO-AUTH"}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverName":"林晓",
                                  "receiverPhone":"13800138000",
                                  "addressLine":"上海市徐汇区 Aurora 路 88 号"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IDEMPOTENCY_KEY"));
    }

    private org.springframework.test.web.servlet.ResultActions createOrder(
            String token,
            String idempotencyKey
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/orders")
                .header("Authorization", bearer(token))
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "receiverName":"林晓",
                          "receiverPhone":"13800138000",
                          "addressLine":"上海市徐汇区 Aurora 路 88 号",
                          "customerNote":"工作日配送"
                        }
                        """));
    }

    private org.springframework.test.web.servlet.ResultActions completePayment(
            String token,
            String paymentNo
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/payments/mock/{paymentNo}/complete", paymentNo)
                .header("Authorization", bearer(token)));
    }

    private void addToCart(String token, long skuId, int quantity) throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"skuId":%d,"quantity":%d}
                                """.formatted(skuId, quantity)))
                .andExpect(status().isOk());
    }

    private String registerCustomer() throws Exception {
        String email = "order-" + UUID.randomUUID() + "@example.com";
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"%s",
                                  "password":"OrderUser@2026",
                                  "displayName":"订单用户"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return token(response);
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return token(response);
    }

    private JsonNode data(String response) throws Exception {
        return objectMapper.readTree(response).path("data");
    }

    private String token(String response) throws Exception {
        return data(response).path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
