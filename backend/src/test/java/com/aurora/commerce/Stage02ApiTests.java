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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Stage02ApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loadsDatabaseDrivenHome() throws Exception {
        mockMvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories", hasSize(6)))
                .andExpect(jsonPath("$.data.featuredProducts", hasSize(4)))
                .andExpect(jsonPath("$.data.heroBanners[0].title").value("把好生活带回家"));

        mockMvc.perform(get("/api/v1/products").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void registersAuthenticatesAndReturnsCurrentUser() throws Exception {
        String email = uniqueEmail();
        String registerBody = """
                {
                  "email": "%s",
                  "password": "AuroraPass@2026",
                  "displayName": "新会员"
                }
                """.formatted(email);

        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).path("data").path("accessToken").asText();
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void rejectsInvalidCredentialsAndInvalidRegistration() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@example.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"short","displayName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void protectsAdminCatalogAndAllowsAdminToPublishProduct() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        String customerToken = registerCustomer();
        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        String adminToken = login("admin@aurora.local", "Aurora@2026");
        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody().replace("\"marketPrice\": 229.00", "\"marketPrice\": 99.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SKU_PRICE"));

        String createResponse = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Stage 02 测试商品"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(createResponse).path("data");
        long productId = data.path("id").asLong();
        mockMvc.perform(patch("/api/v1/admin/products/{id}/publish", productId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Stage 02 测试商品"));
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

    private String registerCustomer() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"%s",
                                  "password":"Customer@2026",
                                  "displayName":"普通用户"
                                }
                                """.formatted(uniqueEmail())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

    private String productBody() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return """
                {
                  "categoryId": 3,
                  "brandId": 3,
                  "name": "Stage 02 测试商品",
                  "subtitle": "用于验证管理端商品闭环",
                  "description": "自动化测试创建的商品",
                  "coverImageUrl": "https://example.com/product.jpg",
                  "featured": false,
                  "skus": [{
                    "skuCode": "STAGE02-%s",
                    "name": "测试 SKU",
                    "specValues": "{\\"color\\":\\"green\\"}",
                    "salePrice": 199.00,
                    "marketPrice": 229.00
                  }]
                }
                """.formatted(suffix);
    }

    private String uniqueEmail() {
        return "stage02-" + UUID.randomUUID() + "@example.com";
    }
}
