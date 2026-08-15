package com.aurora.commerce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Stage06ApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void completesRefundOnlyAfterSaleAndRestocksExactlyOnce() throws Exception {
        Account customer = registerCustomer();
        Account another = registerCustomer();
        String admin = login("admin@aurora.local", "Aurora@2026");
        int stockBefore = totalStock(admin, 1);
        OrderRef order = createPaidOrder(customer.token(), 1);
        assertThat(totalStock(admin, 1)).isEqualTo(stockBefore - 1);

        String applied = applyAfterSale(customer.token(), order.orderNo(), "REFUND_ONLY")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andReturn().getResponse().getContentAsString();
        String afterSaleNo = data(applied).path("afterSaleNo").asText();
        mockMvc.perform(get("/api/v1/after-sales/{no}", afterSaleNo)
                        .header("Authorization", bearer(another.token())))
                .andExpect(status().isNotFound());

        reviewAfterSale(admin, afterSaleNo, "approve", "符合退款条件")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
        reviewAfterSale(admin, afterSaleNo, "refund", "模拟原路退款完成")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.refundNo").isNotEmpty());
        assertThat(totalStock(admin, 1)).isEqualTo(stockBefore);

        reviewAfterSale(admin, afterSaleNo, "refund", "重复回调")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));
        assertThat(totalStock(admin, 1)).isEqualTo(stockBefore);
        mockMvc.perform(get("/api/v1/orders/{orderNo}", order.orderNo())
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.refundedAt").isNotEmpty());
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].title").value("退款已完成"));
    }

    @Test
    void handlesReturnRefundAndRestoresOrderWhenRejected() throws Exception {
        Account customer = registerCustomer();
        String admin = login("admin@aurora.local", "Aurora@2026");
        OrderRef returnOrder = createPaidOrder(customer.token(), 2);
        ship(admin, returnOrder.orderNo());

        String applied = applyAfterSale(customer.token(), returnOrder.orderNo(), "RETURN_REFUND")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String returnCaseNo = data(applied).path("afterSaleNo").asText();
        reviewAfterSale(admin, returnCaseNo, "approve", "请寄回商品")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING_RETURN"));
        mockMvc.perform(post("/api/v1/after-sales/{no}/return", returnCaseNo)
                        .header("Authorization", bearer(customer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carrier":"顺丰速运","trackingNo":"SF-DEMO-20260815"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETURNED"));
        reviewAfterSale(admin, returnCaseNo, "refund", "退货验收完成")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));

        OrderRef rejectedOrder = createPaidOrder(customer.token(), 3);
        String rejectedApplied = applyAfterSale(customer.token(), rejectedOrder.orderNo(), "REFUND_ONLY")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String rejectedCaseNo = data(rejectedApplied).path("afterSaleNo").asText();
        reviewAfterSale(admin, rejectedCaseNo, "reject", "订单已进入备货，暂不支持直接退款")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
        mockMvc.perform(get("/api/v1/orders/{orderNo}", rejectedOrder.orderNo())
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    void supportsFavoritesVerifiedReviewsAndOperationalDashboard() throws Exception {
        Account customer = registerCustomer();
        String admin = login("admin@aurora.local", "Aurora@2026");

        String draftProduct = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId":1,
                                  "brandId":1,
                                  "name":"Stage 06 草稿商品",
                                  "subtitle":"不可被前台收藏",
                                  "description":"用于验证发布状态边界",
                                  "coverImageUrl":"https://example.com/draft.jpg",
                                  "featured":false,
                                  "skus":[{
                                    "skuCode":"STAGE06-DRAFT-%s",
                                    "name":"Stage 06 草稿 SKU",
                                    "specValues":"{}",
                                    "salePrice":99.00,
                                    "marketPrice":109.00
                                  }]
                                }
                                """.formatted(UUID.randomUUID().toString().substring(0, 8))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long draftProductId = data(draftProduct).path("id").asLong();
        mockMvc.perform(post("/api/v1/favorites/{productId}", draftProductId)
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/favorites/4")
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productName").isNotEmpty());
        mockMvc.perform(post("/api/v1/favorites/4")
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/favorites")
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(delete("/api/v1/favorites/4")
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isOk());

        OrderRef order = createPaidOrder(customer.token(), 4);
        ship(admin, order.orderNo());
        mockMvc.perform(post("/api/v1/orders/{orderNo}/confirm-receipt", order.orderNo())
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        String review = mockMvc.perform(post("/api/v1/reviews")
                        .header("Authorization", bearer(customer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderNo":"%s",
                                  "orderItemId":%d,
                                  "rating":5,
                                  "content":"做工细致，背负体验很好。",
                                  "imageUrls":"https://example.com/review.jpg"
                                }
                                """.formatted(order.orderNo(), order.orderItemId())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long reviewId = data(review).path("id").asLong();
        mockMvc.perform(get("/api/v1/products/4/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].rating").value(5));
        mockMvc.perform(patch("/api/v1/admin/reviews/{id}/reply", reviewId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reply\":\"感谢你的认真体验与分享。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adminReply").isNotEmpty());
        mockMvc.perform(patch("/api/v1/admin/reviews/{id}/hide", reviewId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));
        mockMvc.perform(get("/api/v1/products/4/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paidOrderCount").isNumber())
                .andExpect(jsonPath("$.data.latestOrders").isArray());
        mockMvc.perform(get("/api/v1/admin/coupons")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
        mockMvc.perform(get("/api/v1/admin/content/articles")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").isNumber());
    }

    private org.springframework.test.web.servlet.ResultActions applyAfterSale(
            String token, String orderNo, String type
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/after-sales")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "orderNo":"%s",
                          "type":"%s",
                          "reasonCode":"NOT_AS_EXPECTED",
                          "description":"Stage 06 自动化验收售后申请"
                        }
                        """.formatted(orderNo, type)));
    }

    private org.springframework.test.web.servlet.ResultActions reviewAfterSale(
            String admin, String afterSaleNo, String action, String note
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/after-sales/{no}/{action}", afterSaleNo, action)
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"%s\"}".formatted(note)));
    }

    private OrderRef createPaidOrder(String token, long skuId) throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuId\":%d,\"quantity\":1}".formatted(skuId)))
                .andExpect(status().isOk());
        String created = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "stage06-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "receiverName":"售后验收用户",
                                  "receiverPhone":"13800138000",
                                  "addressLine":"上海市浦东新区 Aurora 路 66 号",
                                  "customerNote":"Stage 06 验收",
                                  "userCouponId":null
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode order = data(created);
        String orderNo = order.path("orderNo").asText();
        long orderItemId = order.path("items").get(0).path("id").asLong();
        String payment = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNo\":\"%s\",\"channel\":\"MOCK\"}".formatted(orderNo)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String paymentNo = data(payment).path("paymentNo").asText();
        mockMvc.perform(post("/api/v1/payments/mock/{paymentNo}/complete", paymentNo)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());
        return new OrderRef(orderNo, orderItemId);
    }

    private void ship(String admin, String orderNo) throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{orderNo}/ship", orderNo)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carrier\":\"顺丰速运\",\"trackingNo\":\"SF-%s\"}"
                                .formatted(UUID.randomUUID().toString().substring(0, 8))))
                .andExpect(status().isOk());
    }

    private int totalStock(String admin, long skuId) throws Exception {
        String response = mockMvc.perform(get("/api/v1/admin/inventories")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode stock : data(response)) {
            if (stock.path("skuId").asLong() == skuId) return stock.path("totalQuantity").asInt();
        }
        throw new AssertionError("Stock not found: " + skuId);
    }

    private Account registerCustomer() throws Exception {
        String email = "stage06-" + UUID.randomUUID() + "@example.com";
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"%s",
                                  "password":"Stage06User@2026",
                                  "displayName":"售后验收用户"
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
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
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

    private record OrderRef(String orderNo, long orderItemId) {
    }
}
