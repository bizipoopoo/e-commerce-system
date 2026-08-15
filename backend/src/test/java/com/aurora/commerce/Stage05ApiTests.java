package com.aurora.commerce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Stage05ApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void servesPublishedContentAndExplainablePersonalizedRecommendations() throws Exception {
        mockMvc.perform(get("/api/v1/content/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].title").isNotEmpty());
        mockMvc.perform(get("/api/v1/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").isNumber())
                .andExpect(jsonPath("$.data[0].reason").isNotEmpty());

        Account customer = registerCustomer();
        mockMvc.perform(post("/api/v1/behaviors")
                        .header("Authorization", bearer(customer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventType":"ADD_TO_CART","targetType":"PRODUCT","targetId":1}
                                """))
                .andExpect(status().isOk());
        String personalized = mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(data(personalized).findValuesAsText("reason"))
                .anyMatch(reason -> reason.contains("最近关注"));

        String admin = login("admin@aurora.local", "Aurora@2026");
        String slug = "stage05-ai-life-" + UUID.randomUUID().toString().substring(0, 8);
        String created = mockMvc.perform(post("/api/v1/admin/content/articles")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slug":"%s",
                                  "title":"AI 与好生活的共同选择",
                                  "summary":"一篇用于 Stage 05 验收的内容文章。",
                                  "coverImageUrl":"https://example.com/stage05.jpg",
                                  "contentText":"让技术帮助我们发现真正值得长期使用的事物。",
                                  "channelCode":"TECH_LIFE",
                                  "featured":true
                                }
                                """.formatted(slug)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long articleId = data(created).path("id").asLong();
        mockMvc.perform(patch("/api/v1/admin/content/articles/{id}/publish", articleId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/content/articles/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("AI 与好生活的共同选择"));
    }

    @Test
    void claimsLocksAndUsesCouponWithOrderAndPaymentNotifications() throws Exception {
        Account customer = registerCustomer();
        JsonNode campaign = couponByCode(customer.token(), "AURORA50");
        long couponId = campaign.path("couponId").asLong();

        String claimed = mockMvc.perform(post("/api/v1/coupons/{id}/claim", couponId)
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andReturn().getResponse().getContentAsString();
        long userCouponId = data(claimed).path("userCouponId").asLong();
        mockMvc.perform(post("/api/v1/coupons/{id}/claim", couponId)
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userCouponId").value(userCouponId));

        addToCart(customer.token(), 1, 1);
        addToCart(customer.token(), 2, 1);
        String orderResponse = createOrder(customer.token(), userCouponId, "stage05-coupon-" + UUID.randomUUID())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goodsAmount").value(2928.00))
                .andExpect(jsonPath("$.data.discountAmount").value(50.00))
                .andExpect(jsonPath("$.data.payableAmount").value(2878.00))
                .andReturn().getResponse().getContentAsString();
        BigDecimal allocatedDiscount = data(orderResponse).path("items")
                .findValues("discountAmount").stream()
                .map(JsonNode::decimalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(allocatedDiscount).isEqualByComparingTo("50.00");
        String orderNo = data(orderResponse).path("orderNo").asText();
        couponStatus(customer.token(), userCouponId, "LOCKED");

        String payment = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", bearer(customer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":"%s","channel":"MOCK"}
                                """.formatted(orderNo)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String paymentNo = data(payment).path("paymentNo").asText();
        mockMvc.perform(post("/api/v1/payments/mock/{paymentNo}/complete", paymentNo)
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isOk());
        couponStatus(customer.token(), userCouponId, "USED");

        String centerResponse = mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(3))
                .andReturn().getResponse().getContentAsString();
        long notificationId = data(centerResponse).path("items").get(0).path("id").asLong();
        mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationId)
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(2));

        MvcResult stream = mockMvc.perform(get("/api/v1/notifications/stream")
                        .header("Authorization", bearer(customer.token())))
                .andExpect(request().asyncStarted())
                .andReturn();
        stream.getRequest().getAsyncContext().complete();
    }

    @Test
    void releasesCouponOnCancelAndProtectsMarketingAndMessages() throws Exception {
        Account owner = registerCustomer();
        Account another = registerCustomer();
        String admin = login("admin@aurora.local", "Aurora@2026");
        JsonNode campaign = couponByCode(owner.token(), "HOME120");
        String claimed = mockMvc.perform(post("/api/v1/coupons/{id}/claim", campaign.path("couponId").asLong())
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long userCouponId = data(claimed).path("userCouponId").asLong();

        addToCart(owner.token(), 1, 1);
        String orderNo = data(createOrder(owner.token(), userCouponId, "stage05-release-" + UUID.randomUUID())
                .andReturn().getResponse().getContentAsString()).path("orderNo").asText();
        mockMvc.perform(post("/api/v1/orders/{orderNo}/cancel", orderNo)
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk());
        couponStatus(owner.token(), userCouponId, "AVAILABLE");

        addToCart(owner.token(), 1, 1);
        String reusedOrderNo = data(createOrder(
                owner.token(), userCouponId, "stage05-reuse-" + UUID.randomUUID())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.discountAmount").value(120.00))
                .andReturn().getResponse().getContentAsString()).path("orderNo").asText();
        couponStatus(owner.token(), userCouponId, "LOCKED");
        mockMvc.perform(post("/api/v1/orders/{orderNo}/cancel", reusedOrderNo)
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk());
        couponStatus(owner.token(), userCouponId, "AVAILABLE");

        mockMvc.perform(post("/api/v1/coupons/preview")
                        .header("Authorization", bearer(another.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userCouponId":%d,"goodsAmount":2499.00}
                                """.formatted(userCouponId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COUPON_NOT_AVAILABLE"));
        mockMvc.perform(post("/api/v1/admin/coupons")
                        .header("Authorization", bearer(owner.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/notifications")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId":%d,
                                  "type":"SYSTEM",
                                  "title":"Stage 05 系统消息",
                                  "content":"推荐与消息能力已经上线。"
                                }
                                """.formatted(owner.userId())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].title").value("Stage 05 系统消息"));
    }

    private JsonNode couponByCode(String token, String code) throws Exception {
        String response = mockMvc.perform(get("/api/v1/coupons")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode coupon : data(response)) {
            if (code.equals(coupon.path("code").asText())) return coupon;
        }
        throw new AssertionError("Coupon not found: " + code);
    }

    private void couponStatus(String token, long userCouponId, String statusValue) throws Exception {
        String response = mockMvc.perform(get("/api/v1/me/coupons")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode coupon : data(response)) {
            if (coupon.path("userCouponId").asLong() == userCouponId) {
                assertThat(coupon.path("status").asText()).isEqualTo(statusValue);
                return;
            }
        }
        throw new AssertionError("User coupon not found: " + userCouponId);
    }

    private org.springframework.test.web.servlet.ResultActions createOrder(
            String token, long userCouponId, String idempotencyKey
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
                          "customerNote":"使用会员优惠券",
                          "userCouponId":%d
                        }
                        """.formatted(userCouponId)));
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

    private Account registerCustomer() throws Exception {
        String email = "stage05-" + UUID.randomUUID() + "@example.com";
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"%s",
                                  "password":"Stage05User@2026",
                                  "displayName":"发现频道用户"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode auth = data(response);
        return new Account(auth.path("accessToken").asText(), auth.path("user").path("id").asLong());
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return data(response).path("accessToken").asText();
    }

    private JsonNode data(String response) throws Exception {
        return objectMapper.readTree(response).path("data");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record Account(String token, long userId) {
    }
}
