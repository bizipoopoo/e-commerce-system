package com.aurora.commerce;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigurationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsPublicSystemOverview() throws Exception {
        mockMvc.perform(get("/api/v1/system/overview"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsApisThatAreNotExplicitlyPublic() throws Exception {
        mockMvc.perform(get("/api/v1/not-public"))
                .andExpect(status().isUnauthorized())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
