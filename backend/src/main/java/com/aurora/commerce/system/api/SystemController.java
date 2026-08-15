package com.aurora.commerce.system.api;

import com.aurora.commerce.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system")
class SystemController {

    @GetMapping("/overview")
    ApiResponse<SystemOverview> overview() {
        return ApiResponse.success(new SystemOverview(
                "Aurora Commerce",
                "0.1.0",
                "DEVELOPMENT",
                List.of(
                        "identity", "catalog", "pricing", "inventory", "cart", "order",
                        "payment", "fulfillment", "aftersales", "marketing", "content",
                        "engagement", "recommendation", "notification", "analytics"
                )
        ));
    }

    record SystemOverview(String name, String version, String stage, List<String> modules) {
    }
}
