package com.aurora.commerce.inventory;

import com.aurora.commerce.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/inventories")
class AdminInventoryController {

    private final InventoryFacade inventoryFacade;

    AdminInventoryController(InventoryFacade inventoryFacade) {
        this.inventoryFacade = inventoryFacade;
    }

    @GetMapping
    ApiResponse<List<InventoryFacade.StockView>> inventories(
            @RequestParam(defaultValue = "false") boolean warningOnly
    ) {
        return ApiResponse.success(inventoryFacade.allStocks(warningOnly));
    }

    @PutMapping("/{skuId}")
    ApiResponse<InventoryFacade.StockView> setInventory(
            @PathVariable Long skuId,
            @Valid @RequestBody SetInventoryRequest request
    ) {
        return ApiResponse.success(inventoryFacade.setInventory(
                skuId, request.totalQuantity(), request.warningQuantity(), request.reason()
        ));
    }

    record SetInventoryRequest(
            @Min(0) @Max(10_000_000) int totalQuantity,
            @Min(0) @Max(10_000_000) int warningQuantity,
            @NotBlank @Size(max = 300) String reason
    ) {
    }
}
