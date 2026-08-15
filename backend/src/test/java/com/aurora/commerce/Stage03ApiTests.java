package com.aurora.commerce;

import com.aurora.commerce.inventory.InventoryFacade;
import com.aurora.commerce.shared.error.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Stage03ApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryFacade inventoryFacade;

    @Test
    void managesCartSelectionAndCheckoutPreview() throws Exception {
        String token = registerCustomer();
        String cartResponse = addToCart(token, 1, 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuantity").value(2))
                .andExpect(jsonPath("$.data.selectedAmount").value(4998.00))
                .andReturn().getResponse().getContentAsString();
        long chairItemId = objectMapper.readTree(cartResponse).path("data").path("items").get(0).path("id").asLong();

        addToCart(token, 5, 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuantity").value(3));

        mockMvc.perform(patch("/api/v1/cart/items/{id}", chairItemId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selected\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedQuantity").value(1))
                .andExpect(jsonPath("$.data.selectedAmount").value(189.00));

        mockMvc.perform(post("/api/v1/checkout/preview")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goodsAmount").value(189.00))
                .andExpect(jsonPath("$.data.shippingAmount").value(15.00))
                .andExpect(jsonPath("$.data.payableAmount").value(204.00));

        mockMvc.perform(delete("/api/v1/cart/items/{id}", chairItemId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuantity").value(1));

        String anotherCustomer = registerCustomer();
        mockMvc.perform(delete("/api/v1/cart/items/{id}", chairItemId)
                        .header("Authorization", bearer(anotherCustomer)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CART_ITEM_NOT_FOUND"));
    }

    @Test
    void protectsInventoryManagementAndRejectsUnavailableCartQuantity() throws Exception {
        String customerToken = registerCustomer();
        mockMvc.perform(get("/api/v1/admin/inventories")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden());

        String adminToken = login("admin@aurora.local", "Aurora@2026");
        mockMvc.perform(put("/api/v1/admin/inventories/6")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"totalQuantity":0,"warningQuantity":5,"reason":"Stage 03 库存不足测试"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableQuantity").value(0));

        addToCart(customerToken, 6, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        mockMvc.perform(put("/api/v1/admin/inventories/6")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"totalQuantity":100,"warningQuantity":10,"reason":"恢复演示库存"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void reservesLastUnitAtomicallyAndKeepsRequestsIdempotent() throws Exception {
        inventoryFacade.setInventory(4L, 1, 0, "并发预占测试");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> reserveAfter(start, "stage03-race-a"));
            Future<Boolean> second = executor.submit(() -> reserveAfter(start, "stage03-race-b"));
            start.countDown();

            int successes = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
            assertThat(successes).isEqualTo(1);
            assertThat(inventoryFacade.stock(4L).reservedQuantity()).isEqualTo(1);

            String winningKey = first.get() ? "stage03-race-a" : "stage03-race-b";
            inventoryFacade.reserve(winningKey, Map.of(4L, 1));
            assertThat(inventoryFacade.stock(4L).reservedQuantity()).isEqualTo(1);

            inventoryFacade.release("stage03-race-a");
            inventoryFacade.release("stage03-race-b");
            assertThat(inventoryFacade.stock(4L).reservedQuantity()).isZero();

            inventoryFacade.release("stage03-race-a");
            inventoryFacade.release("stage03-race-b");
            assertThat(inventoryFacade.stock(4L).reservedQuantity()).isZero();
        } finally {
            executor.shutdownNow();
            inventoryFacade.setInventory(4L, 150, 15, "恢复演示库存");
        }
    }

    private boolean reserveAfter(CountDownLatch start, String businessKey) throws InterruptedException {
        start.await();
        try {
            inventoryFacade.reserve(businessKey, Map.of(4L, 1));
            return true;
        } catch (BusinessException exception) {
            assertThat(exception.code()).isEqualTo("INSUFFICIENT_STOCK");
            return false;
        }
    }

    private org.springframework.test.web.servlet.ResultActions addToCart(
            String token,
            long skuId,
            int quantity
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/cart/items")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"skuId":%d,"quantity":%d}
                        """.formatted(skuId, quantity)));
    }

    private String registerCustomer() throws Exception {
        String email = "cart-" + UUID.randomUUID() + "@example.com";
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"%s",
                                  "password":"CartUser@2026",
                                  "displayName":"购物车用户"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return tokenFrom(response);
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return tokenFrom(response);
    }

    private String tokenFrom(String response) throws Exception {
        JsonNode json = objectMapper.readTree(response);
        return json.path("data").path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
